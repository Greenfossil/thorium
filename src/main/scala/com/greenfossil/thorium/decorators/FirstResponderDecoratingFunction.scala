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

import com.greenfossil.thorium.{Configuration, CookieUtil, Flash, RequestAttrs, Session}
import com.linecorp.armeria.common.{Cookie, HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{DecoratingHttpServiceFunction, HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.time.ZoneId
import scala.util.Try

private val firstResponderLogger = LoggerFactory.getLogger("com.greenfossil.thorium.first-responder")

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
    firstResponderLogger.debug(s"FirstResponder - remote:${ctx.remoteAddress()} method:${req.method()} request.uri:${req.uri()}")
    if firstResponderLogger.isTraceEnabled() then
      firstResponderLogger.trace(s"Headers:${req.headers().size()}")
      req.headers().forEach((key, value) => firstResponderLogger.trace(s"key:$key value:$value"))

    //Setup Session
    ctx.setAttr(RequestAttrs.Config, configuration)
    if ignoreRequestFn(ctx) then
      firstResponderLogger.trace(s"Ignore request and injected Configuration.")
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

      firstResponderLogger.debug(s"Injected Configuration and extracted Cookies -TZ: $tz, Session:$session, Flash:$flash")

      //Invoke delegate
      delegate.serve(ctx, req)