package com.greenfossil.thorium

import java.nio.charset.StandardCharsets

case class Endpoint(path: String, method: String, queryParams: List[(String, Any)]):

  def url: String =
    //Query Param is expected to be UrlEncoded
    queryParams match
      case Nil => path
      case _ => path +
        queryParams
          .map(kv => s"${Endpoint.paramKeyValue(kv._1, kv._2)}")
          .mkString("?", "&", "")

  def absoluteUrl(authority: String, secure: Boolean): String =
    val protocol = if secure then "https" else "http"
    s"$protocol://$authority$url"
  
  def absoluteUrl(using request: Request): String =
    absoluteUrl(request.uriAuthority, request.secure)

object Endpoint:

  def apply(path: String): Endpoint = new Endpoint(path, "GET", Nil)

  def paramKeyValue(name: String, value: Any): String =
    (name, value) match
      case (name, x :: Nil) => paramKeyValue(name, x)
      case (name, xs: Seq[Any]) => xs.map(x => s"${name}[]=${x.toString}").mkString("&")
      case (name, x: Any) => s"$name=${x.toString}"

  def paramKeyValueUrlEncoded(name: String, value: Any): String =
    (name, value) match
      case (name, x :: Nil) => paramKeyValueUrlEncoded(name, x)
      case (name, xs: Seq[Any]) => xs.map(x => s"${urlencode(name)}[]=${urlencode(x.toString)}").mkString("&")
      case (name, x: Any) => s"${urlencode(name)}=${urlencode(x.toString)}"

  def urlencode(value: String): String =
    java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString)
