package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.Cookie

object CookieUtil {
  /*
   * Cookie's path must be set to an appropriate uri
   * Defaults to "/" so cookie will follow to all sub-directories
   * https://www.rfc-editor.org/rfc/rfc6265#section-5.1.4
   */
  def bakeCookie(name: String, value: String, secure: Boolean, maxAgeOpt: Option[Long],
                 path: String, domainOpt: Option[String]): Cookie =
    bakeCookie(name = name, value = value, secure = secure, maxAgeOpt = maxAgeOpt, path = path, domainOpt = domainOpt,
      sameSiteOption = None, httpOnly = true, hostOnly = false)


  def bakeCookie(name: String, value: String, secure: Boolean, maxAgeOpt: Option[Long], httpOnly: Boolean,
                 path: String, domainOpt: Option[String]): Cookie =
    bakeCookie(name = name, value = value, secure = secure, maxAgeOpt = maxAgeOpt, path = path, domainOpt = domainOpt,
      sameSiteOption = None, httpOnly = httpOnly, hostOnly = false)

  def bakeCookie(name: String, value: String, secure: Boolean, maxAgeOpt: Option[Long], httpOnly: Boolean, hostOnly: Boolean,
                 path: String, domainOpt: Option[String]): Cookie =
    bakeCookie(name = name, value = value, secure = secure, maxAgeOpt = maxAgeOpt, path = path, domainOpt = domainOpt,
      sameSiteOption = None, httpOnly = httpOnly, hostOnly = hostOnly)

  /*
   * source - https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies
   * - https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/cookies/Cookie
   */
  def bakeCookie(name: String,
                 value: String,
                 /*
                  * A cookie with the Secure attribute is only sent to the server with an encrypted request over
                  * the HTTPS protocol. It's never sent with unsecured HTTP (except on localhost),
                  * which means man-in-the-middle attackers can't access it easily.
                  * Insecure sites (with http: in the URL) can't set cookies with the Secure attribute.
                  * However, don't assume that Secure prevents all access to sensitive information in cookies.
                  * For example, someone with access to the client's hard disk (or JavaScript if
                  * the HttpOnly attribute isn't set) can read and modify the information.
                  */
                 secure: Boolean,
                 maxAgeOpt: Option[Long],

                 /*
                  * The Path attribute indicates a URL path that must exist in the requested URL in order to
                  * send the Cookie header. The %x2F ("/") character is considered a directory separator,
                  * and subdirectories match as well.
                  */
                 path: String,

                 /*
                  * The Domain attribute specifies which hosts can receive a cookie. If unspecified,
                  * the attribute defaults to the same host that set the cookie, excluding subdomains.
                  * If Domain is specified, then subdomains are always included. Therefore, specifying Domain is
                  * less restrictive than omitting it. However, it can be helpful when subdomains
                  * need to share information about a user.
                  */
                 domainOpt: Option[String],
                 /*
                  * The SameSite attribute lets servers specify whether/when cookies are sent
                  * with cross-site requests (where Site is defined by the registrable domain and
                  * the scheme: http or https).
                  * This provides some protection against cross-site request forgery attacks (CSRF).
                  * It takes three possible values: Strict, Lax, and None.
                  *
                  * With Strict, the cookie is only sent to the site where it originated. Lax is similar,
                  * except that cookies are sent when the user navigates to the cookie's origin site.
                  * For example, by following a link from an external site. None specifies that cookies
                  * are sent on both originating and cross-site requests, but only in secure contexts
                  * (i.e., if SameSite=None then the Secure attribute must also be set).
                  * If no SameSite attribute is set, the cookie is treated as Lax.
                  */
                 sameSiteOption: Option["Strict" | "Lax" | "None"] = None ,

                 /*
                  * Use the HttpOnly attribute to prevent access to cookie values via JavaScript.
                  *
                  * A boolean, true if the cookie is marked as HttpOnly (i.e. the cookie is inaccessible
                  * to client-side scripts), or false otherwise.
                  */
                 httpOnly: Boolean,

                 /*
                  * A boolean, true if the cookie is a host-only cookie (i.e. the request's host must
                  * exactly match the domain of the cookie), or false otherwise.
                  */
                 hostOnly: Boolean
                ): Cookie =
    val cookieBuilder =
      if secure || sameSiteOption.contains("None")
      then Cookie.secureBuilder(name, value)
      else Cookie.builder(name, value)
    Option(path).filter(_.nonEmpty).map(cookieBuilder.path)
    maxAgeOpt.map(cookieBuilder.maxAge)
    domainOpt.map(cookieBuilder.domain)
    sameSiteOption.map(cookieBuilder.sameSite)
    cookieBuilder.httpOnly(httpOnly)
    cookieBuilder.hostOnly(hostOnly)
    cookieBuilder.build()

  def bakeCookie(name: String, value: String): Cookie =
    bakeCookie(name, value, false, None, "/", None)

  def bakeCookie(name: String, value: String, path: String): Cookie =
    bakeCookie(name, value, false, None, path, None)

  def bakeCookie(name: String, value: String, maxAge: Long): Cookie =
    bakeCookie(name, value, maxAge, false)

  def bakeCookie(name: String, value: String, secure: Boolean): Cookie =
    bakeCookie(name, value, secure, None, "/", None)

  def bakeCookie(name: String, value: String, maxAge: Long, secure: Boolean): Cookie =
    bakeCookie(name , value, secure, Some(maxAge), "/", None)

  def bakeDiscardCookie(name: String): Cookie =
    bakeDiscardCookie(name, secure = false, path = "/" )

  def bakeDiscardCookie(name: String, secure: Boolean, path: String): Cookie =
    bakeCookie(name , "", secure, maxAgeOpt = Option(0L), path, None)

  def bakeSessionCookie(session: Session): Option[Cookie] =
    bakeBase64URLEncodedCookie(name = RequestAttrs.Session.name(), data = session.data, path = "/")

  def bakeFlashCookie(flash: Flash): Option[Cookie] =
    bakeBase64URLEncodedCookie(name = RequestAttrs.Flash.name(), data = flash.data, path = "/")

  def bakeBase64URLEncodedCookie(name:String, data: Map[String, String], path: String): Option[Cookie] =
    if data.isEmpty then None
    else
      val jwt = Json.toJson(data).encodeBase64URL
      Option(bakeCookie(name,jwt, path))
}
