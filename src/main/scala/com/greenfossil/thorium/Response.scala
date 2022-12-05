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

import com.linecorp.armeria.common.{HttpResponse, HttpStatus}

/*
 * Response
 */

def Ok[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.OK, body)

def BadRequest[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.BAD_REQUEST, body)

def Redirect(url: String, queryString: Map[String, Seq[String]]): Result =
  Redirect(url, queryString, HttpStatus.SEE_OTHER)

def Redirect(url: String, queryString: Map[String, Seq[String]], status: HttpStatus): Result =
  val loc = s"${url}${ queryString.toList.map{case (k, v) => Endpoint.paramKeyValueUrlEncoded(k, v)}.mkString("?", "&", "")}"
  toResult(status,loc)

import scala.compiletime.{erasedValue, error}
inline def Redirect[A](inline location: A): Result =
  inline erasedValue[A] match
    case _: String =>
      toResult(HttpStatus.SEE_OTHER, location.asInstanceOf[String])
    case _: EssentialAction =>
      Redirect(EndpointMcr(location))
    case _: Endpoint =>
      Redirect(location.asInstanceOf[Endpoint].url)
    case x: AnyRef =>
      Redirect(x.endpoint)
    case _ =>
      error("Unsupported redirect location type")

inline def Redirect[A](inline location: A, status: HttpStatus): Result =
  inline erasedValue[A] match
    case _: String =>
      toResult(status, location.asInstanceOf[String])
    case _: EssentialAction =>
      Redirect(EndpointMcr(location), status)
    case _: Endpoint =>
      Redirect(location.asInstanceOf[Endpoint].url, status)
    case x: AnyRef =>
      Redirect(x.endpoint, status)
    case _ =>
      error("Unsupported redirect location type")

def NotFound[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.NOT_FOUND, body)

def Forbidden[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.FORBIDDEN, body)

def InternalServerError[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.INTERNAL_SERVER_ERROR, body)

def Unauthorized[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.UNAUTHORIZED, body)

private def toResult[C](status: HttpStatus, body: C)(using w: Writeable[C]): Result =
  if /*Redirect*/ status.code() >= 300 && status.code() <= 308
  then Result(HttpResponse.ofRedirect(status, body.asInstanceOf[String]))
  else
    val (mediaType, is) = w.content(body)
    Result(HttpResponse.of(status, mediaType, is.readAllBytes()))