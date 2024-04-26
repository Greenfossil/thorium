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

import java.net.{CookieManager, URI, http}
import scala.language.implicitConversions

class AddHttpServiceSuite extends munit.FunSuite{
  import com.linecorp.armeria.client.*
  import com.linecorp.armeria.common.*

  var server: Server = null

  override def beforeAll(): Unit = {
    server = Server(0)
      .addHttpService("/text", Action { req =>
        val method = req.method
        if req.asText == "Hello Armeria!" && method == HttpMethod.POST then Ok("Received Text")
        else BadRequest("Did not receive the right text")
      })
      .addHttpService("/json", Action { req =>
        val method = req.method
        val json = req.asJson
        val msgOpt = (json \ "msg").asOpt[String]
        if msgOpt.contains("Hello Armeria!") && method == HttpMethod.POST then Ok("Received Text")
        else BadRequest("Did not receive the right text")
      })
      .addHttpService("/form", Action { req =>
        val method = req.method
        val form = req.asFormUrlEncoded
        val msg = form.getOrElse("msg[]", Nil)
        if msg == Seq("Hello", "Armeria!") && method == HttpMethod.POST then Ok("Received Text")
        else BadRequest("Did not receive the right text")
      })
      .addHttpService("/multipart-form", Action { req =>
        req.asMultipartFormData(mpForm => {
          val form = mpForm.asFormUrlEncoded
          if form.nonEmpty then Ok("Received Text")
          else BadRequest("Did not receive the right text")
        })
      })
      .addHttpService("/cookie", Action { req =>
        val cookie1 = Cookie.ofSecure("cookie1", "one")
        val cookie2 = Cookie.ofSecure("cookie2", "two")
        Ok("Here are your cookies").withCookies(cookie1, cookie2)
      })
      .addHttpService("/json2", Action {req =>
        Json.obj(
          "msg" -> "HelloWorld!"
        )
      })
      .start()
  }

  override def afterAll(): Unit = {
    Thread.sleep(1000)
    server.stop()
  }

  test("Text") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/text"))
        .POST(http.HttpRequest.BodyPublishers.ofString("Hello Armeria!"))
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertNoDiff(resp.body(), "Received Text")
  }

  test("Json"){
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/json"))
        .POST(http.HttpRequest.BodyPublishers.ofString(Json.obj("msg" -> "Hello Armeria!").stringify))
        .header("Content-Type", MediaType.JSON.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertNoDiff(resp.body(), "Received Text")
  }

  test("FormUrlEncoded"){
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/form"))
        .POST(http.HttpRequest.BodyPublishers.ofString("msg[]=Hello&msg[]=Armeria!"))
        .header("Content-Type", MediaType.FORM_DATA.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertNoDiff(resp.body(), "Received Text")
  }

  test("Multipart Form") {
    //    val fileURI = getClass.getClassLoader.getResource("sample.png").toURI
    //    val file = new File(fileURI)
    import com.linecorp.armeria.common.multipart.*
    val mp = Multipart.of(
      BodyPart.of(ContentDisposition.of("form-data", "name1"), "hello1"),
      BodyPart.of(ContentDisposition.of("form-data", "name1"), "hello2"),
      BodyPart.of(ContentDisposition.of("form-data", "name2", "hello.txt"), "hello1")
    )
    val client = WebClient.of(s"http://localhost:${server.port}")
    val resp = client.execute(mp.toHttpRequest("/multipart-form")).aggregate().join()
    assertNoDiff(resp.contentUtf8(), "Received Text")
  }

  test("Armeria Cookie"){
    val resp = WebClient.of(s"http://localhost:${server.port}").get("/cookie").aggregate().join()
    val cookies = resp.headers().cookies()
    assertEquals(cookies.size, 2)
    assertNoDiff(resp.contentUtf8(), "Here are your cookies")
  }

  test("HttpClient Cookie"){
    val cm = CookieManager()
    val resp = http.HttpClient.newBuilder().cookieHandler(cm).build().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/cookie")).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(cm.getCookieStore.getCookies.size, 2)
    assertNoDiff(resp.body(), "Here are your cookies")
  }

  test("Json2") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/json2")).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), HttpStatus.OK.code())
    assertEquals(resp.headers().firstValue("content-type").get, MediaType.JSON.toString)
    assertNoDiff(resp.body(), """{"msg":"HelloWorld!"}""")
  }

}
