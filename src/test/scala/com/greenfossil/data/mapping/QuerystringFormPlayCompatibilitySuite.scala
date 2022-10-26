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

package com.greenfossil.data.mapping

import com.greenfossil.commons.json.Json
import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.client.*
import com.linecorp.armeria.common.*

class QuerystringFormPlayCompatibilitySuite extends munit.FunSuite {

  import Mapping.*
  
  var server: Server = null

  private def simpleForm: Mapping[(String, String)] = tuple(
    "id" -> text,
    "name" -> text
  )

  override def beforeAll(): Unit = {
    server = Server()
      .addService("/form", Action { implicit req =>
        simpleForm.bindFromRequest().fold(
          ex => BadRequest(s"Errors in form, ${ex.errors.mkString(", ")}"),
          values => Ok(s"Form values: [${values._1}], [${values._2}], Query Params: [${req.queryParams}]")
        )
      })
      .start()
  }

  override def afterAll(): Unit = {
    Thread.sleep(1000)
    server.stop()
  }

  /*
   * curl -X POST -F 'id="1"' -F 'name="John"' http://localhost:9000/form\?age\=22 -o /dev/null
   */
  test("Form data with query string - Client"){
    val client = WebClient.of(s"http://localhost:${server.port}")
    val req = HttpRequest.of(HttpMethod.POST, "/form?age=22", MediaType.FORM_DATA, "id=1&name=John")
    client.execute(req).aggregate().thenAccept(aggregate =>
      println(s"values = ${aggregate.contentUtf8()}") //values = Form values: [1], [John], Query Params: [[age=22]]
    )
  }

}
