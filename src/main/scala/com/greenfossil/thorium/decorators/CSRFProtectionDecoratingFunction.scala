/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium.decorators

import com.greenfossil.thorium
import com.greenfossil.thorium.HMACUtil.*
import com.greenfossil.thorium.{CookieUtil, *}
import com.linecorp.armeria.common.multipart.Multipart
import com.linecorp.armeria.common.{Request as _, *}
import com.linecorp.armeria.server.{DecoratingHttpServiceFunction, HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.util.Base64
import java.util.concurrent.CompletableFuture
import scala.annotation.unused
import scala.util.Try

/**
 * Implementation - https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
 */
object CSRFProtectionDecoratingFunction:

  val csrfLogger = LoggerFactory.getLogger("com.greenfossil.thorium.csrf")

  inline private val NONCE_LENGTH = 32

  private var enabledCSRFProtection: Boolean = false

  private def csrfResponseHtml(@unused ctx: ServiceRequestContext, @unused origin: String): String =
    s"""<!DOCTYPE html>
       |<html>
       |<head>
       |  <title>Unauthorized Access</title>
       |</head>
       |<body>
       |  <h1>Access Denied</h1>
       |</body>
       |</html>
       |""".stripMargin

  private val defaultUnauthorizedResponse =
    (ctx: ServiceRequestContext, origin: String, isTokenMatched: Boolean, isHmacVerified: Boolean) =>
      (MediaType.HTML_UTF_8, csrfResponseHtml(ctx, origin))

  //Methods that does not need a token, HEAD, OPTIONS, TRACE
  private val verificationRequiredMethods = List("POST", "PUT", "DELETE", "PATCH")

  val defaultToVerifyMethodFn = (method: String) => verificationRequiredMethods.contains(method)

  def apply(): CSRFProtectionDecoratingFunction =
    apply((_, _) => false)

  def apply(allowOriginFn: (String, ServiceRequestContext) => Boolean): CSRFProtectionDecoratingFunction =
    enabledCSRFProtection = true
    csrfLogger.info(s"CSRFProtection enabled.")
    new CSRFProtectionDecoratingFunction(
      allowOriginFn = allowOriginFn,
      verifyModMethodFn = defaultToVerifyMethodFn,
      blockCSRFResponseFn = defaultUnauthorizedResponse,
    )

  def generateCSRFTokenCookie(configuration: Configuration, sessionIdOpt: Option[String]): Cookie =
    val csrfToken = generateCSRFToken(configuration, sessionIdOpt)
    CookieUtil.csrfCookieBuilder(Configuration().httpConfiguration.csrfConfig, csrfToken).build()

  def generateCSRFToken(using request: Request): String =
    generateCSRFToken(request.config, request.session.idOpt)

  private def generateCSRFToken(config: Configuration, sessionIdOpt: Option[String]): String =
    val appSecret = config.httpConfiguration.secretConfig.secret
    val alg = config.httpConfiguration.csrfConfig.jwt.signatureAlgorithm
    val sessionId = sessionIdOpt.getOrElse(HMACUtil.randomAlphaNumericString(16))
    generateCSRFToken(appSecret, alg, sessionId)
      .fold(
        ex =>
          csrfLogger.error("Fail to generate CSRF token", ex)
          "",
        token =>
          csrfLogger.trace(s"$enabledCSRFProtection - generated token:$token")
          token
      )

  /**
   *
   * @param key
   * @param userId
   * @param sessionId
   * @return
   */
  def generateCSRFToken(key: String, algorithm: String, sessionId: String): Try[String] =
    Try:
      val message = sessionId + "!" + randomBytes(NONCE_LENGTH, Base64.getUrlEncoder.encodeToString)
      val hmac = HMACUtil.hmac(message.getBytes("UTF-8"), key.getBytes("UTF-8"),
        algorithm, Base64.getUrlEncoder.encodeToString)
      val token = hmac + "." + message
      token

  def verifyHmac(csrfToken: String, key: String, algorithm: String): Boolean =
    (Try:
      if csrfToken == null then
        csrfLogger.trace(s"CSRF Token is null")
        false
      else
        val Array(tokenHMAC, tokenMessage) = csrfToken.split("\\.", 2)
        val expectedHMAC = HMACUtil.hmac(tokenMessage.getBytes("UTF-8"), key.getBytes("UTF-8"),
          algorithm, Base64.getUrlEncoder.encodeToString)
        val hmacVerified = constantTimeEquals(tokenHMAC.getBytes("UTF-8"), expectedHMAC.getBytes("UTF-8"))
        if !hmacVerified then csrfLogger.warn(s"HMAC invalid, token: $csrfToken, expected:$expectedHMAC")
        hmacVerified
      ).getOrElse(false)

end CSRFProtectionDecoratingFunction

/**
 *
 * @param allowOriginFn - (Origin, ServiceRequestContext) => Boolean
 * @param verifyModMethodFn - Method that will be subjected to CSRF verification
 * @param blockCSRFResponseFn - (ServiceRequestContext, Origin, isTokenMatched, isHMACVerified)
 */
class CSRFProtectionDecoratingFunction(allowOriginFn: (String, ServiceRequestContext) => Boolean,
                                       verifyModMethodFn: String => Boolean,
                                       blockCSRFResponseFn: (ServiceRequestContext, String, Boolean, Boolean) => (MediaType, String),
                            ) extends DecoratingHttpServiceFunction:

  import CSRFProtectionDecoratingFunction.*

  private def unauthorizedResponse(config: Configuration, contentType: MediaType, content: String): HttpResponse =
    //Remove session and assume it is a CSRF attack
    import config.httpConfiguration.*

    val discardCookieNames = List(sessionConfig.cookieName, csrfConfig.cookieName, flashConfig.cookieName, "tz")
    csrfLogger.info(s"""Discard cookies:${discardCookieNames.mkString("[",",","]")}""")
    val headers =
      ResponseHeaders.builder(HttpStatus.UNAUTHORIZED)
        .contentType(contentType)
        .cookies(CookieUtil.bakeDiscardCookies(config.httpConfiguration.cookieConfig, discardCookieNames) *)
        .build()
    HttpResponse.of(headers, HttpData.ofUtf8(content))

  private def forwardRequest(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest, tokenUsed: Boolean = false): HttpResponse =
    csrfLogger.info(s"""Forwarding request to delegate, token is verified:$tokenUsed it would be${if tokenUsed then "" else " not"} discarded""")
    val resp = delegate.serve(ctx, req)
    csrfLogger.trace("Response obtained from delegate")
    if !tokenUsed then resp
    else
      val config = ctx.attr(RequestAttrs.Config)
      val discardCSRFCookie = CookieUtil.bakeDiscardCookie(config.httpConfiguration.csrfConfig, config.httpConfiguration.csrfConfig.cookieName)
      resp.mapHeaders(_.toBuilder.cookies(discardCSRFCookie).build())

  private def findCSRFToken(ctx: ServiceRequestContext): CompletableFuture[String] =
    val csrfCookieName = ctx.attr(RequestAttrs.Config).httpConfiguration.csrfConfig.cookieName
    val mediaType = ctx.request().contentType()
    val futureResp = new CompletableFuture[String]()
    csrfLogger.debug(s"Find CSRFToken from request - content-type:${ctx.request().contentType()}")
    if mediaType == null then
      csrfLogger.warn("Null media type assume not token")
      futureResp.complete(null)
    else if mediaType.is(MediaType.FORM_DATA) then
      ctx.request()
        .aggregate()
        .thenAccept: aggReq =>
          ctx.blockingTaskExecutor().execute(() => {
            aggReq.contentUtf8()
            val form = FormUrlEncodedParser.parse(aggReq.contentUtf8())
            val csrfToken = form.get(csrfCookieName).flatMap(_.headOption).orNull
            csrfLogger.info(s"Found CSRFToken:$csrfToken, content-type:$mediaType.")
            futureResp.complete(csrfToken)
          })
    else if mediaType.isMultipart then
      ctx.request()
        .aggregate()
        .thenAccept: aggReg =>
          Multipart.from(aggReg.toHttpRequest.toDuplicator.duplicate())
            .aggregate()
            .thenAccept: multipart =>
              val part = multipart.field(csrfCookieName)
              futureResp.complete(if part == null then null else part.contentUtf8())
    else {
      csrfLogger.info(s"Find CSRFToken found unsupported for content-type:$mediaType.")
      futureResp.complete(null)
    }
    futureResp

  private def allPathPrefixes(ctx: ServiceRequestContext): Boolean =
    val requestPath = ctx.request().path()
    val allowPathPrefixes = ctx.attr(RequestAttrs.Config).httpConfiguration.csrfConfig.allowPathPrefixes
    allowPathPrefixes.exists(prefix => requestPath.startsWith(prefix))

  override def serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    val headers = req.headers()
    val origin = headers.get(HttpHeaderNames.ORIGIN)
    val referer = headers.get(HttpHeaderNames.REFERER)
    val isSameTarget = origin != null && origin.startsWith(req.uri().getScheme) && origin.endsWith(req.uri().getAuthority)
    val isSameOrigin =  "same-origin" == headers.get(HttpHeaderNames.SEC_FETCH_SITE) || isSameTarget
    val allowOrigin = allowOriginFn(origin, ctx)
    val nonModMethods = !verifyModMethodFn(ctx.method().name()) //Methods POST, PUT,PATCH and DELETE are mod methods
    val isAssetPath = ctx.request().path().startsWith("/assets") && HttpMethod.GET == ctx.method()
    if  isAssetPath || nonModMethods || (allPathPrefixes(ctx)  && isSameOrigin) then
      csrfLogger.trace(s"$Request granted - isSameOrigin:$isSameOrigin, allowOrigin:$allowOrigin, method:${req.method()}, uri:${req.uri()}, Origin: $origin, referer:$referer")
      delegate.serve(ctx, req)
    else
      val config = ctx.attr(RequestAttrs.Config)
      csrfLogger.info(s"Verifying request - isSameOrigin:$isSameOrigin, allowOrigin:$allowOrigin, method:${req.method()}, uri:${req.uri()}, content-type:${req.contentType()} Origin: $origin, referer:$referer ...")
        headers.forEach((key, value) => csrfLogger.debug(s"Header:$key - value:$value"))

      val cookies = headers.cookies()
      csrfLogger.info(s"Cookies found:${cookies.size()}")
      cookies.forEach(cookie => csrfLogger.info(s"Cookie $cookie"))

      //CrossOrigin validation
      val futureResp = new CompletableFuture[HttpResponse]()
      findCSRFToken(ctx)
        .thenAccept: formCSRFToken =>
          ctx.blockingTaskExecutor().execute(() => {
            val cookieCSRFToken: String = headers.cookies().stream().filter(config.httpConfiguration.csrfConfig.cookieName == _.name()).findFirst()
              .map(_.value()).orElse(null)
            val isTokenPairMatched = formCSRFToken != null && formCSRFToken == cookieCSRFToken
            csrfLogger.info(s"CSRFTokenPair matched:$isTokenPairMatched,  FormCSRFToken:[$formCSRFToken], CookieCSRFToken:[$cookieCSRFToken]")
            //HMAC is verified only when isTokenMatched is true
            val appSecret = config.httpConfiguration.secretConfig.secret
            val isHMACVerified = isTokenPairMatched && verifyHmac(cookieCSRFToken, appSecret, config.httpConfiguration.csrfConfig.jwt.signatureAlgorithm)
            csrfLogger.info(s"isTokenMatched:$isTokenPairMatched, isHhmacVerified:$isHMACVerified")
            val resp = if isHMACVerified
            then
              csrfLogger.info(s"CSRFToken is verified, forward request to delegate.")
              forwardRequest(delegate, ctx, req, true)
            else if isSameOrigin || allowOrigin
            then
              //Do not enforce CSRF protection sameOrigin
              csrfLogger.warn(s"Verification fails but request will be forwarded since either conditions is true, SameOrigin:$isSameOrigin, allowOrigin:$allowOrigin.\nFormToken:${formCSRFToken}\nCookieToken:$cookieCSRFToken")
              forwardRequest(delegate, ctx, req)
            else
              csrfLogger.warn(s"Request blocked, Origin: $origin, isSameOrigin:$isSameOrigin, allowOrigin:$allowOrigin, method:${req.method()}, uri:${req.uri()} path:${req.path} content-type:${req.contentType()}")
              req.headers().forEach((key, value) => csrfLogger.warn(s"Header:$key value:$value"))
              val (mediaType, content) = blockCSRFResponseFn(ctx, origin, isTokenPairMatched, isHMACVerified)
              unauthorizedResponse(config, mediaType, content)
            futureResp.complete(resp)
          })
      HttpResponse.of(futureResp)

