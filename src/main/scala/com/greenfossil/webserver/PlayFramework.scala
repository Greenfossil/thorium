package com.greenfossil.webserver

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.ServiceRequestContext

import java.util.Locale.LanguageRange

object Call {
//  def apply(url: String): Call = Call(url)

  def apply(method: String, url: String): String = ???
}

//TODO - should this be Endpoint
case class Call(url: String) {
  override def toString: String = url
  def absoluteURL: String = url // FIXME
  def absoluteURL(secure: Boolean): String = ???
}

