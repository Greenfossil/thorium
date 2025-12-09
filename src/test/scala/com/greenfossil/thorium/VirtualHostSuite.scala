package com.greenfossil.thorium

import com.linecorp.armeria.common
import com.linecorp.armeria.common.SessionProtocol

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

object Host1Services extends AbstractTestServices("host1")

object Host2Services extends AbstractTestServices("host2")


class VirtualHostSuite extends munit.FunSuite:

  private def httpSend(url: String): String =
    HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build()
      .send(
        HttpRequest.newBuilder(URI.create(url)).build(),
        HttpResponse.BodyHandlers.ofString()
      ).body()

  test("contextPath".fail) {
    val server = Server(8080)
      .serverBuilderSetup {
        _.port(8081, SessionProtocol.HTTP)
         .port(8082, SessionProtocol.HTTP)
          .virtualHost(8081)
          .annotatedService(Host1Services)
          .and
          .virtualHost(8082)
          .annotatedService(Host2Services)
      }

    val started = server.start()
    println("Sever started")

    started.printRoutes

    assertNoDiff(httpSend(s"http://localhost:8081/echo/hello"), "host1: hello!")
    assertNoDiff(httpSend(s"http://localhost:8081/hi"), "host1: Hello User")
    assertNoDiff(httpSend(s"http://localhost:8082/echo/hello"), "host2: hello!")
    assertNoDiff(httpSend(s"http://localhost:8082/hi"), "host2: Hello User")

    val stopped = started.stop()
    println("Stopped.")
  }
