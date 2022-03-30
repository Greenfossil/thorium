package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.AnnotatedHttpService


case class Endpoint(url: String, method: String = "POST") {
  
  def absoluteUrl(host: String, secure: Boolean): String =
    val protocol = if secure then "https" else "http"
    s"$protocol://$host/$secure"
  
  def absoluteUrl(request: Request): String = ??? //absoluteUrl(request.host, request.secure)
  
}

object Endpoint {

  inline def apply(inline action: AnnotatedHttpService): Endpoint = EndpointMcr(action)

  inline def apply[A <: Controller](inline fn: A => AnnotatedHttpService): Endpoint = ???

  @deprecated("Use Endpoint()", "")
  inline def path(inline action: AnnotatedHttpService): Endpoint = EndpointMcr(action)


}
