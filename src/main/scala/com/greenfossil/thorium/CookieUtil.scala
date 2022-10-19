package com.greenfossil.thorium

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, CookieBuilder}

import scala.concurrent.duration.*

/*
 * source - https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies
 * - https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/cookies/Cookie
 *
 * Attribute: Secure
 *   A cookie with the Secure attribute is only sent to the server with an encrypted request over
 *   the HTTPS protocol. It's never sent with unsecured HTTP (except on localhost),
 *   which means man-in-the-middle attackers can't access it easily.
 *   Insecure sites (with http: in the URL) can't set cookies with the Secure attribute.
 *   However, don't assume that Secure prevents all access to sensitive information in cookies.
 *   For example, someone with access to the client's hard disk (or JavaScript if
 *   the HttpOnly attribute isn't set) can read and modify the information.
 *
 * Attribute: Path
 *   The Path attribute indicates a URL path that must exist in the requested URL in order to
 *   send the Cookie header. The %x2F ("/") character is considered a directory separator,
 *   and subdirectories match as well.
 *
 * Attribute: Domain
 *   The Domain attribute specifies which hosts can receive a cookie. If unspecified,
 *   the attribute defaults to the same host that set the cookie, excluding subdomains.
 *   If Domain is specified, then subdomains are always included. Therefore, specifying Domain is
 *   less restrictive than omitting it. However, it can be helpful when subdomains
 *   need to share information about a user.
 *
 * Attribute: SameSite
 *   The SameSite attribute lets servers specify whether/when cookies are sent
 *   with cross-site requests (where Site is defined by the registrable domain and
 *   the scheme: http or https).
 *   This provides some protection against cross-site request forgery attacks (CSRF).
 *   It takes three possible values: Strict, Lax, and None.
 *
 *   With Strict, the cookie is only sent to the site where it originated. Lax is similar,
 *   except that cookies are sent when the user navigates to the cookie's origin site.
 *   For example, by following a link from an external site. None specifies that cookies
 *   are sent on both originating and cross-site requests, but only in secure contexts
 *   (i.e., if SameSite=None then the Secure attribute must also be set).
 *   If no SameSite attribute is set, the cookie is treated as Lax.
 *
 * Attribute: httpOnly
 *  Use the HttpOnly attribute to prevent access to cookie values via JavaScript.
 *
 *  A boolean, true if the cookie is marked as HttpOnly (i.e. the cookie is inaccessible
 *  to client-side scripts), or false otherwise.
 *
 * Attribute: hostOnly
 * A boolean, true if the cookie is a host-only cookie (i.e. the request's host must
 * exactly match the domain of the cookie), or false otherwise.
 */
object CookieUtil:

  def cookieBuilder(config: CookieConfiguration, name: String, value: String): CookieBuilder =
    val cb = Cookie.secureBuilder(name, value)
      .secure(config.secure)
      .path(config.path)
      .httpOnly(config.httpOnly)
      .hostOnly(config.hostOnly)
    config.maxAge.foreach(dur => cb.maxAge(dur.getSeconds))
    config.sameSite.foreach(ss => cb.sameSite(ss))
    config.domain.foreach(d => cb.domain(d))
    cb

  /*
   * SessionCookie does not have hostOnly attribute
   */
  def sessionCookieBuilder(config: SessionConfiguration, secret: String, data: Map[String, String]): CookieBuilder =
    val jwt = AESUtil.encrypt(secret, Json.toJson(data).toString)
    val cb = Cookie.secureBuilder(config.cookieName, jwt)
      .secure(config.secure)
      .path(config.path)
      .httpOnly(config.httpOnly)
    config.maxAge.foreach(dur => cb.maxAge(dur.getSeconds))
    config.sameSite.foreach(ss => cb.sameSite(ss))
    config.domain.foreach(d => cb.domain(d))
    cb

  /*
   * FlashCookie does not have hostOnly, maxAge attributes
   */
  def flashCookieBuilder(config: FlashConfiguration, secret: String, data: Map[String, String]): CookieBuilder =
    val jwt = AESUtil.encrypt(secret, Json.toJson(data).toString)
    val cb = Cookie.secureBuilder(config.cookieName, jwt)
      .secure(config.secure)
      .path(config.path)
      .httpOnly(config.httpOnly)
    config.sameSite.foreach(ss => cb.sameSite(ss))
    config.domain.foreach(d => cb.domain(d))
    cb

  /**
    *
    * @param name
    * @param value
    * @return - A cookie builder, it will create secure cookie by default,
    *         to finish creating cookie by invoking build() method
    */
  @deprecated("use cookieBuilder instead")
  def builder(name: String, value: String): CookieBuilder =
    cookieBuilder(name, value)

  def cookieBuilder(name: String, value: String): CookieBuilder =
    cookieBuilder(Configuration.apply().httpConfiguration.cookieConfig, name, value)

  def bakeCookie(name: String, value: String)(using request: Request): Cookie =
    cookieBuilder(request.httpConfiguration.cookieConfig, name, value)
      .build()

  def bakeCookie(name: String, value: String, maxAgeOpt: Option[Long])(using request: Request): Cookie =
    val cb = cookieBuilder(request.httpConfiguration.cookieConfig, name, value)
    maxAgeOpt.foreach(dur => cb.maxAge(dur))
    cb.build()

  def bakeDiscardCookie(name: String)(using request: Request): Cookie =
    cookieBuilder(request.httpConfiguration.cookieConfig, name, "")
      .maxAge(0L)
      .build()

  def bakeSessionCookie(session: Session)(using request: Request): Option[Cookie] =
    session.data.headOption.map{ _ =>
      sessionCookieBuilder(request.httpConfiguration.sessionConfig, request.httpConfiguration.secretConfig.secret, session.data)
        .build()
    }

  def bakeFlashCookie(flash: Flash)(using request: Request): Option[Cookie] =
    flash.data.headOption.map{_ =>
      flashCookieBuilder(request.httpConfiguration.flashConfig, request.httpConfiguration.secretConfig.secret, flash.data)
        .build()
    }