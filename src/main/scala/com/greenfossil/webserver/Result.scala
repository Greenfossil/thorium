package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, HttpResponse, HttpStatus}

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import scala.collection.immutable.TreeMap

/**
 * Case Insensitive Ordering. We first compare by length, then
 * use a case insensitive lexicographic order. This allows us to
 * use a much faster length comparison before we even start looking
 * at the content of the strings.
 */
private object CaseInsensitiveOrdered extends Ordering[String] {
  def compare(x: String, y: String): Int = {
    val xl = x.length
    val yl = y.length
    if xl < yl then -1 else if (xl > yl) 1 else x.compareToIgnoreCase(y)
  }
}

object ResponseHeader {
  val basicDateFormatPattern = "EEE, dd MMM yyyy HH:mm:ss"
  val httpDateFormat: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern(basicDateFormatPattern + " 'GMT'")
      .withLocale(java.util.Locale.ENGLISH)
      .withZone(ZoneOffset.UTC)

  def apply(headers: Map[String, String]): ResponseHeader = apply(headers, null)

  def apply(headers: Map[String, String], reasonPhrase: String): ResponseHeader =
    val ciHeaders = TreeMap[String, String]()(CaseInsensitiveOrdered) ++ headers
    new ResponseHeader(ciHeaders, Option(reasonPhrase))
}

case class ResponseHeader(headers: TreeMap[String, String], reasonPhrase:Option[String] = None)


object Result {

  def apply(body: HttpResponse | String | Array[Byte]): Result =
    new Result(ResponseHeader(Map.empty), body, Map.empty, None, None, Nil)

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

case class Result(header: ResponseHeader,
                  body: HttpResponse | String | Array[Byte],
                  queryString: Map[String, Seq[String]] = Map.empty,
                  newSessionOpt: Option[Session] = None,
                  newFlashOpt: Option[Flash] = None,
                  newCookies:Seq[Cookie]){

  /**
   * Adds headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers the headers to add to this result.
   * @return the new result
   */
  def withHeaders(headers: (String, String)*): Result = 
    copy(header = header.copy(headers = header.headers ++ headers))

  /**
   * Add a header with a DateTime formatted using the default http date format
   * @param headers the headers with a DateTime to add to this result.
   * @return the new result.
   */
  def withDateHeaders(headers: (String, ZonedDateTime)*): Result = 
    copy(header = header.copy(headers = header.headers ++ headers.map {
      case (name, dateTime) => (name, dateTime.format(ResponseHeader.httpDateFormat))
    }))

  /**
   * Discards headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingHeader(ETAG)
   * }}}
   *
   * @param name the header to discard from this result.
   * @return the new result
   */
  def discardingHeader(name: String): Result = 
    copy(header = header.copy(headers = header.headers - name))

  /**
   * Adds cookies to this result. If the result already contains cookies then cookies with the same name in the new
   * list will override existing ones.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withCookies(Cookie("theme", "blue"))
   * }}}
   *
   * @param cookies the cookies to add to this result
   * @return the new result
   */
  def withCookies(cookies: Cookie*): Result = 
    val filteredCookies = newCookies.filter(cookie => !cookies.exists(_.name == cookie.name))
    if cookies.isEmpty then this else copy(newCookies = filteredCookies ++ cookies)
  
  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).discardingCookies("theme")
   * }}}
   *
   * @param cookiesName the cookies to discard along to this result
   * @return the new result
   */
  def discardingCookies[A <: String | Cookie](cookies: A*): Result = {
    val _cookies: Seq[Cookie] = cookies.map{
        case name:String => Result.bakeCookie(name, "", 0)
        case c: Cookie => c
    }
    withCookies(_cookies*)
  }

  /**
   * Sets a new session for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withSession(session + ("saidHello" -> "true"))
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: Session): Result =
    copy(newSessionOpt = Some(session))

  /**
   * Sets a new session for this result, discarding the existing session.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withSession("saidHello" -> "yes")
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: (String, String)*): Result = 
    withSession(Session(session.toMap))

  /**
   * Discards the existing session for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).withNewSession
   * }}}
   *
   * @return the new result
   */
  def withNewSession: Result = 
    withSession(Session())

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).flashing(flash + ("success" -> "Done!"))
   * }}}
   *
   * @param flash the flash scope to set with this result
   * @return the new result
   */
  //TODO - check if need to warnFlashingIfNotRedirect
  def flashing(flash: Flash): Result = {
//    Result.warnFlashingIfNotRedirect(flash, header)
    copy(newFlashOpt = Some(flash))
  }

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Redirect(routes.Application.index()).flashing("success" -> "Done!")
   * }}}
   *
   * @param values the flash values to set with this result
   * @return the new result
   */
  def flashing(values: (String, String)*): Result = 
    flashing(Flash(values.toMap))

  /**
   * @param request Current request
   * @return The session carried by this result. Reads the request’s session if this result does not modify the session.
   */
  def session(using request: Request): Session = 
    newSessionOpt.getOrElse(request.session)

  /**
   * Example:
   * {{{
   *   Ok.addingToSession("foo" -> "bar").addingToSession("baz" -> "bah")
   * }}}
   * @param values (key -> value) pairs to add to this result’s session
   * @param request Current request
   * @return A copy of this result with `values` added to its session scope.
   */
  def addingToSession(values: (String, String)*)(using request: Request): Result =
    withSession(new Session(request.session.data ++ values.toMap))

  /**
   * Example:
   * {{{
   *   Ok.removingFromSession("foo")
   * }}}
   * @param keys Keys to remove from session
   * @param request Current request
   * @return A copy of this result with `keys` removed from its session scope.
   */
  def removingFromSession(keys: String*)(using request: Request): Result =
    withSession(new Session(request.session.data -- keys))

  override def toString = s"Result($header)"

  def toHttpResponse(req: Request): HttpResponse =
    val httpResp = body match
      case httpResponse: HttpResponse => httpResponse
      case bytes: Array[Byte] => HttpResponse.of(HttpStatus.OK, req.contentType, bytes)
      case string: String => HttpResponse.of(string)

    /*
     * Forward Session, if not new values
     * Discard session is newSession isEmpty or append new values
     */
    val sessionCookieOption: Option[Cookie] = newSessionOpt.map{ newSession =>
        //If newSession isEmtpy, expire session cookie
        if newSession.isEmpty then
          Result.bakeDiscardCookie(RequestAttrs.Session.name())
        else
          //Append new session will to session cookie
          val session = req.session + newSession
          Result.bakeSessionCookie(session).orNull
    }


    val flashCookieOpt: Option[Cookie] = newFlashOpt.flatMap{newFlash =>
      //Create a new flash cookie
      Result.bakeFlashCookie(newFlash)
    }.orElse{
      //Expire the current flash cookie
      if req.flash.nonEmpty
      then Some(Result.bakeDiscardCookie(RequestAttrs.Flash.name()))
      else None
    }

    val httpRespWithCookies = (sessionCookieOption ++ flashCookieOpt).toList ++ newCookies match {
      case Nil => httpResp
      case cookies => httpResp.mapHeaders(_.toBuilder.cookies(cookies*).build())
    }
    
    val httpRespWithHeaders = httpRespWithCookies.mapHeaders(_.withMutations{builder =>
      header.headers.map{header =>
        builder.set(header._1, header._2)
      }
    })
    
    httpRespWithHeaders

}
