package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.multipart.BodyPartBuilder

import java.io.File

class RequestSuite extends munit.FunSuite{
  import com.linecorp.armeria.client.*
  import com.linecorp.armeria.common.*

  val fixture = FunFixture[(WebServer, WebClient)](
    setup = { test =>
      val server = WebServer()
        .addService("/text", Service { req =>
          val method = req.method()
          if req.asText == "Hello Armeria!" && method == HttpMethod.POST then Ok("Received Text")
          else BadRequest("Did not receive the right text")
        })
        .addService("/json", Service { req =>
          val method = req.method()
          val json = req.asJson
          val msgOpt = (json \ "msg").asOpt[String]
          if msgOpt.contains("Hello Armeria!") && method == HttpMethod.POST then Ok("Received Text")
          else BadRequest("Did not receive the right text")
        })
        .addService("/form", Service { req =>
          val method = req.method()
          val form = req.asFormUrlEncoded
          val msg = form.getOrElse("msg[]", Nil)
          if msg == Seq("Hello", "Armeria!") && method == HttpMethod.POST then Ok("Received Text")
          else BadRequest("Did not receive the right text")
        })
        .addService("/multipart-form", Service { req =>
          val method = req.method()
          val mp = req.asMultipartFormData
          val form = mp.asFormUrlEncoded
          val files = mp.files
          if form.nonEmpty then Ok("Received Text")
          else BadRequest("Did not receive the right text")
        })
        .addService("/cookie", Service { req =>
          val cookie1 = Cookie.ofSecure("cookie1", "one")
          val cookie2 = Cookie.ofSecure("cookie2", "two")
          Ok("Here is your cookie").withCookies(cookie1, cookie2)
        })
        .start()

      val client = WebClient.of(s"http://localhost:${server.port}")
      (server, client)
    },
    teardown = { (server, client) =>
      server.stop()
    }
  )

  fixture.test("Text") { (server, client) =>
    val creq = HttpRequest.of(HttpMethod.POST, "/text", MediaType.PLAIN_TEXT, "Hello Armeria!")
    val cresp = client.execute(creq).aggregate().join()
    assertNoDiff(cresp.contentUtf8(), "Received Text")
  }

  fixture.test("Json"){(server, client) =>
    val creq = HttpRequest.of(HttpMethod.POST, "/json", MediaType.JSON, Json.obj("msg" -> "Hello Armeria!").toString)
    val cresp = client.execute(creq).aggregate().join()
    assertNoDiff(cresp.contentUtf8(), "Received Text")
  }

  fixture.test("FormUrlEncoded"){(server, client) =>
    val creq = HttpRequest.of(HttpMethod.POST, "/form", MediaType.FORM_DATA, "msg[]=Hello&msg[]=Armeria!")
    val cresp = client.execute(creq).aggregate().join()
    assertNoDiff(cresp.contentUtf8(), "Received Text")
  }

  fixture.test("Multipart Form"){(server, client) =>
//    val fileURI = getClass.getClassLoader.getResource("sample.png").toURI
//    val file = new File(fileURI)
    import com.linecorp.armeria.common.multipart.*
    val mp = Multipart.of(
      BodyPart.of(ContentDisposition.of("form-data", "name1"), "hello1"),
      BodyPart.of(ContentDisposition.of("form-data", "name1"), "hello2"),
      BodyPart.of(ContentDisposition.of("form-data", "name2", "hello.txt"), "hello1")
    )
    val cresp = client.execute(mp.toHttpRequest("/multipart-form")).aggregate().join()
    assertNoDiff(cresp.contentUtf8(), "Received Text")
  }

  fixture.test("Cookie"){(server, client) =>
    val creq = HttpRequest.of(HttpMethod.GET, "/cookie")
    val cresp:AggregatedHttpResponse = client.execute(creq).aggregate().join()
    val cookies = cresp.headers().cookies()
    assertEquals(cookies.size, 2)
    assertNoDiff(cresp.contentUtf8(), "Here is your cookie")
  }

}
