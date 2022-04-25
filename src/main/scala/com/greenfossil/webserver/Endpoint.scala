package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.AnnotatedHttpService


case class Endpoint(url: String, method: String = "POST") {
  
  def absoluteUrl(authority: String, secure: Boolean): String =
    val protocol = if secure then "https" else "http"
    s"$protocol://$authority$url"
  
  def absoluteUrl(request: Request): String = 
    absoluteUrl(request.uriAuthority, "https".equalsIgnoreCase(request.uriScheme))
  
}

object Endpoint {

  val Login = Endpoint("/login")

  val Logout = Endpoint("/logout")

  inline def apply(inline action: AnnotatedHttpService): Endpoint = EndpointMcr(action)

}
