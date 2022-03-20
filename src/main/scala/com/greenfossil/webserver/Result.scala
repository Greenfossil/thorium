package com.greenfossil.webserver

import com.linecorp.armeria.common.{Cookie, HttpResponse, HttpStatus}

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.collection.immutable.TreeMap

///**
// * Case Insensitive Ordering. We first compare by length, then
// * use a case insensitive lexicographic order. This allows us to
// * use a much faster length comparison before we even start looking
// * at the content of the strings.
// */
//private object CaseInsensitiveOrdered extends Ordering[String] {
//  def compare(x: String, y: String): Int = {
//    val xl = x.length
//    val yl = y.length
//    if (xl < yl) -1 else if (xl > yl) 1 else x.compareToIgnoreCase(y)
//  }
//}
//
//class ResponseHeader(val status: Int,
//                    _headers: Map[String, String] = Map.empty,
//                     val reasonPhrase:Option[String] = None) {
//  val headers: Map[String, String] = TreeMap[String, String]()(CaseInsensitiveOrdered) ++ _headers
//
//
//  // validate headers so we know this response header is well formed
//  for ((name, value) <- headers) {
//    if (name eq null) throw new NullPointerException("Response header names cannot be null!")
//    if (value eq null) throw new NullPointerException(s"Response header '$name' has null value!")
//  }
//
//  def copy(
//            status: Int = status,
//            headers: Map[String, String] = headers,
//            reasonPhrase: Option[String] = reasonPhrase
//          ): ResponseHeader =
//    new ResponseHeader(status, headers, reasonPhrase)
//
//  override def toString = s"$status, $headers"
//  override def hashCode = (status, headers).hashCode
//  override def equals(o: Any) = o match {
//    case ResponseHeader(s, h, r) => (s, h, r).equals((status, headers, reasonPhrase))
//    case _                       => false
//  }
//
//}
//
//object ResponseHeader {
//  val basicDateFormatPattern = "EEE, dd MMM yyyy HH:mm:ss"
//  val httpDateFormat: DateTimeFormatter =
//    DateTimeFormatter
//      .ofPattern(basicDateFormatPattern + " 'GMT'")
//      .withLocale(java.util.Locale.ENGLISH)
//      .withZone(ZoneOffset.UTC)
//
//  def apply(
//             status: Int,
//             headers: Map[String, String] = Map.empty,
//             reasonPhrase: Option[String] = None
//           ): ResponseHeader =
//    new ResponseHeader(status, headers)
//  def unapply(rh: ResponseHeader): Option[(Int, Map[String, String], Option[String])] =
//    if (rh eq null) None else Some((rh.status, rh.headers, rh.reasonPhrase))
//}

object Result {

  def apply(body: HttpResponse | String): Result =
    new Result(body, None, None, Nil)

}


case class Result(body: HttpResponse | String,
                  newSessionOpt: Option[Session] = None,
                  newFlashOpt: Option[Flash] = None,
                  newCookies:Seq[Cookie]){

//  /**
//   * Adds headers to this result.
//   *
//   * For example:
//   * {{{
//   * Ok("Hello world").withHeaders(ETAG -> "0")
//   * }}}
//   *
//   * @param headers the headers to add to this result.
//   * @return the new result
//   */
//  def withHeaders(headers: (String, String)*): Result = {
//    copy(header = header.copy(headers = header.headers ++ headers))
//  }
//
//  /**
//   * Add a header with a DateTime formatted using the default http date format
//   * @param headers the headers with a DateTime to add to this result.
//   * @return the new result.
//   */
//  def withDateHeaders(headers: (String, ZonedDateTime)*): Result = {
//    copy(header = header.copy(headers = header.headers ++ headers.map {
//      case (name, dateTime) => (name, dateTime.format(ResponseHeader.httpDateFormat))
//    }))
//  }

//  /**
//   * Discards headers to this result.
//   *
//   * For example:
//   * {{{
//   * Ok("Hello world").discardingHeader(ETAG)
//   * }}}
//   *
//   * @param name the header to discard from this result.
//   * @return the new result
//   */
//  def discardingHeader(name: String): Result = {
//    copy(header = header.copy(headers = header.headers - name))
//  }

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
  def withCookies(cookies: Cookie*): Result = {
    val filteredCookies = newCookies.filter(cookie => !cookies.exists(_.name == cookie.name))
    if (cookies.isEmpty) this else copy(newCookies = filteredCookies ++ cookies)
  }

//  /**
//   * Discards cookies along this result.
//   *
//   * For example:
//   * {{{
//   * Redirect(routes.Application.index()).discardingCookies("theme")
//   * }}}
//   *
//   * @param cookies the cookies to discard along to this result
//   * @return the new result
//   */
//  def discardingCookies(cookies: DiscardingCookie*): Result = {
//    withCookies(cookies.map(_.toCookie): _*)
//  }

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
  def withSession(session: (String, String)*): Result = withSession(Session(session.toMap))

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
  def withNewSession: Result = withSession(Session())

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
  def flashing(values: (String, String)*): Result = flashing(Flash(values.toMap))

//  /**
//   * Changes the result content type.
//   *
//   * For example:
//   * {{{
//   * Ok("<text>Hello world</text>").as("application/xml")
//   * }}}
//   *
//   * @param contentType the new content type.
//   * @return the new result
//   */
//  def as(contentType: String): Result = copy(body = body.as(contentType))

//  /**
//   * @param request Current request
//   * @return The session carried by this result. Reads the request’s session if this result does not modify the session.
//   */
//  def session(implicit request: RequestHeader): Session = newSession.getOrElse(request.session)

//  /**
//   * Example:
//   * {{{
//   *   Ok.addingToSession("foo" -> "bar").addingToSession("baz" -> "bah")
//   * }}}
//   * @param values (key -> value) pairs to add to this result’s session
//   * @param request Current request
//   * @return A copy of this result with `values` added to its session scope.
//   */
//  def addingToSession(values: (String, String)*)(implicit request: RequestHeader): Result =
//    withSession(new Session(session.data ++ values.toMap))

//  /**
//   * Example:
//   * {{{
//   *   Ok.removingFromSession("foo")
//   * }}}
//   * @param keys Keys to remove from session
//   * @param request Current request
//   * @return A copy of this result with `keys` removed from its session scope.
//   */
//  def removingFromSession(keys: String*)(implicit request: RequestHeader): Result =
//    withSession(new Session(session.data -- keys))
//
//  override def toString = s"Result(${header})"

//
//  /**
//   * Encode the cookies into the Set-Cookie header. The session is always baked first, followed by the flash cookie,
//   * followed by all the other cookies in order.
//   */
//  def bakeCookies(
//                   cookieHeaderEncoding: CookieHeaderEncoding = new DefaultCookieHeaderEncoding(),
//                   sessionBaker: CookieBaker[Session] = new DefaultSessionCookieBaker(),
//                   flashBaker: CookieBaker[Flash] = new DefaultFlashCookieBaker(),
//                   requestHasFlash: Boolean = false
//                 ): Result = {
//
//    val allCookies = {
//      val setCookieCookies = cookieHeaderEncoding.decodeSetCookieHeader(header.headers.getOrElse(SET_COOKIE, ""))
//      val session = newSession.map { data =>
//        if (data.isEmpty) sessionBaker.discard.toCookie else sessionBaker.encodeAsCookie(data)
//      }
//      val flash = newFlash
//        .map { data =>
//          if (data.isEmpty) flashBaker.discard.toCookie else flashBaker.encodeAsCookie(data)
//        }
//        .orElse {
//          if (requestHasFlash) Some(flashBaker.discard.toCookie) else None
//        }
//      setCookieCookies ++ session ++ flash ++ newCookies
//    }
//
//    if (allCookies.isEmpty) {
//      this
//    } else {
//      withHeaders(SET_COOKIE -> cookieHeaderEncoding.encodeSetCookieHeader(allCookies))
//    }
//  }
}
