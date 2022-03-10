package com.greenfossil.webserver

import com.greenfossil.commons.json.{JsValue, Json}
import com.linecorp.armeria.common.{Cookie, HttpData, RequestHeaders}
import com.linecorp.armeria.common.multipart.AggregatedMultipart
import com.linecorp.armeria.server.ServiceRequestContext

import java.util.Locale
import java.util.Locale.LanguageRange

trait Request(val requestContext: ServiceRequestContext) {
  export requestContext.*

//Overlap Play and Armeria api  
//  def path: String
//  def method: String = ""
//  def remoteAddress[A <: java.net.SocketAddress]: A /*String*/ = requestContext.remoteAddress()

  def uri: java.net.URI  /*String*/ = request().uri()
  
  def headers: RequestHeaders /*Headers*/ = request().headers()

  //https://www.javatips.net/api/java.util.locale.languagerange
  import scala.jdk.CollectionConverters.*
  def acceptLanguages: Seq[LanguageRange] = request().acceptLanguages().asScala.toSeq

  def cookies: Set[Cookie] =
    request().headers().cookies().asInstanceOf[java.util.Set[Cookie]].asScala.toSet
  
  def session: Session = Session(Map.empty) //FIXME

  @deprecated("use remoteAddress instead")
  def getRealIP: String = "FIXME"

  //  def getReferer: Option[String] =
//    headers.get("X-Alt-Referer").orElse(headers.get("referer"))
//
  def flash: Flash = ???

  def locale: Locale = {
//    import scala.jdk.CollectionConverters.*
//    val config: Config = AppSettings.instance.config
//    val _appLangs: util.List[String] = config.getStringList("app.i18n.langs")
//    def appLangs: List[String] = _appLangs.asScala.toList
//    val variant = config.getString("app.i18n.variant")
//    val supportedLanguages: Seq[String] = ??? //To be used request.acceptLanguages
//
//    val lang: String = Locale.lookupTag(request.acceptLanguages, _appLangs)
//
//    //PreferLang is constrained by what the app will support
//    val preferLang = if (appLangs.exists(_.equalsIgnoreCase(lang))) lang else appLangs.headOption.getOrElse("en")
//      new Locale.Builder().setLanguageTag(preferLang).setVariant(variant).build()
    ???
  }
//
//
//  def isXhr: Boolean

  //https://www.playframework.com/documentation/2.8.x/ScalaBodyParsers
  lazy val asText: String = request().aggregate().join().contentUtf8()

  //application/json
  lazy val asJson: JsValue = Json.parse(asText)

  //application/x-www-form-urlencoded
  lazy val asFormUrlEncoded: Map[String, Seq[String]] =
    FormUrlEncodedParser.parse(asText)

  //MultiPart
  import com.linecorp.armeria.common.multipart.Multipart
  def asMultipartFormData: MultipartFormData =
    MultipartFormData(Multipart.from(requestContext.request()).aggregate().join())

  //Raw Buffer - TODO - testcase needed and check for conformance
  def asRaw: HttpData =
    requestContext.request().aggregate().join().content()
}

//trait PlayRequest[+A](val requestContext: ServiceRequestContext) {
//
//  /**
//   * Body content
//   */
//  def body[A]: A
//
//  def path: String
//  def uri: String
//  def method: String
//  def headers: Headers
//  def remoteAddress: String
//
//  @deprecated("use remoteAddress instead")
//  def getRealIP: String
//
//  //https://www.javatips.net/api/java.util.locale.languagerange
//  def acceptLanguages: Seq[LanguageRange]
//
//  def cookies: Set[Cookie]
//  def getReferer: Option[String] =  headers.get("X-Alt-Referer").orElse(headers.get("referer"))
//  def flash: Flash
//
//  def session: Session // = Session(Map.empty) //FIXME
//
//  def isXhr: Boolean
//
//}