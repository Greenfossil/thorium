package com.greenfossil.webserver

import com.greenfossil.commons.CryptoSupport
import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, CookieBuilder}

import scala.concurrent.duration.*

object CookieUtil {

  /**
    *
    * @param name
    * @param value
    * @return - A cookie builder, it will create secure cookie by default,
    *         to finish creating cookie by invoking build() method
    */
  def builder(name: String, value: String): CookieBuilder =
    Cookie.secureBuilder(name, value)

  def bakeCookie(name: String, value: String)(using request: Request): Cookie =
    val cookieConfiguration = request.httpConfiguration.cookieConfig
    bakeCookie(name, value, cookieConfiguration.maxAge.map(_.getSeconds))

  def bakeCookie(name: String, value: String, maxAgeOpt: Option[Long])(using request: Request): Cookie =
    val cookieConfiguration = request.httpConfiguration.cookieConfig
    bakeCookie(name = name, value = value, secure = cookieConfiguration.secure, maxAgeOpt = maxAgeOpt, path = cookieConfiguration.path,
      domainOpt = cookieConfiguration.domain, sameSiteOpt = cookieConfiguration.sameSite, httpOnly = cookieConfiguration.httpOnly, hostOnly = cookieConfiguration.hostOnly)

  def bakeDiscardCookie(name: String)(using request: Request): Cookie =
    bakeCookie(name , "", maxAgeOpt = Option(0L))

  def bakeSessionCookie(session: Session)(using request: Request): Option[Cookie] =
    val sessionConfiguration = request.httpConfiguration.sessionConfig
    bakeBase64URLEncodedCookie(sessionConfiguration.cookieName, session.data, sessionConfiguration.secure, sessionConfiguration.maxAge.map(_.getSeconds),
      sessionConfiguration.path, sessionConfiguration.domain, sessionConfiguration.sameSite, sessionConfiguration.httpOnly, false)

  def bakeFlashCookie(flash: Flash)(using request: Request): Option[Cookie] =
    val flashConfiguration = request.httpConfiguration.flashConfig
    bakeBase64URLEncodedCookie(flashConfiguration.cookieName, flash.data, flashConfiguration.secure, None,
      flashConfiguration.path, flashConfiguration.domain, flashConfiguration.sameSite, flashConfiguration.httpOnly, false)

  def bakeBase64URLEncodedCookie(name:String, data: Map[String, String], secure: Boolean, maxAgeOpt: Option[Long], path: String,
                                 domainOpt: Option[String], sameSiteOpt: Option["Strict" | "Lax" | "None"], httpOnly: Boolean, hostOnly: Boolean)
                                (using request: Request): Option[Cookie] =
    if data.isEmpty then None
    else
      val jwt = CryptoSupport.base64EncryptAES(request.httpConfiguration.secretConfig.secret, Json.toJson(data).toString)
      Option(bakeCookie(name, jwt, secure, maxAgeOpt, path, domainOpt, sameSiteOpt, httpOnly, hostOnly))

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
                 sameSiteOpt: Option["Strict" | "Lax" | "None"] = None,

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
    val cookieBuilder = Cookie.secureBuilder(name, value)
    cookieBuilder.secure(secure || sameSiteOpt.contains("None"))
    Option(path).filter(_.nonEmpty).map(cookieBuilder.path)
    maxAgeOpt.map(cookieBuilder.maxAge)
    domainOpt.map(cookieBuilder.domain)
    sameSiteOpt.map(cookieBuilder.sameSite)
    cookieBuilder.httpOnly(httpOnly)
    cookieBuilder.hostOnly(hostOnly)
    cookieBuilder.build()
}
