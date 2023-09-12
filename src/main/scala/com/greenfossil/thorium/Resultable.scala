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
import com.linecorp.armeria.common.{Cookie, MediaType}
import io.netty.util.AsciiString

import java.io.InputStream
import java.time.ZonedDateTime

trait Resultable[A]:
  extension (a: A)
    def withHeaders(headers: (String | AsciiString, String)*): Result

    def withDateHeaders(headers: (String, ZonedDateTime)*): Result

    def discardingHeader(name: String): Result

    def as(contentType: MediaType): Result

    def withCookies(cookies: Cookie*): Result

    def discardingCookies[B <: String | Cookie](cookies: B*)(using request: Request): Result

    def withSession(session: Session): Result

    def withSession(sessions: (String, String)*): Result

    def withNewSession: Result

    def flashing(flash: Flash): Result

    def flashing(values: (String, String)*): Result

    def session(using request: Request): Session

    def addingToSession(values: (String, String)*)(using request: Request): Result

    def removingFromSession(keys: String*)(using request: Request): Result


given StringResultable: Resultable[String] with
  extension (s: String)
    def withHeaders(headers: (String | AsciiString, String)*): Result =
      Result(s).withHeaders(headers *)

    def withDateHeaders(headers: (String, ZonedDateTime)*): Result =
      Result(s).withDateHeaders(headers *)

    def discardingHeader(name: String): Result =
      Result(s).discardingHeader(name)

    def as(contentType: MediaType): Result =
      Result(s).as(contentType)

    def withCookies(cookies: Cookie*): Result =
      Result(s).withCookies(cookies*)

    def discardingCookies[A <: String | Cookie](cookies: A*)(using request: Request): Result =
      Result(s).discardingCookies(cookies*)

    def withSession(session: Session): Result =
      Result(s).withSession(session)

    def withSession(sessions: (String, String)*): Result =
      Result(s).withSession(sessions*)

    def withNewSession: Result =
      Result(s).withSession()

    def flashing(flash: Flash): Result =
      Result(s).flashing(flash)

    def flashing(values: (String, String)*): Result =
      Result(s).flashing(values*)

    def session(using request: Request): Session =
      Result(s).session

    def addingToSession(values: (String, String)*)(using request: Request): Result =
      Result(s).addingToSession(values *)

    def removingFromSession(keys: String*)(using request: Request): Result =
      Result(s).removingFromSession(keys *)

    def jsonPrettyPrint: String =
      Json.prettyPrint(Json.parse(s))

given InputStreamResultable: Resultable[InputStream] with

  extension (is: InputStream)
    def withHeaders(headers: (String | AsciiString, String)*): Result =
      Result(is).withHeaders(headers*)

    def withDateHeaders(headers: (String, ZonedDateTime)*): Result =
      Result(is).withDateHeaders(headers *)

    def discardingHeader(name: String): Result =
      Result(is).discardingHeader(name)

    def as(contentType: MediaType): Result =
      Result(is).as(contentType)

    def withCookies(cookies: Cookie*): Result =
      Result(is).withCookies(cookies *)

    def discardingCookies[A <: String | Cookie](cookies: A*)(using request: Request): Result =
      Result(is).discardingCookies(cookies *)

    def withSession(session: Session): Result =
      Result(is).withSession(session)

    def withSession(sessions: (String, String)*): Result =
      Result(is).withSession(sessions *)

    def withNewSession: Result =
      Result(is).withSession()

    def flashing(flash: Flash): Result =
      Result(is).flashing(flash)

    def flashing(values: (String, String)*): Result =
      Result(is).flashing(values *)

    def session(using request: Request): Session =
      Result(is).session

    def addingToSession(values: (String, String)*)(using request: Request): Result =
      Result(is).addingToSession(values *)

    def removingFromSession(keys: String*)(using request: Request): Result =
      Result(is).removingFromSession(keys *)

given ArrayBytesResultable: Resultable[Array[Byte]] with

  extension (bytes: Array[Byte])
    def withHeaders(headers: (String | AsciiString, String)*): Result =
      Result(bytes).withHeaders(headers*)

    def withDateHeaders(headers: (String, ZonedDateTime)*): Result =
      Result(bytes).withDateHeaders(headers *)

    def discardingHeader(name: String): Result =
      Result(bytes).discardingHeader(name)

    def as(contentType: MediaType): Result =
      Result(bytes).as(contentType)

    def withCookies(cookies: Cookie*): Result =
      Result(bytes).withCookies(cookies *)

    def discardingCookies[A <: String | Cookie](cookies: A*)(using request: Request): Result =
      Result(bytes).discardingCookies(cookies *)

    def withSession(session: Session): Result =
      Result(bytes).withSession(session)

    def withSession(sessions: (String, String)*): Result =
      Result(bytes).withSession(sessions *)

    def withNewSession: Result =
      Result(bytes).withSession()

    def flashing(flash: Flash): Result =
      Result(bytes).flashing(flash)

    def flashing(values: (String, String)*): Result =
      Result(bytes).flashing(values *)

    def session(using request: Request): Session =
      Result(bytes).session

    def addingToSession(values: (String, String)*)(using request: Request): Result =
      Result(bytes).addingToSession(values *)

    def removingFromSession(keys: String*)(using request: Request): Result =
      Result(bytes).removingFromSession(keys *)