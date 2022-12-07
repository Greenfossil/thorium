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
  Redirect(location, HttpStatus.SEE_OTHER)

inline def Redirect[A](inline location: A, inline status: HttpStatus): Result =
  ${RedirectImpl( '{location}, '{status}) }

import scala.quoted.*
def RedirectImpl[A: Type](locExpr: Expr[A], statusExpr: Expr[HttpStatus])(using quotes: Quotes): Expr[Result] =
  locExpr match
    case '{$ea: EssentialAction} =>
      '{toResult($statusExpr, EndpointMcr($locExpr).url)}
    case '{$ep: Endpoint} =>
      '{toResult($statusExpr, $ep.url)}
    case '{$ref: AnyRef} =>
      '{toResult($statusExpr, EndpointMcr($ref).url)}

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