package com.greenfossil.webserver

import com.greenfossil.webserver.examples.FormServices
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.HttpStatus

class FormBindingSuite extends munit.FunSuite{
  var server: WebServer = null

  test("bind from request with empty parameter"){
    val client = WebClient.builder(s"http://localhost:${server.port}").followRedirects().build()
    val resp = client.get("/form")
    resp.aggregate().thenApply{ aggResp =>
      println("Response...")
      println(s"aggResp.status() = ${aggResp.status()}")
      println(s"aggResp.headers() = ${aggResp.headers()}")
      println(s"aggResp.contentUtf8() = ${aggResp.contentUtf8()}")
      assertEquals(aggResp.status(), HttpStatus.BAD_REQUEST) // FIXME binding of the longnumber can be null
    }.join()
  }

  override def beforeAll(): Unit = {
    server = WebServer(8080)
      .addAnnotatedService(FormServices)
      .start()
  }

  override def afterAll(): Unit = {
    server.server.stop()
  }
}
