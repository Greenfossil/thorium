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
import com.greenfossil.thorium.{CookieUtil, *}
import com.linecorp.armeria.common.{Request as _, *}
import com.linecorp.armeria.server.{DecoratingHttpServiceFunction, HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

object ThreatGuardModuleDecoratingFunction:

  private val logger = LoggerFactory.getLogger("com.greenfossil.thorium.csrf")

  private def accessDeniedHtml: String =
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

  def apply(module: ThreatGuardModule): ThreatGuardModuleDecoratingFunction =
    new ThreatGuardModuleDecoratingFunction(module, (_, _) => accessDeniedHtml)

class ThreatGuardModuleDecoratingFunction(module: ThreatGuardModule, accessDeniedFn: (Configuration, ServiceRequestContext) => String) extends DecoratingHttpServiceFunction:

  import ThreatGuardModuleDecoratingFunction.logger

  private def accessDeniedResponse(svcReqContext: ServiceRequestContext): HttpResponse =
    val config = svcReqContext.attr(RequestAttrs.Config)

    //Remove session and assume it is a CSRF attack
    import config.httpConfiguration.*

    //discard all cookies
    val discardCookieNames = List(sessionConfig.cookieName, csrfConfig.cookieName, flashConfig.cookieName, "tz")
    logger.info(s"""Discard cookies:${discardCookieNames.mkString("[",",","]")}""")

    val headers =
      ResponseHeaders.builder(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.HTML_UTF_8)
        .cookies(CookieUtil.bakeDiscardCookies(config.httpConfiguration.cookieConfig, discardCookieNames) *)
        .build()

    val html = accessDeniedFn(config, svcReqContext)
    HttpResponse.of(headers, HttpData.ofUtf8(html))

  override def serve(delegate: HttpService, svcReqContext: ServiceRequestContext, req: HttpRequest): HttpResponse =
    HttpResponse.of:
      module.isSafe(delegate, svcReqContext, req)
        .handle: (isSafe, ex) =>
          if ex != null then
            logger.warn("Exception raised", ex)
            accessDeniedResponse(svcReqContext)
          else if !isSafe then
            logger.warn("IsSafe validation fails")
            accessDeniedResponse(svcReqContext)
          else
            logger.debug(s"Request isSafe... forward request to delegate:${delegate}")
            delegate.serve(svcReqContext, req)
