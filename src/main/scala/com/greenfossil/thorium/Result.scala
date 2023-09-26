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

import com.greenfossil.commons.json.JsValue
import com.greenfossil.htmltags.Tag
import com.linecorp.armeria.common.{Cookie, HttpStatus, MediaType}
import io.netty.util.AsciiString

import java.time.ZonedDateTime

def Ok(body: SimpleResponse): Result =
  Result.of(HttpStatus.OK, body)

def Ok(jsValue: JsValue): Result =
  Result.of(HttpStatus.OK, jsValue.stringify, MediaType.JSON)

def Ok(tag: Tag): Result =
  Result.of(HttpStatus.OK, tag.render, MediaType.HTML_UTF_8)

def BadRequest(body: SimpleResponse): Result =
  Result.of(HttpStatus.BAD_REQUEST, body)

def BadRequest(jsValue: JsValue): Result =
  Result.of(HttpStatus.BAD_REQUEST, jsValue.stringify, MediaType.JSON)

def BadRequest(tag: Tag): Result =
  Result.of(HttpStatus.BAD_REQUEST, tag.render, MediaType.HTML_UTF_8)

def NotFound(body: SimpleResponse): Result =
  Result.of(HttpStatus.NOT_FOUND, body)

def NotFound(jsValue: JsValue): Result =
  Result.of(HttpStatus.NOT_FOUND, jsValue.stringify, MediaType.JSON)

def NotFound(tag: Tag): Result =
  Result.of(HttpStatus.NOT_FOUND, tag.render, MediaType.HTML_UTF_8)

def Forbidden(body: SimpleResponse): Result =
  Result.of(HttpStatus.FORBIDDEN, body)

def Forbidden(jsValue: JsValue): Result =
  Result.of(HttpStatus.FORBIDDEN, jsValue.stringify, MediaType.JSON)

def Forbidden(tag: Tag): Result =
  Result.of(HttpStatus.FORBIDDEN, tag.render, MediaType.HTML_UTF_8)

def InternalServerError(body: SimpleResponse): Result =
  Result.of(HttpStatus.INTERNAL_SERVER_ERROR, body)

def InternalServerError(jsValue: JsValue): Result =
  Result.of(HttpStatus.INTERNAL_SERVER_ERROR, jsValue.stringify, MediaType.JSON)

def InternalServerError(tag: Tag): Result =
  Result.of(HttpStatus.INTERNAL_SERVER_ERROR, tag.render, MediaType.HTML_UTF_8)

def Unauthorized(body: SimpleResponse): Result =
  Result.of(HttpStatus.UNAUTHORIZED, body)

def Unauthorized(jsValue: JsValue): Result =
  Result.of(HttpStatus.UNAUTHORIZED, jsValue.stringify, MediaType.JSON)

def Unauthorized(tag: Tag): Result =
  Result.of(HttpStatus.UNAUTHORIZED, tag.render, MediaType.HTML_UTF_8)

def Redirect(url: String, queryParamTup:(String, String), queryParamTups: (String, String)*): Result =
  val params = queryParamTup +: queryParamTups
  Redirect(url, params.map((name, value) => (name, Seq(value)) ).toMap)

def Redirect(url: String, queryString: Map[String, Seq[String]]): Result =
  Redirect(url, queryString, HttpStatus.SEE_OTHER)

def Redirect(url: String, queryString: Map[String, Seq[String]], status: HttpStatus): Result =
  val loc = s"${url}${ queryString.toList.map{case (k, v) => Endpoint.paramKeyValueUrlEncoded(k, v)}.mkString("?", "&", "")}"
  Result.ofRedirect(status,loc)

inline def Redirect[A](inline location: A): Result =
  Redirect(location, HttpStatus.SEE_OTHER)

inline def Redirect[A](inline location: A, inline status: HttpStatus): Result =
  ${RedirectImpl( '{location}, '{status}) }

import scala.quoted.*
def RedirectImpl[A: Type](locExpr: Expr[A], statusExpr: Expr[HttpStatus])(using quotes: Quotes): Expr[Result] =
  locExpr match
    case '{$ea: EssentialAction} =>
      '{Result.ofRedirect($statusExpr, EndpointMcr($locExpr).url)}
    case '{$ep: Endpoint} =>
      '{Result.ofRedirect($statusExpr, $ep.url)}
    case '{$ref: AnyRef} =>
      '{Result.ofRedirect($statusExpr, EndpointMcr($ref).url)}


object Result:

  def isRedirect(code: Int): Boolean = code >= 300 && code <= 308

  def apply(body: ActionResponse): Result =
    body match
      case bodyResult: Result => bodyResult
      case body: SimpleResponse =>
        new Result(status = HttpStatus.OK, body = body)

  def of(status: HttpStatus, simpleResponse: SimpleResponse): Result =
    Result(status = status, body = simpleResponse)

  def of(tag: Tag): Result =
    of(HttpStatus.OK, tag.render, MediaType.HTML_UTF_8)

  def of(jsValue: JsValue): Result =
    of(HttpStatus.OK, jsValue.stringify, MediaType.JSON)

  def of(status: HttpStatus, simpleResponse: SimpleResponse, contentType: MediaType): Result =
    Result(status = status, body = simpleResponse, contentTypeOpt = Option(contentType))

  def ofRedirect(status: HttpStatus, location: String): Result =
    if isRedirect(status.code()) then Result(status = status,  body = location)
    else throw IllegalArgumentException(s"Redirect status: ${status} not supported")

case class Result(header: ResponseHeader = ResponseHeader(Map.empty),
                  status: HttpStatus,
                  body: SimpleResponse,
                  newSessionOpt: Option[Session] = None,
                  newFlashOpt: Option[Flash] = None,
                  newCookies:Seq[Cookie] = Nil,
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

  def as(status: HttpStatus, contentType: MediaType): Result =
    copy(status = status, contentTypeOpt = Some(contentType))
  
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
   * @param sessions the session to set with this result
   * @return the new result
   */
  def withSession(sessions: (String, String)*): Result = 
    withSession(Session(sessions.toMap))

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
   * RedirectApplication.index()).flashing(flash + ("success" -> "Done!"))
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
     *
     * @param values - values are added to an existing new Flash, else a new Flash would be created with these values
     * @return
     */
  def addingToFlashing(values: (String, String)*): Result =
  flashing(Flash(newFlashOpt.map(_.data).getOrElse(Map.empty) ++ values.toMap))


  /**
   *
   * @param keys - keys are removed from current new Flash instance, else it is ignored
   * @return
   */
  def removingFromFlashing(keys: String*): Result =
  flashing(Flash(newFlashOpt.map(_.data -- keys).getOrElse(Map.empty)))

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
    withSession(Session(request.session.data ++ values.toMap))

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
    withSession(Session(request.session.data -- keys))

  override def toString = s"Result($header)"

