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

package com.greenfossil.thorium

import com.greenfossil.commons.LocaleUtil
import com.greenfossil.commons.json.{JsValue, Json}
import com.greenfossil.thorium.decorators.CSRFProtectionDecoratingFunction
import com.linecorp.armeria.common.*
import com.linecorp.armeria.server.{ProxiedAddresses, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.net.{InetAddress, InetSocketAddress}
import java.util.Locale
import java.util.Locale.LanguageRange
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import scala.util.Try

private[thorium] val requestLogger = LoggerFactory.getLogger("http.request")

object Request:
  
  def actionResponder(request: Request, actionResponseFn: Request => ActionResponse): ActionResponse =
    actionResponseFn(request)

trait Request(val requestContext: ServiceRequestContext, 
              val aggregatedHttpRequest: AggregatedHttpRequest) extends com.greenfossil.commons.LocaleProvider:

  import scala.jdk.CollectionConverters.*

  def config: Configuration = requestContext.attr(RequestAttrs.Config)

  def requestTZ = requestContext.attr(RequestAttrs.TZ)

  def session: Session = requestContext.attr(RequestAttrs.Session)

  def flash: Flash = requestContext.attr(RequestAttrs.Flash)

  def csrfTokenName: String = config.httpConfiguration.csrfConfig.cookieName

  /*
   * Create a CSRFToken, and and set attr RequestAttrs.CSRFToken
   * Every request can have only 1 csrf-token
   */
  lazy val csrfToken: String =
    val token = CSRFProtectionDecoratingFunction.generateCSRFToken(using this)
    requestContext.setAttr(RequestAttrs.CSRFToken, token)
    token

  def env: Environment = config.environment

  def httpConfiguration: HttpConfiguration = config.httpConfiguration

  def contentType: MediaType = requestContext.request().contentType()

  def queryParam(param: String): Option[String] = Option(requestContext.queryParam(param))

  def queryParams(param: String): List[String] = requestContext.queryParams(param).asScala.toList

  def isXhr: Boolean =
    // Check header key and value if XHR (case insensitive)
    requestContext.request().headers().contains("X-Requested-With","XMLHttpRequest" )

  def queryParams: QueryParams = requestContext.queryParams()

  def queryString: String = queryParams.toQueryString

  def queryParamsList: List[(String, String)] =
    queryParams.stream()
      .map(e => e.getKey -> e.getValue)
      .collect(Collectors.toList[(String, String)])
      .asScala
      .toList

  def remoteAddress: InetAddress =
    requestContext.remoteAddress().getAddress

  def secure: Boolean = "https".equalsIgnoreCase(uriScheme)

  def uri: java.net.URI = requestContext.uri()

  //TOOD - need to test
  def host: String =
    getHeader(HttpHeaderNames.HOST).getOrElse(uri.getHost)

  def port: Int = uri.getPort

  /**
    * The host name and port number (if there is any)
    *
    */
  def uriAuthority: String = uri.getAuthority

  /**
    * URI scheme (e.g. http, https)
    */
  def uriScheme: String = uri.getScheme

  def path: String = requestContext.path()

  def endpoint: Endpoint = Endpoint(path)

  def headers: RequestHeaders = requestContext.request().headers()

  def getHeader(name: CharSequence): Option[String] = Option(headers.get(name))

  def getHeaderAll(name: CharSequence): List[String] = headers.getAll(name).asScala.toList

  //https://www.javatips.net/api/java.util.locale.languagerange
  def acceptLanguages: Seq[LanguageRange] =
    Option(requestContext.request().acceptLanguages()).map(_.asScala.toSeq).getOrElse(Nil)

  lazy val cookies: Set[Cookie] =
    requestContext.request().headers().cookies().asInstanceOf[java.util.Set[Cookie]].asScala.toSet

  def findCookie(name: String): Option[Cookie] =
    cookies.find(c => c.name() == name)

  def clientAddress: InetAddress = requestContext.clientAddress()

  def proxiedAddresses: ProxiedAddresses = requestContext.proxiedAddresses()

  def proxiedDestinationAddresses: Seq[InetSocketAddress] = proxiedAddresses.destinationAddresses().asScala.toSeq

  def proxiedSourceAddress: InetSocketAddress = proxiedAddresses.sourceAddress()

  def refererOpt: Option[String] =
    getHeader("X-Alt-Referer")
      .orElse(getHeader("referer"))

  def method: HttpMethod = requestContext.method()

  def userAgent: Option[String] = Option(headers.get(HttpHeaderNames.USER_AGENT))

  def authorization: Option[String] = Option(headers.get(HttpHeaderNames.AUTHORIZATION))

  def availableLanguages: Seq[Locale] =
    Try(config.config.getStringList("app.i18n.langs").asScala.toList.map(Locale.forLanguageTag))
      .getOrElse(Seq(Locale.getDefault))

  def localeVariantOpt: Option[String] = Try(config.config.getString("app.i18n.variant")).toOption

  def locale: Locale = LocaleUtil.getBestMatchLocale(acceptLanguages, availableLanguages, localeVariantOpt)

  def asText: String = aggregatedHttpRequest.contentUtf8()

  //application/json
  def asJson: JsValue = Json.parse(asText)

  //application/x-www-form-urlencoded
  def asFormUrlEncoded: FormUrlEndcoded =
    FormUrlEncodedParser.parse(asText)

  //MultiPart
  import com.linecorp.armeria.common.multipart.Multipart
  private def asMultipartFormData: CompletableFuture[MultipartFormData] =
    actionLogger.debug(s"Processing private asMultipartFormData, ${Thread.currentThread()}")
    Multipart.from(aggregatedHttpRequest.toHttpRequest)
      .aggregate()
      .thenApply(mp =>
        actionLogger.debug(s"Getting Multipart Response. Thread:${Thread.currentThread()}")
        MultipartFormData(mp, requestContext.config().multipartUploadsLocation())
      )
    
  def asMultipartFormData(fn: MultipartFormData => ActionResponse): ActionResponse =
    actionLogger.debug(s"Processing asMultipartFormData, ${Thread.currentThread()}")
    val resp = asMultipartFormData.thenApply(fn(_)).get
    actionLogger.debug(s"Return action response:$resp")
    resp

  //Raw Buffer - TODO - testcase needed and check for conformance
  def asRaw: HttpData = aggregatedHttpRequest.content()
