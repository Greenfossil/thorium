package com.greenfossil.webserver

import com.greenfossil.commons.json.{JsObject, JsValue, Json}
import com.linecorp.armeria.common.{AggregatedHttpRequest, Cookie, HttpData, HttpMethod, HttpResponse, RequestHeaders}
import com.linecorp.armeria.common.multipart.{AggregatedMultipart, BodyPart}
import com.linecorp.armeria.common.stream.StreamMessage
import com.linecorp.armeria.server.ServiceRequestContext

import java.time.ZoneId
import java.util.{Base64, Locale}
import java.util.Locale.LanguageRange
import java.util.concurrent.CompletableFuture
import scala.util.Try

object RequestAttrs {
  import io.netty.util.AttributeKey
  val TZ = AttributeKey.valueOf[ZoneId]("tz")
  val Session = AttributeKey.valueOf[Session]("session")
  val Flash = AttributeKey.valueOf[Flash]("flash")
}

trait Request(val requestContext: ServiceRequestContext, val aggregatedHttpRequest: AggregatedHttpRequest) {
//  export requestContext.*

//Overlap Play and Armeria api  
//  def path: String
//  def method: String = ""
//  def remoteAddress[A <: java.net.SocketAddress]: A /*String*/ = requestContext.remoteAddress()

  def uri: java.net.URI  /*String*/ = aggregatedHttpRequest.uri()

  def path(): String = aggregatedHttpRequest.path()
  
  def headers: RequestHeaders /*Headers*/ = aggregatedHttpRequest.headers()

  //https://www.javatips.net/api/java.util.locale.languagerange
  import scala.jdk.CollectionConverters.*
  def acceptLanguages: Seq[LanguageRange] = aggregatedHttpRequest.acceptLanguages().asScala.toSeq

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
    println(s"Request sessionJwt = ${sessionJwt}, cookie = ${c.value}")
    sessionJwt.asOpt[Map[String, String]].map(Session(_))
  }.getOrElse(Session())

  val flash: Flash = cookies.find(c => c.name() == RequestAttrs.Flash.name()).flatMap{c =>
    Json.parseBase64URL(c.value()).asOpt[Map[String, String]].map(Flash(_))
  }.getOrElse(Flash())

  @deprecated("use remoteAddress instead")
  def getRealIP: String = "FIXME"

  //  def getReferer: Option[String] =
//    headers.get("X-Alt-Referer").orElse(headers.get("referer"))
//

  def refererOpt: Option[String] = ???

  def availableLanguages: Seq[Locale] = Seq(Locale.getDefault)

  def localeVariantOpt: Option[String] = None

  def locale: Locale = LocaleUtil.getBestMatchLocale(acceptLanguages, availableLanguages, localeVariantOpt)

  def method(): HttpMethod = aggregatedHttpRequest.method()

  //https://www.playframework.com/documentation/2.8.x/ScalaBodyParsers
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
    Multipart.from(requestContext.request())
      .aggregate()
      .thenApply(mp => MultipartFormData(mp))
    
  def asMultipartFormData(fn: MultipartFormData => Result): Result =
    asMultipartFormData.thenApply(fn(_)).get

  //Raw Buffer - TODO - testcase needed and check for conformance
  def asRaw: HttpData = aggregatedHttpRequest.content()
}
