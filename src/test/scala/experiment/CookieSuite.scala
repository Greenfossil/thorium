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

package experiment

import com.linecorp.armeria.common.{Cookie, HttpResponse, ResponseHeaders}

class CookieSuite extends munit.FunSuite {

  test("cookie"){
    val cookie = Cookie.ofSecure("name", "value")
    val resp = HttpResponse.of("response")
    println(s"resp = ${resp.peekHeaders(headers => {
      val c = headers.cookies()
      c.toString
    } )}")
    val cookieResp = resp.mapHeaders(headers => headers.toBuilder.cookie(cookie).build() )
    println(s"cookieResp = ${cookieResp.peekHeaders(headers => {
      val c = headers.cookies()
      c.toString
    })}")
  }

}
