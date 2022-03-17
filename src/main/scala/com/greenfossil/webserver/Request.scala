package com.greenfossil.webserver

import com.greenfossil.commons.json.{JsObject, JsValue, Json}
import com.linecorp.armeria.common.{AggregatedHttpRequest, Cookie, HttpData, HttpMethod, RequestHeaders}
import com.linecorp.armeria.common.multipart.AggregatedMultipart
import com.linecorp.armeria.server.ServiceRequestContext

import java.time.ZoneId
import java.util.{Base64, Locale}
import java.util.Locale.LanguageRange
import scala.util.Try

object RequestAttrs {
  import io.netty.util.AttributeKey
  val TZ = AttributeKey.valueOf[ZoneId]("tz")
  val Session = AttributeKey.valueOf[Session]("session")
  val Flash = AttributeKey.valueOf[Flash]("flash")
}

trait Request(val requestContext: ServiceRequestContext, aggregatedHttpRequest: AggregatedHttpRequest) {
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
    Json.parseBase64URL(c.value()).asOpt[Map[String, String]].map(Session(_))
  }.getOrElse(Session.newSession)

  def flash: Flash = cookies.find(c => c.name() == RequestAttrs.Flash.name()).flatMap{c =>
    Json.parseBase64URL(c.value()).asOpt[Map[String, String]].map(Flash(_))
  }.getOrElse(Flash.empty)

  @deprecated("use remoteAddress instead")
  def getRealIP: String = "FIXME"

  //  def getReferer: Option[String] =
//    headers.get("X-Alt-Referer").orElse(headers.get("referer"))
//

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
  def asMultipartFormData: MultipartFormData =
    MultipartFormData(Multipart.from(requestContext.request()).aggregate().join())

  //Raw Buffer - TODO - testcase needed and check for conformance
  def asRaw: HttpData =
    requestContext.request().aggregate().join().content()
}
