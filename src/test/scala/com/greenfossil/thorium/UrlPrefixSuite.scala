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

import com.linecorp.armeria.server.annotation.Get

object Service1:
  @Get("/redirect1")
  def redirect1 = Action{request =>
    Redirect(foo)
  }

  @Get("/foo")
  def foo = Action{request =>
    Ok("Foo")
  }

object Service2:
  @Get("/bar")
  def bar = Action{request =>
    Ok("Bar")
  }

class UrlPrefixSuite extends munit.FunSuite {

  test("annotatedService with path prefix"){
    val server = Server()
      .addServices(Service1)
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb
          .annotatedService("/ext", Service2)
      })
      .start()
    assertEquals(Service2.bar.endpoint.url, "/bar")
    assertEquals(Service2.bar.endpoint.prefixedUrl(server.serviceConfigs), "/ext/bar")
    val routePatterns = server.serviceConfigs.map(_.route().patternString())
    assertEquals(routePatterns, Seq("/foo", "/redirect1", "/bar", "/ext/bar"))
  }

  test("serviceUnder with path prefix"){
    val server = Server()
      .addServices(Service1)
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb
          .serviceUnder("/ext", Service2.bar)
      })
      .start()
    assertEquals(Service2.bar.endpoint.url, "/bar")
    val routePatterns = server.serviceConfigs.map(_.route().patternString())
    assertEquals(routePatterns, Seq("/foo", "/redirect1", "/bar", "/ext/*"))
    assertEquals(Service2.bar.endpoint.prefixedUrl(server.serviceConfigs), "/ext/bar")
  }

}
