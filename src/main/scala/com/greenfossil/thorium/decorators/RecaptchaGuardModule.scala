
package com.greenfossil.thorium.decorators


import com.greenfossil.thorium.{Recaptcha, RequestAttrs}
import com.linecorp.armeria.common.{HttpMethod, HttpRequest}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.util.concurrent.CompletableFuture
import scala.util.{Failure, Success}

object RecaptchaGuardModule:

  private val DefaultAllPathsVerifyingFn =
    (_: ServiceRequestContext) => true

  def apply():RecaptchaGuardModule =
    new RecaptchaGuardModule(DefaultAllPathsVerifyingFn)

  def apply(pathVerifyingFn: ServiceRequestContext => Boolean): RecaptchaGuardModule =
    new RecaptchaGuardModule(pathVerifyingFn)

class RecaptchaGuardModule(pathVerifyingFn: ServiceRequestContext => Boolean) extends ThreatGuardModule:

  protected val logger = LoggerFactory.getLogger("com.greenfossil.thorium.recaptcha")
  override def isSafe(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): CompletableFuture[Boolean] =
    if HttpMethod.GET.equals(req.method) || !pathVerifyingFn(ctx) then CompletableFuture.completedFuture(true)
    else
      val config = ctx.attr(RequestAttrs.Config)
      val recaptchaV3SecretKey = config.httpConfiguration.recaptchaConfig.secretKey
      val recaptchaTokenName = config.httpConfiguration.recaptchaConfig.tokenName
      logger.trace(s"Recaptcha isSafe - uri:${ctx.uri}, method:${req.method}, recaptchaTokenName:$recaptchaTokenName, ecaptchaV3SecretKey = $recaptchaV3SecretKey")
      extractTokenValue(ctx, recaptchaTokenName)
        .thenApply: recaptchaToken =>
          val isSafe = Recaptcha.siteVerify(recaptchaToken, recaptchaV3SecretKey, config.httpConfiguration.recaptchaConfig.timeout) match
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