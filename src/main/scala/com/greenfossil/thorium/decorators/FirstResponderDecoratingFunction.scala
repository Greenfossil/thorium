package com.greenfossil.thorium.decorators

import com.greenfossil.thorium.{Configuration, CookieUtil, Flash, RequestAttrs, Session, serverLogger}
import com.linecorp.armeria.common.{Cookie, HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{DecoratingHttpServiceFunction, HttpService, ServiceRequestContext}

import java.time.ZoneId
import scala.util.Try

/**
 * This will be the first/last request within the control of Thorium.
 * This is use to setup Config/Session/Flash attributes which will be useful for all services
 *
 * @param configuration
 * @return
 */
class FirstResponderDecoratingFunction(val configuration: Configuration,
                                       val ignoreRequestFn: ServiceRequestContext => Boolean = _.request().uri().getPath.startsWith("/assets")
                                      ) extends DecoratingHttpServiceFunction:
  override def serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    //Setup Session
    ctx.setAttr(RequestAttrs.Config, configuration)
    if ignoreRequestFn(ctx) then
      serverLogger.trace(s"FirstResponder: ignore request and injected Configuration - method:${req.method()} request.uri:${req.uri()}.")
      delegate.serve(ctx, req)
    else
      val cookies: Set[Cookie] =
        import scala.jdk.CollectionConverters.*
        ctx.request().headers().cookies().asScala.toSet

      //Setup TZ attr
      val tz = cookies.find(c => RequestAttrs.TZ.name() == c.name())
        .flatMap { c => Try(ZoneId.of(c.value())).toOption }
        .getOrElse(ZoneId.systemDefault())
      ctx.setAttr(RequestAttrs.TZ, tz)

      //Setup Session
      val session = cookies.find(c => c.name() == configuration.httpConfiguration.sessionConfig.cookieName).flatMap { c =>
        CookieUtil.decryptCookieValue(c, configuration.httpConfiguration.secretConfig.secret).map(Session(_))
      }.getOrElse(Session())
      ctx.setAttr(RequestAttrs.Session, session)

      //Setup Flash
      val flash = cookies.find(c => c.name() == configuration.httpConfiguration.flashConfig.cookieName).flatMap { c =>
        CookieUtil.decryptCookieValue(c, configuration.httpConfiguration.secretConfig.secret).map(Flash(_))
      }.getOrElse(Flash())
      ctx.setAttr(RequestAttrs.Flash, flash)

      serverLogger.debug(s"FirstResponder: injected Configuration and extracted Cookies - method:${req.method()} request.uri:${req.uri()}\nTZ: $tz\nSession:$session\nFlash:$flash")

      //Invoke delegate
      delegate.serve(ctx, req)