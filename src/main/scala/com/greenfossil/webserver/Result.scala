package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, HttpResponse, HttpStatus, MediaType}

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import scala.collection.immutable.TreeMap
import scala.util.Try

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
    new Result(ResponseHeader(Map.empty), body, Map.empty, None, None, Nil, None)

}

case class Result(header: ResponseHeader,
                  body: HttpResponse | String | Array[Byte],
                  queryString: Map[String, Seq[String]] = Map.empty,
                  newSessionOpt: Option[Session] = None,
                  newFlashOpt: Option[Flash] = None,
                  newCookies:Seq[Cookie],
                  contentTypeOpt: Option[MediaType] = None
                 ){

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
  def discardingCookies[A <: String | Cookie](cookies: A*)(using request: Request): Result = {
    val _cookies: Seq[Cookie] = cookies.map{
        case name: String => CookieUtil.bakeCookie(name, "", Option(0L))
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

  /*
   * Forward Session, if not new values
   * Discard session is newSession isEmpty or append new values
   */
  private def getNewSessionCookie(using req: Request): Option[Cookie] = newSessionOpt.map{ newSession =>
    //If newSession isEmtpy, expire session cookie
    if newSession.isEmpty then
      CookieUtil.bakeDiscardCookie(req.httpConfiguration.sessionConfig.cookieName)
    else
      //Append new session will to session cookie
      val session = req.session + newSession
      CookieUtil.bakeSessionCookie(session).orNull
  }

  private def getNewFlashCookie(using req: Request): Option[Cookie] = newFlashOpt.flatMap{newFlash =>
    //Create a new flash cookie
    CookieUtil.bakeFlashCookie(newFlash)
  }.orElse{
    //Expire the current flash cookie
    if req.flash.nonEmpty
    then Some(CookieUtil.bakeDiscardCookie(req.httpConfiguration.flashConfig.cookieName))
    else None
  }

  private def getAllCookies(using req: Request): Seq[Cookie] = (getNewSessionCookie ++ getNewFlashCookie).toList ++ newCookies

  private def addCookiesToHttpResponse(cookies: Seq[Cookie], resp: HttpResponse): HttpResponse =
    if cookies.isEmpty then resp
    else resp.mapHeaders(_.toBuilder.cookies(cookies*).build())

  private def addHeadersToHttpResponse(responseHeader: ResponseHeader, resp: HttpResponse): HttpResponse =
    if responseHeader.headers.isEmpty then resp
    else resp.mapHeaders(_.withMutations{builder =>
      responseHeader.headers.map{ header =>
        builder.set(header._1, header._2)
      }
    })

  private def addContentTypeToHttpResponse(contextTypeOpt: Option[MediaType], resp: HttpResponse): HttpResponse =
    contextTypeOpt match
      case Some(contentType) =>
        resp.mapHeaders(_.withMutations { builder =>
          builder.contentType(contentType)
        })
      case None => resp

  def toHttpResponse(req: Request): HttpResponse =
    given Request = req
    val httpResp = body match
      case httpResponse: HttpResponse => httpResponse
      case bytes: Array[Byte] => HttpResponse.of(HttpStatus.OK, req.contentType, bytes)
      case string: String => HttpResponse.of(string)

    val result:Try[HttpResponse] = for {
      respWithCookies <- Try(addCookiesToHttpResponse(getAllCookies, httpResp))
      respWithHeaders <- Try(addHeadersToHttpResponse(header, respWithCookies))
      respWithContentType <- Try(addContentTypeToHttpResponse(contentTypeOpt, respWithHeaders))
    } yield respWithContentType

    result.getOrElse(httpResp)

}
