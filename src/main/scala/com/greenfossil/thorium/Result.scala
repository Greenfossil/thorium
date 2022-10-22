package com.greenfossil.thorium

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, HttpResponse, MediaType}
import io.netty.util.AsciiString

import java.io.InputStream
import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import scala.collection.immutable.TreeMap
import scala.util.Using

private object CaseInsensitiveOrdered extends Ordering[String]:
  def compare(left: String, right: String): Int =
    left.compareToIgnoreCase(right)

object ResponseHeader:
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

case class ResponseHeader(headers: TreeMap[String, String], reasonPhrase:Option[String] = None)

object Result:

  def apply(body: ActionResponse): Result =
    body match
      case bodyResult: Result => bodyResult
      case body: (HttpResponse | String | Array[Byte] | InputStream) =>
        new Result(ResponseHeader(Map.empty), body, None, None, Nil, None)

case class Result(header: ResponseHeader,
                  body: HttpResponse | String | Array[Byte] | InputStream,
                  newSessionOpt: Option[Session] = None,
                  newFlashOpt: Option[Flash] = None,
                  newCookies:Seq[Cookie],
                  contentTypeOpt: Option[MediaType] = None
                 ):

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
  def withHeaders(headers: (String | AsciiString, String)*): Result =
    val _headers = headers.map(tup2 => tup2._1.toString -> tup2._2)
    copy(header = header.copy(headers = header.headers ++ _headers))

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

  def as(contentType: MediaType): Result =
    copy(contentTypeOpt = Some(contentType))

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
  def discardingCookies[A <: String | Cookie](cookies: A*)(using request: Request): Result =
    val _cookies: Seq[Cookie] = cookies.map{
        case name: String => CookieUtil.bakeCookie(name, "", Option(0L))
        case c: Cookie => c
    }
    withCookies(_cookies*)

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
  def flashing(flash: Flash): Result =
    copy(newFlashOpt = Some(flash))

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

