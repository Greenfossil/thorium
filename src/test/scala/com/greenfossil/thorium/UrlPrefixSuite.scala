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

import com.linecorp.armeria.server.annotation.{Get, Param}

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

  @Get("/site/favicon")
  def favicon = Action{request =>
    Ok("favicon")
  }

  @Get("/param/:str")
  def param(@Param str: String) = Action{request =>
    Ok(str)
  }

  @Get("/query")
  def query(@Param str: String) = Action{request =>
    Ok(str)
  }

  @Get("regex:^/(?<num>\\d+)$")
  def number(@Param num: Int) = Action{ request =>
    Ok(num.toString)
  }

  @Get("prefix:/test")
  def test = Action{request =>
    request.requestContext.mappedPath()
    Ok(request.uri.toString)
  }

class UrlPrefixSuite extends munit.FunSuite {
  test("route patterns"){
    val server = Server()
      .addServices(Service1)
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb
          .annotatedService("/ext", Service2)
          .serviceUnder("/ext2", Service1.foo)
      })
      .start()

    server.server.stop()
    val routePatterns = server.serviceConfigs.map(_.route().patternString())
    assertEquals(routePatterns, List(
      // direct routes
      "/foo", "/redirect1", "/bar",
      "^/(?<num>\\d+)$", "/query", "/test/*", "/param/:str",
      //annotatedService routes
      "/site/favicon",
      "/ext/bar",
      "/ext/^/(?<num>\\d+)$",
      "/ext/query",
      "/ext/test/*",
      "/ext/param/:str",
      "/ext/site/favicon",
      // serviceUnder routes
      "/ext2/*"
    ))
  }

  test("annotatedService with path prefix"){
    val server = Server()
      .addServices(Service1)
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb
          .annotatedService("/ext", Service2)
      })
      .start()
    server.server.stop()

    assertEquals(Service2.bar.endpoint.url, "/bar")
    assertEquals(Service2.bar.endpoint.prefixedUrl(server.serviceConfigs), "/ext/bar")

    assertEquals(Service2.param("hello").endpoint.url, "/param/hello")
    assertEquals(Service2.param("hello").endpoint.prefixedUrl(server.serviceConfigs), "/ext/param/hello")

    assertEquals(Service2.query("hello").endpoint.url, "/query?str=hello")
    assertEquals(Service2.query("hello").endpoint.prefixedUrl(server.serviceConfigs), "/ext/query?str=hello")

    assertEquals(Service2.number(1).endpoint.url, "/1")
    assertEquals(Service2.number(1).endpoint.prefixedUrl(server.serviceConfigs), "/ext/1")

    assertEquals(Service2.test.endpoint.url, "/test")
    assertEquals(Service2.test.endpoint.prefixedUrl(server.serviceConfigs), "/ext/test")

    //Test non macro created Endpoint
    assertEquals(Endpoint("/test/path").url, "/test/path")
    assertEquals(Endpoint("/test/path", "prefix:/test").prefixedUrl(server.serviceConfigs), "/ext/test/path")
    assertEquals(Endpoint("/test/path", Service2.test.endpoint.pathPatternOpt.get).prefixedUrl(server.serviceConfigs), "/ext/test/path")
  }

  test("prefixUrl2 - with prefix routes"){
    val server = Server()
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb.annotatedService("/ext", Service2)
        sb.annotatedService("/ext2", Service2)
      })
      .start()
    server.server.stop()
    //Ensures the Endpoint path has prepended with prefix '/ext' depending request path
    //1. without prefix
    assertEquals(Endpoint("/test/foo", "prefix:/test").prefixedUrl2("/test/bar", server.serviceConfigs), "/test/foo")
    //2. with prefix /ext
    assertEquals(Endpoint("/test/foo", "prefix:/test").prefixedUrl2("/ext/test/bar", server.serviceConfigs), "/ext/test/foo")
    //3. with prefix /ext2
    assertEquals(Endpoint("/test/foo", "prefix:/test").prefixedUrl2("/ext2/test/bar", server.serviceConfigs), "/ext2/test/foo")
  }

  test("prefixUrl2 - favicon route") {
    val server = Server()
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb.annotatedService("/ext", Service2)
        sb.annotatedService("/ext2", Service2)
      })
      .start()
    server.server.stop()
    assertEquals(Service2.favicon.endpoint.prefixedUrl2("/bar", server.serviceConfigs), "/site/favicon")
    assertEquals(Service2.favicon.endpoint.prefixedUrl2("/ext/bar", server.serviceConfigs), "/ext/site/favicon")
  }

  test("prefixUrl2 - with parameters") {
    val server = Server()
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb.annotatedService("/ext", Service2)
        sb.annotatedService("/ext2", Service2)
      })
      .start()
    server.server.stop()
    
    assertEquals(Service2.param("hello").endpoint.prefixedUrl2("/bar", server.serviceConfigs), "/param/hello")
    assertEquals(Service2.param("hello").endpoint.prefixedUrl2("/ext/bar", server.serviceConfigs), "/ext/param/hello")
    assertEquals(Service2.param("hello").endpoint.prefixedUrl2("/ext2/bar", server.serviceConfigs), "/ext2/param/hello")

    assertEquals(Service2.query("hello").endpoint.prefixedUrl2("/bar", server.serviceConfigs), "/query?str=hello")
    assertEquals(Service2.query("hello").endpoint.prefixedUrl2("/ext/bar", server.serviceConfigs), "/ext/query?str=hello")
    assertEquals(Service2.query("hello").endpoint.prefixedUrl2("/ext2/bar", server.serviceConfigs), "/ext2/query?str=hello")

    assertEquals(Service2.number(1).endpoint.prefixedUrl2("/bar", server.serviceConfigs), "/1")
    assertEquals(Service2.number(1).endpoint.prefixedUrl2("/ext/bar", server.serviceConfigs), "/ext/1")
    assertEquals(Service2.number(1).endpoint.prefixedUrl2("/ext2/bar", server.serviceConfigs), "/ext2/1")
  }


  test("serviceUnder with path prefix".ignore){
    val server = Server()
      .addServices(Service1)
      .addServices(Service2)
      .serverBuilderSetup(sb => {
        sb
          .serviceUnder("/ext", Service2.bar)
      })
      .start()
    server.server.stop()

    assertEquals(Service2.bar.endpoint.url, "/bar")
    assertEquals(Service2.bar.endpoint.prefixedUrl(server.serviceConfigs), "/ext/bar")
  }

}
