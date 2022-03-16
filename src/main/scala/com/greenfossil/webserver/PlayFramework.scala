package com.greenfossil.webserver

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.ServiceRequestContext

import java.util.Locale.LanguageRange


//class Headers(var _headers: Seq[(String, String)]) {
//  def headers: Seq[(String, String)] = _headers
//
//  /**
//   * Optionally returns the first header value associated with a key.
//   */
//  def get(key: String): Option[String] = ???
//}
//
//trait Cookie {
//  def name: String
//}

case class DiscardingCookie(name: String)

object Call {
//  def apply(url: String): Call = Call(url)

  def apply(method: String, url: String): String = ???
}

//TODO - should this be Endpoint
case class Call(url: String) {
  override def toString: String = url
  def absoluteURL: String = ???
  def absoluteURL(secure: Boolean): String = ???
}

