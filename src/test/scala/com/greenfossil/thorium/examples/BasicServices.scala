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

package com.greenfossil.thorium.examples

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.{Get, Param, Post}

import java.io.ByteArrayInputStream
import scala.language.implicitConversions

/*
 * Please ensure com.greenfossil.webserver.examples.main is started
 */
object BasicServices extends Controller {

  //curl http://localhost:8080/simple
  @Get("/simple")
  def simple = Action { request =>
    "HelloWorld!"
  }

  //curl http://localhost:8080/simple2?name=Homer
  @Get("/simple2")
  def simpleQueryString = Action { request =>
    s"HelloWorld! - ${request.uri} ${request.uri}"
  }

  //curl http://localhost:8080/hello
  @Get("/hello")
  def helloText = Action { request =>
    "HelloWorld!"
  }

  //curl http://localhost:8080/hello-json
  @Get("/hello-json")
  def helloJson = Action { request =>
    import com.greenfossil.commons.json.Json
    val json = Json.obj("greetings" -> "HelloWorld!")
    Ok(json)
  }

  @Get("/redirect") //curl -v -L http://localhost:8080/redirect
  def redirectText = Action { request =>
    Redirect(redirectText2)
  }

  @Get("/redirectText2")
  def redirectText2 = Action { request =>
    "You are at Text2!"
  }

  //This method should not be called, it is here to ensure compilation works for InputStream as direct return
  // curl -v http://localhost:8080/image -o image.png
  @Get("/image")
  def image = Action {request =>
    import com.linecorp.armeria.common.HttpHeaderNames.CACHE_CONTROL
    val is = ClassLoader.getSystemResourceAsStream("favicon.png")
    is.withHeaders(CACHE_CONTROL -> "no-store").as(MediaType.PNG)
  }

  //This method should not be called, it is here to ensure compilation works for InputStream as direct return
  // curl -v http://localhost:8080/bytes
  @Get("/bytes")
  def bytes = Action { request =>
    import com.linecorp.armeria.common.HttpHeaderNames.CACHE_CONTROL
    val bytes: Array[Byte] = "HelloWorld!".getBytes
    bytes.withHeaders(CACHE_CONTROL -> "no-store").as(MediaType.PLAIN_TEXT)
  }

  //This method should not be called, it is here to ensure compilation works for InputStream as direct return
  // curl -v http://localhost:8080/session
  @Get("/session")
  def session = Action { request =>
    import com.linecorp.armeria.common.HttpHeaderNames.CACHE_CONTROL
    val bytes: Array[Byte] = "HelloWorld!".getBytes
    bytes.withHeaders(CACHE_CONTROL -> "no-store").as(MediaType.PLAIN_TEXT).withSession("foo" -> "bar")
  }

}

