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
import com.linecorp.armeria.common.{HttpResponse, HttpStatus, MediaType}

import scala.util.Try

given Conversion[HttpResponse, Result] = Result(_)

given Conversion[JsValue, Result] with
  def apply(jsValue: JsValue): Result = Result.of(HttpStatus.OK, jsValue.stringify, MediaType.JSON)

given  Conversion[Tag, Result] with
  def apply(tag: Tag): Result = Result.of(HttpStatus.OK, tag.render, MediaType.HTML_UTF_8)

/**
 * Unwraps a `Try[A]` into a `Result`, where `A` is any type that can be
 * converted to a `Result` (String, JsValue, Tag, Result, etc.).
 *
 * On [[scala.util.Success]]: wraps the value in a `Result` via `Result.apply`
 * (which returns a `Result` as-is, or wraps a `String`/`JsValue`/`Tag` etc.
 * with `HttpStatus.OK`).
 * On [[scala.util.Failure]]: re-throws the exception so that Thorium's
 * `EssentialAction.serve` catch block produces an `HttpResponse.ofFailure`,
 * which routes to the registered [[ServerErrorHandler]]. This matches the
 * behavior downstream error handlers expect (branded error pages, logging,
 * etc.) rather than leaking exception messages directly to the client.
 *
 * Example:
 * {{{
 * @Get("/search")
 * def search: Action = Action { req =>
 *   Try(db.findUser(req.queryParam("q")))   // Try[String]
 * }
 * }}}
 */
given Conversion[Try[ActionResponse], Result] with
  def apply(t: Try[ActionResponse]): Result = t.fold(throw _, Result(_))

/**
 * Unwraps an `Option[A]` into a `Result`, where `A` is any type that can be
 * converted to a `Result` (String, JsValue, Tag, Result, etc.).
 *
 * On `Some(value)`: wraps the value in a `Result` via `Result.apply`.
 * On `None`: returns `NotFound` (404) — the conventional HTTP semantic for
 * "resource not found".
 *
 * Example:
 * {{{
 * @Get("/user/:id")
 * def user: Action = Action { req =>
 *   db.findUser(req.pathParam("id"))   // Option[String]
 * }
 * }}}
 */
given Conversion[Option[ActionResponse], Result] with
  def apply(opt: Option[ActionResponse]): Result = opt.map(Result(_)).getOrElse(NotFound(""))

/**
 * Unwraps an `Either[String, A]` into a `Result`, where `A` is any type that
 * can be converted to a `Result` (String, JsValue, Tag, Result, etc.).
 *
 * On `Right(value)`: wraps the value in a `Result` via `Result.apply`.
 * On `Left(errorMsg)`: returns `BadRequest` (400) with the error message —
 * the conventional HTTP semantic for "invalid request / validation failure".
 *
 * Example:
 * {{{
 * @Post("/validate")
 * def validate: Action = Action { req =>
 *   validateInput(req.asText)   // Either[String, String]
 * }
 * }}}
 */
given Conversion[Either[String, ActionResponse], Result] with
  def apply(e: Either[String, ActionResponse]): Result = e.fold(BadRequest(_), Result(_))