package com.greenfossil.webserver

import java.nio.charset.StandardCharsets

case class Endpoint(path: String, method: String, queryParams: List[(String, Any)]) {

  //TODO - the query-string is not urlencoded
  def url: String = queryParams match {
    case Nil => path
    case _ => path +
      queryParams
        .map(kv => s"${Endpoint.paramKeyValue(kv._1, kv._2)}")
        .mkString("?", "&", "")
  }

  def absoluteUrl(authority: String, secure: Boolean): String =
    val protocol = if secure then "https" else "http"
    s"$protocol://$authority$url"
  
  def absoluteUrl(using request: Request): String =
    absoluteUrl(request.uriAuthority, request.secure)

}

object Endpoint {

  val Login: Endpoint = apply("/login")

  val Logout: Endpoint = apply("/logout")

  def apply(path: String): Endpoint = new Endpoint(path, "GET", Nil)

  @deprecated("to be removed - use extension EssentialAction.endpoint instead")
  inline def apply(inline action: EssentialAction): Endpoint =
    EndpointMcr(action)

  def paramKeyValue(name: String, value: Any): String =
    (name, value) match
      case (name, x :: Nil) => paramKeyValue(name, x)
      case (name, xs: Seq[Any]) => xs.map(x => s"${name}[]=${x.toString}").mkString("&")
      case (name, x: Any) => s"$name=${x.toString}"

  def paramKeyValueUrlEncoded(name: String, value: Any): String =
    (name, value) match
      case (name, x :: Nil) => paramKeyValueUrlEncoded(name, x)
      case (name, xs: Seq[Any]) => xs.map(x => s"${name}[]=${urlencode(x.toString)}").mkString("&")
      case (name, x: Any) => s"$name=${urlencode(x.toString)}"

  def urlencode(value: String): String =
    java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString)

}
