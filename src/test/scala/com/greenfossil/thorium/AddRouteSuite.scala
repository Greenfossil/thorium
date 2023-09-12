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
import com.linecorp.armeria.common.MediaType

import scala.language.implicitConversions

object AddRouteSuite:

  var server: Server = null

  //  override def beforeAll(): Unit =
  @main
  def test =
    server = Server(8080)
      .addRoute: route =>
        route.get("/route/greetings/:name").build:
          Action { req =>
            val name = req.pathParam("name")
            s"<h1>Greetings ${name}!</h1>".as(MediaType.HTML_UTF_8)
          }
      .addRoute: route =>
        route.post("/route/howdy").consumes(MediaType.FORM_DATA).build:
          Action { req =>
            val form: FormUrlEndcoded = req.asFormUrlEncoded
            s"Howdy ${form.getFirst("name")}"
          }
      .addRoute: route =>
        route.get("/route/greetings-tag/:name").build:
          Action { req =>
            import com.greenfossil.htmltags.*
            h2(s"Welcome! ${req.pathParam("name")}")
          }
      .addRoute: route =>
        route.get("/route/howdy-tag/:name").build:
          Action { req =>
            import com.greenfossil.htmltags.*
            h2(s"Howdy! ${req.pathParam("name")}")
              .flashing("test" -> "test")
          }
      .addRoute: route =>
        route.get("/route/greetings-json/:name").build:
          Action{req =>
            Json.obj{
              "Welcome" -> req.pathParam("name")
            }
          }
      .addRoute: route =>
        route.get("/route/howdy-json/:name").build:
          Action { req =>
            Json.obj {
              "Howdy" -> req.pathParam("name")
            }.flashing("test" -> "test")
          }
      .start()


