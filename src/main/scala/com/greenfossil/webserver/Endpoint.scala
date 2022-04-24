package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.AnnotatedHttpService


case class Endpoint(url: String, method: String = "POST") {
  
  def absoluteUrl(authority: String, secure: Boolean): String =
    val protocol = if secure then "https" else "http"
    s"$protocol://$authority/$secure"
  
  def absoluteUrl(request: Request): String = 
    absoluteUrl(request.uriAuthority, "https".equalsIgnoreCase(request.uriScheme))
  
}

object Endpoint {

  val Login = Endpoint("/login")

  val Logout = Endpoint("/logout")

  inline def apply(inline action: AnnotatedHttpService): Endpoint = EndpointMcr(action)

  //FIXME
  inline def apply[A <: Controller](inline fn: A => AnnotatedHttpService): Endpoint = ???

  @deprecated("Use Endpoint()", "")
  inline def path(inline action: AnnotatedHttpService): Endpoint = EndpointMcr(action)


}
