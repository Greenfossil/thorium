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
import scala.util.Try

object CSRFProtectionDecoratingFunction:

  val csrfLogger = LoggerFactory.getLogger("com.greenfossil.thorium.csrf")

  inline private val NONCE_LENGTH = 32

  private def csrfResponseHtml(ctx: ServiceRequestContext, origin: String): String =
    s"""<!DOCTYPE html>
       |<html>
       |<head>
       |  <title>Unauthorized Access</title>
       |</head>
       |<body>
       |  <h1>Access Denied</h1>
       |  <p>Unauthorized Access from origin: $origin, URL:${ctx.uri()}</p>
       |  <p>Your login session is removed</p>
       |</body>
       |</html>
       |""".stripMargin

  val defaultUnauthorizedResponse =
    (ctx: ServiceRequestContext, origin: String, isTokenMatched: Boolean, isHmacVerified: Boolean) =>
      (MediaType.HTML_UTF_8, csrfResponseHtml(ctx, origin))

  //Methods that does not need a token, HEAD, OPTIONS, TRACE
  private val verificationRequiredMethods = List("POST", "PUT", "DELETE", "PATCH")

  val defaultToVerifyMethodFn = (method: String) => verificationRequiredMethods.contains(method)

  def apply(): CSRFProtectionDecoratingFunction =
    new CSRFProtectionDecoratingFunction((_, _) => false, defaultToVerifyMethodFn, defaultUnauthorizedResponse)

  def apply(allowOriginFn: (String, ServiceRequestContext) => Boolean): CSRFProtectionDecoratingFunction =
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

  def generateCSRFToken(config: Configuration, sessionIdOpt: Option[String]): String =
    val appSecret = config.httpConfiguration.secretConfig.secret
    val alg = config.httpConfiguration.csrfConfig.jwt.signatureAlgorithm
    val sessionId = sessionIdOpt.getOrElse(HMACUtil.randomAlphaNumericString(16))
    generateCSRFToken(appSecret, alg, sessionId)
      .fold(
        ex => {
          csrfLogger.error("Fail to generate CSRF token", ex)
          ""
        },
        token => {
          csrfLogger.debug(s"Generated token:$token")
          token
        }
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
        csrfLogger.debug(s"CSRF Token is null")
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
    csrfLogger.debug(s"""Discard cookies:${discardCookieNames.mkString("[",",","]")}""")
    val headers =
      ResponseHeaders.builder(HttpStatus.UNAUTHORIZED)
        .contentType(contentType)
        .cookies(CookieUtil.bakeDiscardCookies(config.httpConfiguration.cookieConfig, discardCookieNames) *)
        .build()
    HttpResponse.of(headers, HttpData.ofUtf8(content))

  private def forwardRequest(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest, tokenUsed: Boolean = false): HttpResponse =
    csrfLogger.debug(s"""Forwarding request to delegate, token is verified:$tokenUsed it would be${if tokenUsed then "" else " not"} discarded""")
    val resp = delegate.serve(ctx, req)
    csrfLogger.debug("Response obtained from delegate")
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
            csrfLogger.debug(s"Found CSRFToken:${csrfToken} for contentType:${mediaType}.")
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
              futureResp.complete(part.contentUtf8())
    else {
      csrfLogger.debug(s"Find CSRFToken found unsupported for contentType:${mediaType}.")
      futureResp.complete(null)
    }
    futureResp

  private def allPathPrefixes(ctx: ServiceRequestContext): Boolean =
    val requestPath = ctx.request().path()
    val allowPathPrefixes = ctx.attr(RequestAttrs.Config).httpConfiguration.csrfConfig.allowPathPrefixes
    allowPathPrefixes.exists(prefix => requestPath.startsWith(prefix))

  override def serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    csrfLogger.debug("CSRF Protection enabled.")
    val headers = req.headers()
    val origin = headers.get(HttpHeaderNames.ORIGIN)
    val referer = headers.get(HttpHeaderNames.REFERER)
    val isSameTarget = origin != null && origin.startsWith(req.uri().getScheme) && origin.endsWith(req.uri().getAuthority)
    val isSameOrigin =  "same-origin" == headers.get(HttpHeaderNames.SEC_FETCH_SITE) || isSameTarget
    val allowOrigin = allowOriginFn(origin, ctx)
    val nonModMethods = !verifyModMethodFn(ctx.method().name()) //Methods POST, PUT,PATCH and DELETE are mod methods
    val isAssetPath = ctx.request().path().startsWith("/assets") && HttpMethod.GET == ctx.method()
    if  isAssetPath || nonModMethods || (allPathPrefixes(ctx)  && isSameOrigin) then
      csrfLogger.debug(s"Request ignored - method:${req.method()}, uri:${req.uri()}, Origin: $origin, isSameOrigin:$isSameOrigin, allowOrigin:$allowOrigin, referer:$referer")
      delegate.serve(ctx, req)
    else
      val config = ctx.attr(RequestAttrs.Config)
      csrfLogger.debug(s"Request to verify - method:${req.method()}, uri:${req.uri()}, Origin: $origin, isSameOrigin:$isSameOrigin, allowOrigin:$allowOrigin, referer:$referer")

      if csrfLogger.isTraceEnabled then
        headers.forEach((key, value) => csrfLogger.trace(s"Header:$key - value:$value"))

      val cookies = headers.cookies()
      csrfLogger.debug(s"Cookies found:${cookies.size()}")
      if csrfLogger.isDebugEnabled then cookies.forEach(cookie => csrfLogger.debug(s"Cookie ${cookie}"))

      //CrossOrigin validation
      val futureResp = new CompletableFuture[HttpResponse]()
      findCSRFToken(ctx)
        .thenAccept: formCSRFToken =>
          ctx.blockingTaskExecutor().execute(() => {
            val cookieCSRFToken: String = headers.cookies().stream().filter(config.httpConfiguration.csrfConfig.cookieName == _.name()).findFirst()
              .map(_.value()).orElse(null)
            val isTokenMatched = formCSRFToken != null && formCSRFToken == cookieCSRFToken
            csrfLogger.debug(s"CSRFToken match status: ${isTokenMatched}\nFormCSRFToken  :$formCSRFToken\nCookieCSRFToken:$cookieCSRFToken")
            //HMAC is verified only when isTokenMatched is true
            val appSecret = config.httpConfiguration.secretConfig.secret
            val isHMACVerified = isTokenMatched && verifyHmac(cookieCSRFToken, appSecret, config.httpConfiguration.csrfConfig.jwt.signatureAlgorithm)
            csrfLogger.debug(s"isTokenMatched:$isTokenMatched, isHhmacVerified:$isHMACVerified")
            val resp = if isHMACVerified
            then
              csrfLogger.debug(s"CSRFToken is verified, forward request to delegate.")
              forwardRequest(delegate, ctx, req, true)
            else if isSameOrigin || allowOrigin
            then
              //Do not enforce CSRF protection sameOrigin
              csrfLogger.warn(s"CSRF verification fails but request will be forwarded since either conditions is true, SameOrigin:${isSameOrigin}, allowOrigin:${allowOrigin}.\nFormToken:${formCSRFToken}\nCookieToken:${cookieCSRFToken}")
              forwardRequest(delegate, ctx, req)
            else
              csrfLogger.warn(s"Cross Origin request blocked, Origin: $origin, isSameOrigin:$isSameOrigin, allowOrigin:$allowOrigin, method:${req.method()}, uri:${req.uri()}")
              val (mediaType, content) = blockCSRFResponseFn(ctx, origin, isTokenMatched, isHMACVerified)
              unauthorizedResponse(config, mediaType, content)
            futureResp.complete(resp)
          })
      HttpResponse.from(futureResp)

