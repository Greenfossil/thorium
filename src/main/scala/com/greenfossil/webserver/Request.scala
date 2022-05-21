package com.greenfossil.webserver

import com.greenfossil.commons.json.{JsValue, Json}
import com.linecorp.armeria.common.{AggregatedHttpRequest, Cookie, HttpMethod, HttpData, MediaType, QueryParams, RequestHeaders}
import com.linecorp.armeria.server.ServiceRequestContext

import java.net.SocketAddress
import java.time.ZoneId
import java.util.Locale.LanguageRange
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import java.util.Locale
import scala.util.Try

object RequestAttrs {
  import io.netty.util.AttributeKey
  val TZ = AttributeKey.valueOf[ZoneId]("tz")
  val Session = AttributeKey.valueOf[Session]("session")
  val Flash = AttributeKey.valueOf[Flash]("flash")
  val Env = AttributeKey.valueOf[Environment]("env")
  val HttpConfig = AttributeKey.valueOf[HttpConfiguration]("httpConfig")
}

trait Request(val requestContext: ServiceRequestContext, val aggregatedHttpRequest: AggregatedHttpRequest) {
  
  def env: Environment = requestContext.attr(RequestAttrs.Env)
  
  def httpConfiguration: HttpConfiguration = requestContext.attr(RequestAttrs.HttpConfig)

  def contentType: MediaType = requestContext.request().contentType()

  def queryParam(param: String): Option[String] = Option(requestContext.queryParam(param))

  def isXhr: Boolean = 
    // Check header key and value if XHR (case insensitive)
    requestContext.request().headers().contains("X-Requested-With","XMLHttpRequest" )

  def queryParams: QueryParams = requestContext.queryParams()

  import scala.jdk.CollectionConverters.*
  def queryParamsList: List[(String, String)] = 
    queryParams.stream()
      .map(e => e.getKey -> e.getValue)
      .collect(Collectors.toList[(String, String)])
      .asScala
      .toList
  
  def remoteAddress[A <: java.net.SocketAddress]: A = requestContext.remoteAddress()

  def secure: Boolean = "https".equalsIgnoreCase(uriScheme)

  def uri: java.net.URI = aggregatedHttpRequest.uri()

  /**
    * The host name and port number (if there is any)
    *
    */
  def uriAuthority: String = uri.getAuthority

  /**
    * URI scheme (e.g. http, https)
    */
  def uriScheme: String = uri.getScheme

  def path: String = aggregatedHttpRequest.path()

  def endpoint: Endpoint = Endpoint(path)
  
  def headers: RequestHeaders = aggregatedHttpRequest.headers()

  def getHeader(name: String): Option[String] = Option(headers.get(name))

  //https://www.javatips.net/api/java.util.locale.languagerange
  def acceptLanguages: Seq[LanguageRange] = 
    aggregatedHttpRequest.acceptLanguages().asScala.toSeq

  def cookies: Set[Cookie] =
    aggregatedHttpRequest.headers().cookies().asInstanceOf[java.util.Set[Cookie]].asScala.toSet

  /*
   * Setup attrs
   */
  cookies.find(c => c.name() == RequestAttrs.TZ.name()).foreach{c =>
    val tz = Try(ZoneId.of(c.value())).fold(
      ex => ZoneId.systemDefault(),
      tz => tz
    )
    requestContext.setAttr(RequestAttrs.TZ, tz)
  }

  val session: Session = cookies.find(c => c.name() == RequestAttrs.Session.name()).flatMap{c =>
    val sessionJwt: JsValue = Json.parseBase64URL(c.value())
    sessionJwt.asOpt[Map[String, String]].map(Session(_))
  }.getOrElse(Session())

  val flash: Flash = cookies.find(c => c.name() == RequestAttrs.Flash.name()).flatMap{c =>
    Json.parseBase64URL(c.value()).asOpt[Map[String, String]].map(Flash(_))
  }.getOrElse(Flash())

  @deprecated("use remoteAddress instead")
  def getRealIP: String = remoteAddress.toString

  def refererOpt: Option[String] =
    getHeader("X-Alt-Referer")
      .orElse(getHeader("referer"))

  def availableLanguages: Seq[Locale] = Seq(Locale.getDefault)

  def localeVariantOpt: Option[String] = None

  def locale: Locale = LocaleUtil.getBestMatchLocale(acceptLanguages, availableLanguages, localeVariantOpt)

  def method(): HttpMethod = aggregatedHttpRequest.method()
  
  def  asText: String = aggregatedHttpRequest.contentUtf8()

  //application/json
  def asJson: JsValue = Json.parse(asText)

  //application/x-www-form-urlencoded
  def asFormUrlEncoded: Map[String, Seq[String]] =
    FormUrlEncodedParser.parse(asText)

  //MultiPart
  import com.linecorp.armeria.common.multipart.Multipart
  import com.linecorp.armeria.scala.implicits.*
  def asMultipartFormData: CompletableFuture[MultipartFormData] =
    Multipart.from(aggregatedHttpRequest.toHttpRequest)
      .aggregate()
      .thenApply(mp => MultipartFormData(mp))
    
  def asMultipartFormData(fn: MultipartFormData => Result): Result =
    asMultipartFormData.thenApply(fn(_)).get

  //Raw Buffer - TODO - testcase needed and check for conformance
  def asRaw: HttpData = aggregatedHttpRequest.content()
}


import com.greenfossil.data.mapping.Mapping

extension[A](field: Mapping[A])
  def bindFromRequest()(using request: Request): Mapping[A] =
    val queryData: List[(String, String)] =
      request.method() match {
        case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Nil
        case _ => request.queryParamsList
      }

    request match {

      //TODO - to be tested
//      case req if req.asMultipartFormData.get().bodyPart.nonEmpty =>
//        field.bind(req.asMultipartFormData.get().asFormUrlEncoded ++ queryData.groupMap(_._1)(_._2))

      case req if Try(req.asJson).isSuccess =>
        //        bind(req.asJson, querydata)
        field.bind(request.asJson)

      case req =>
        field.bind(req.asFormUrlEncoded ++ queryData.groupMap(_._1)(_._2))
    }
