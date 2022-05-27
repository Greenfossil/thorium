package com.greenfossil.webserver

import com.greenfossil.commons.CryptoSupport
import com.greenfossil.commons.json.{JsValue, Json}
import com.linecorp.armeria.common.{AggregatedHttpRequest, Cookie, HttpData, HttpHeaderNames, HttpMethod, MediaType, QueryParams, RequestHeaders}
import com.linecorp.armeria.server.ServiceRequestContext

import java.net.{InetAddress, SocketAddress}
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

  import scala.jdk.CollectionConverters.*

  def env: Environment = requestContext.attr(RequestAttrs.Env)
  
  def httpConfiguration: HttpConfiguration = requestContext.attr(RequestAttrs.HttpConfig)

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
  
  def remoteAddress: InetAddress = requestContext.remoteAddress()

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

  lazy val cookies: Set[Cookie] =
    aggregatedHttpRequest.headers().cookies().asInstanceOf[java.util.Set[Cookie]].asScala.toSet

  def findCookie(name: String): Option[Cookie] =
    cookies.find(c => c.name() == name)

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

  //FIXME - Session will be encrypted, need to do decrytion
  lazy val session: Session = cookies.find(c => c.name() == httpConfiguration.sessionConfig.cookieName).flatMap{c =>
    Json.parse(CryptoSupport.base64DecryptAES(httpConfiguration.secretConfig.secret, c.value()))
      .asOpt[Map[String, String]].map(Session(_))
  }.getOrElse(Session())

  lazy val flash: Flash = cookies.find(c => c.name() == httpConfiguration.flashConfig.cookieName).flatMap{c =>
    Json.parse(CryptoSupport.base64DecryptAES(httpConfiguration.secretConfig.secret, c.value()))
      .asOpt[Map[String, String]].map(Flash(_))
  }.getOrElse(Flash())

  @deprecated("use remoteAddress instead")
  def getRealIP: String = remoteAddress.toString

  def refererOpt: Option[String] =
    getHeader("X-Alt-Referer")
      .orElse(getHeader("referer"))

  def method: HttpMethod = aggregatedHttpRequest.method()

  def userAgent: Option[String] = Option(headers.get(HttpHeaderNames.USER_AGENT))

  def authorization: Option[String] = Option(headers.get(HttpHeaderNames.AUTHORIZATION))

  def availableLanguages: Seq[Locale] = Seq(Locale.getDefault)

  def localeVariantOpt: Option[String] = None

  def locale: Locale = LocaleUtil.getBestMatchLocale(acceptLanguages, availableLanguages, localeVariantOpt)
  
  def asText: String = aggregatedHttpRequest.contentUtf8()

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
      request.method match {
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
