package com.greenfossil.webserver

import com.greenfossil.commons.json.Json

import java.io.File

class RequestSuite extends munit.FunSuite{
  import com.linecorp.armeria.client.*
  import com.linecorp.armeria.common.*

  var server: WebServer = null

  override def beforeAll(): Unit = {
    server = WebServer()
      .addService("/text", Action { req =>
        val method = req.method
        if req.asText == "Hello Armeria!" && method == HttpMethod.POST then Ok("Received Text")
        else BadRequest("Did not receive the right text")
      })
      .addService("/json", Action { req =>
        val method = req.method
        val json = req.asJson
        val msgOpt = (json \ "msg").asOpt[String]
        if msgOpt.contains("Hello Armeria!") && method == HttpMethod.POST then Ok("Received Text")
        else BadRequest("Did not receive the right text")
      })
      .addService("/form", Action { req =>
        val method = req.method
        val form = req.asFormUrlEncoded
        val msg = form.getOrElse("msg[]", Nil)
        if msg == Seq("Hello", "Armeria!") && method == HttpMethod.POST then Ok("Received Text")
        else BadRequest("Did not receive the right text")
      })
      .addService("/multipart-form", Action { req =>
        val method = req.method
        req.asMultipartFormData(mpForm => {
          val form = mpForm.asFormUrlEncoded
          val files = mpForm.files
          println(s"form = ${form}")
          println(s"files.size = ${files.size}")
          files foreach println
          if form.nonEmpty then Ok("Received Text")
          else BadRequest("Did not receive the right text")
        })
      })
      .addService("/cookie", Action { req =>
        val cookie1 = Cookie.ofSecure("cookie1", "one")
        val cookie2 = Cookie.ofSecure("cookie2", "two")
        Ok("Here is your cookie").withCookies(cookie1, cookie2)
      })
      .start()
  }

  override def afterAll(): Unit = {
    Thread.sleep(1000)
    server.stop()
  }

  import com.linecorp.armeria.scala.implicits.*
  test("Text") {
    val client = WebClient.of(s"http://localhost:${server.port}")

    val creq = HttpRequest.of(HttpMethod.POST, "/text", MediaType.PLAIN_TEXT, "Hello Armeria!")
    client.execute(creq).aggregate().thenAccept(
      aggregate =>
        assertNoDiff(aggregate.contentUtf8(), "Received Text")
    )
  }

  test("Json"){
    val client = WebClient.of(s"http://localhost:${server.port}")
    val creq = HttpRequest.of(HttpMethod.POST, "/json", MediaType.JSON, Json.obj("msg" -> "Hello Armeria!").toString)
    client.execute(creq).aggregate().thenAccept(aggregate =>
      assertNoDiff(aggregate.contentUtf8(), "Received Text")
    )
  }

  test("FormUrlEncoded"){
    val client = WebClient.of(s"http://localhost:${server.port}")
    val creq = HttpRequest.of(HttpMethod.POST, "/form", MediaType.FORM_DATA, "msg[]=Hello&msg[]=Armeria!")
    client.execute(creq).aggregate().thenAccept(aggregate =>
      assertNoDiff(aggregate.contentUtf8(), "Received Text")
    )
  }

  test("Multipart Form"){
//    val fileURI = getClass.getClassLoader.getResource("sample.png").toURI
//    val file = new File(fileURI)
    import com.linecorp.armeria.common.multipart.*
    val mp = Multipart.of(
      BodyPart.of(ContentDisposition.of("form-data", "name1"), "hello1"),
      BodyPart.of(ContentDisposition.of("form-data", "name1"), "hello2"),
      BodyPart.of(ContentDisposition.of("form-data", "name2", "hello.txt"), "hello1")
    )
    val client = WebClient.of(s"http://localhost:${server.port}")
    client.execute(mp.toHttpRequest("/multipart-form")).aggregate().thenAccept(aggregate =>
      assertNoDiff(aggregate.contentUtf8(), "Received Text")
    )
  }

  test("Cookie"){
    val creq = HttpRequest.of(HttpMethod.GET, "/cookie")
    val client = WebClient.of(s"http://localhost:${server.port}")
    client.execute(creq).aggregate().thenAccept(aggregate => {
      val cookies = aggregate.headers().cookies()
      assertEquals(cookies.size, 2)
      assertNoDiff(aggregate.contentUtf8(), "Here is your cookie")
      }
    )
  }

}
