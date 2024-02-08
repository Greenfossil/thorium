
package com.greenfossil.thorium.decorators


import com.greenfossil.thorium.{Recaptcha, RequestAttrs}
import com.linecorp.armeria.common.{HttpMethod, HttpRequest}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.util.concurrent.CompletableFuture
import scala.util.{Failure, Success}

object RecaptchaGuardModule:

  private val DefaultAllPathsVerifyPredicate =
    (_: String, _: ServiceRequestContext) => true

  def apply():RecaptchaGuardModule =
    new RecaptchaGuardModule(DefaultAllPathsVerifyPredicate)

  /**
   * pathVerifyPredicate is true implies that the path's will be verified against Recaptcha service.
   * If false, implies that Recaptcha will not be verified.
   * @param pathVerifyPredicate
   * @return
   */
  def apply(pathVerifyPredicate: (String, ServiceRequestContext) => Boolean): RecaptchaGuardModule =
    new RecaptchaGuardModule(pathVerifyPredicate)

/**
 * pathVerifyPredicate is true implies that the path's will be verified against Recaptcha service.
 * If false, implies that Recaptcha will not be verified.
 * @param pathVerifyPredicate - (Request Path, ServiceRequestContext) => Boolean
 */
class RecaptchaGuardModule(pathVerifyPredicate: (String, ServiceRequestContext) => Boolean) extends ThreatGuardModule:

  protected val logger = LoggerFactory.getLogger("com.greenfossil.thorium.recaptcha")
  override def isSafe(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): CompletableFuture[Boolean] =
    if HttpMethod.GET.equals(req.method) || !pathVerifyPredicate(ctx.path(), ctx) then CompletableFuture.completedFuture(true)
    else
      val config = ctx.attr(RequestAttrs.Config)
      val recaptchaSecretKey = config.httpConfiguration.recaptchaConfig.secretKey
      val recaptchaTokenName = config.httpConfiguration.recaptchaConfig.tokenName
      logger.trace(s"Recaptcha isSafe - uri:${ctx.uri}, method:${req.method}, recaptchaTokenName:$recaptchaTokenName, ecaptchaV3SecretKey = $recaptchaSecretKey")
      extractTokenValue(ctx, recaptchaTokenName)
        .thenApply: recaptchaToken =>
          val isSafe = Recaptcha.siteVerify(recaptchaToken, recaptchaSecretKey, config.httpConfiguration.recaptchaConfig.timeout) match
            case Failure(exception) =>
              logger.warn("isSafe excception", exception)
              false
            case Success(recaptchaResponse) =>
              logger.debug(s"recaptchaResponse = $recaptchaResponse")
              ctx.setAttr(RequestAttrs.RecaptchaResponse, recaptchaResponse)
              recaptchaResponse.success
          val msg = s"Request isSafe:$isSafe, method:${req.method}, uri:${req.uri} path:${req.path} content-type:${req.contentType}"
          if isSafe then logger.debug(msg)
          else
            logger.warn(msg)
            req.headers.forEach((key, value) => logger.warn(s"Header:$key value:$value"))
          isSafe