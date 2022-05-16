package com.greenfossil.webserver

case class Endpoint(url: String, method: String = "POST") {
  
  def absoluteUrl(authority: String, secure: Boolean): String =
    val protocol = if secure then "https" else "http"
    s"$protocol://$authority$url"
  
  def absoluteUrl(request: Request): String = 
    absoluteUrl(request.uriAuthority, request.secure)
  
}

object Endpoint {

  val Login = Endpoint("/login")

  val Logout = Endpoint("/logout")

  inline def apply(inline action: EssentialAction): Endpoint = EndpointMcr(action)

}
