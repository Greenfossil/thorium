/*
 *  Copyright 2022 Greenfossil Pte Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.greenfossil.thorium.decorators

import com.greenfossil.thorium
import com.greenfossil.thorium.HMACUtil.*
import com.greenfossil.thorium.{CookieUtil, *}
import com.linecorp.armeria.common.{Request as _, *}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.util.Base64
import java.util.concurrent.CompletableFuture
import scala.util.Try

object CSRFGuardModule:

  private val csrfLogger = LoggerFactory.getLogger("com.greenfossil.thorium.csrf")

  inline private val NONCE_LENGTH = 32

  private var enabledCSRFProtection: Boolean = false

  //Methods that does not need a token, HEAD, OPTIONS, TRACE
  private val verificationRequiredMethods = List("POST", "PUT", "DELETE", "PATCH")

  val defaultToVerifyMethodFn = (method: String) => verificationRequiredMethods.contains(method)

  def apply(): CSRFGuardModule =
    apply((_, _) => false)

  def apply(allowOriginFn: (String, ServiceRequestContext) => Boolean): CSRFGuardModule =
    enabledCSRFProtection = true
    csrfLogger.info(s"CSRFProtection enabled.")
    new CSRFGuardModule(allowOriginFn = allowOriginFn, verifyModMethodFn = defaultToVerifyMethodFn)

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

end CSRFGuardModule

class CSRFGuardModule(allowOriginFn: (String, ServiceRequestContext) => Boolean, verifyModMethodFn: String => Boolean) extends ThreatGuardModule:

  protected val logger = CSRFGuardModule.csrfLogger

  import CSRFGuardModule.*
  override def isSafe(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): CompletableFuture[Boolean] =
    val headers = req.headers
    val origin = headers.get(HttpHeaderNames.ORIGIN)
    val referer = headers.get(HttpHeaderNames.REFERER)
    val isSameTarget = origin != null && origin.startsWith(req.uri.getScheme) && origin.endsWith(req.uri.getAuthority)
    val isSameOrigin =  "same-origin" == headers.get(HttpHeaderNames.SEC_FETCH_SITE) || isSameTarget
    val isAllowOrigin = allowOriginFn(origin, ctx)
    val isNonModMethods = !verifyModMethodFn(ctx.method.name) //Methods POST, PUT,PATCH and DELETE are mod methods
    if  isAssetPath(ctx) || isNonModMethods || (allPathPrefixes(ctx)  && isSameOrigin) then
      logger.trace(s"$Request granted - isSameOrigin:$isSameOrigin, allowOrigin:$isAllowOrigin, method:${req.method}, uri:${req.uri}, Origin: $origin, referer:$referer")
      CompletableFuture.completedFuture(true)
    else
      val config = ctx.attr(RequestAttrs.Config)
      logger.info(s"Verifying request - isSameOrigin:$isSameOrigin, allowOrigin:$isAllowOrigin Origin: $origin, referer:$referer, method:${req.method}, uri:${req.uri}, content-type:${req.contentType} ...")
      headers.forEach((key, value) => logger.debug(s"Header:$key - value:$value"))

      val cookies = headers.cookies
      logger.info(s"Cookies found:${cookies.size}")
      cookies.forEach(cookie => logger.info(s"Cookie $cookie"))

      //CrossOrigin validation
      val csrfCookieName = config.httpConfiguration.csrfConfig.cookieName
      extractTokenValue(ctx, csrfCookieName)
        .thenApply: formCSRFToken =>
          val cookieCSRFToken: String = cookies.stream
            .filter(csrfCookieName == _.name)
            .findFirst
            .map(_.value).orElse(null)
          val isTokenPairMatched = formCSRFToken != null && formCSRFToken == cookieCSRFToken
          logger.info(s"CSRFTokenPair matched:$isTokenPairMatched,  FormCSRFToken:[$formCSRFToken], CookieCSRFToken:[$cookieCSRFToken]")
          //HMAC is verified only when isTokenMatched is true
          val appSecret = config.httpConfiguration.secretConfig.secret
          val isHMACVerified = isTokenPairMatched && verifyHmac(cookieCSRFToken, appSecret, config.httpConfiguration.csrfConfig.jwt.signatureAlgorithm)
          logger.info(s"isTokenMatched:$isTokenPairMatched, isHhmacVerified:$isHMACVerified")
          //logs request header
          val isSafe = isHMACVerified || isAllowOrigin //isSameOrigin - must also ensure csrf tokens exists and validated
          val msg = s"Request isSafe:$isSafe, Origin: $origin, isSameOrigin:$isSameOrigin, allowOrigin:$isAllowOrigin, method:${req.method}, uri:${req.uri} path:${req.path} content-type:${req.contentType}"
          if isSafe then logger.debug(msg)
          else
            logger.warn(msg)
            req.headers.forEach((key, value) => logger.warn(s"Header:$key value:$value"))
          isSafe

  private def allPathPrefixes(ctx: ServiceRequestContext): Boolean =
    val requestPath = ctx.request.path
    val allowPathPrefixes = ctx.attr(RequestAttrs.Config).httpConfiguration.csrfConfig.allowPathPrefixes
    allowPathPrefixes.exists(prefix => requestPath.startsWith(prefix))