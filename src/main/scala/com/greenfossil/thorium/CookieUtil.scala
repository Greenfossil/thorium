/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, CookieBuilder}

import java.util.Base64
import scala.util.{Failure, Try}

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

  def cookieBuilder(config: CookieConfigurationLike, name: String, value: String): CookieBuilder =
    val cb = Cookie.secureBuilder(name, value)
      .secure(config.secure)
      .path(config.path)
      .httpOnly(config.httpOnly)
    config.hostOnly.foreach(cb.hostOnly)
    config.maxAge.foreach(dur => cb.maxAge(dur.getSeconds))
    config.sameSite.foreach(cb.sameSite)
    config.domain.foreach(cb.domain)
    cb

  /*
   * SessionCookie does not have hostOnly attribute
   */
  def sessionCookieBuilder(config: SessionConfiguration, secret: String, data: Map[String, String]): CookieBuilder =
    val jwt = AESUtil.encryptWithEmbeddedIV(Json.toJson(data).toString, secret, Base64.getEncoder)
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
    val jwt = AESUtil.encryptWithEmbeddedIV(Json.toJson(data).toString, secret, Base64.getEncoder)
    val cb = Cookie.secureBuilder(config.cookieName, jwt)
      .secure(config.secure)
      .path(config.path)
      .httpOnly(config.httpOnly)
    config.sameSite.foreach(ss => cb.sameSite(ss))
    config.domain.foreach(d => cb.domain(d))
    cb

  /*
   * FlashCookie does not have hostOnly, maxAge attributes
   */
  def csrfCookieBuilder(config: CSRFConfiguration, token: String): CookieBuilder =
    val cb = Cookie.secureBuilder(config.cookieName, token)
      .secure(config.secure)
      .path(config.path)
      .httpOnly(config.httpOnly)
    config.sameSite.foreach(ss => cb.sameSite(ss))
    config.domain.foreach(d => cb.domain(d))
    cb

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
    bakeDiscardCookie(request.httpConfiguration.cookieConfig, name)
    
  def bakeDiscardCookies(cookieConfiguration: CookieConfigurationLike, cookieNames: Seq[String]): Seq[Cookie] =
    cookieNames.map(bakeDiscardCookie(cookieConfiguration, _))

  def bakeDiscardCookies(cookieConfiguration: CookieConfigurationLike, cookieNames: Seq[String], domain: String): Seq[Cookie] =
    cookieNames.map(bakeDiscardCookie(cookieConfiguration, _, domain))

  def bakeDiscardCookie(cookieConfig: CookieConfigurationLike, name: String): Cookie =
    cookieBuilder(cookieConfig, name, "")
      .maxAge(0L)
      .build()

  def bakeDiscardCookie(cookieConfig: CookieConfigurationLike, name: String, domain: String): Cookie =
    cookieBuilder(cookieConfig, name, "")
      .maxAge(0L)
      .domain(domain)
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

  def decryptCookieValue(cookie: Cookie, appSecret: String): Option[Map[String, String]] = Try {
    val cookieValue = AESUtil.decryptWithEmbeddedIV(cookie.value(), appSecret, Base64.getDecoder)
    Json.parse(cookieValue).asOpt[Map[String, String]]
  }.recoverWith { case e =>
    requestLogger.trace(s"Failed to decrypt the retrieved cookie: [${cookie.name()}] -> [${cookie.value()}]", e)
    Failure(e)
  }.toOption.flatten  