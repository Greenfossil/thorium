package com.greenfossil.thorium

import com.linecorp.armeria.common

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

trait AbstractTestServices(hostname: String):
  import com.linecorp.armeria.server.annotation.{Get, Param}

  @Get("/echo/hello")
  def echo(using request: Request) = s"${hostname}: hello!"

  @Get("/hi")
  def redirect(using request: Request) =
    Redirect(sayHello("User")) //Redirect will not work as the prefix is missing.

  @Get("/sayHello/:name")
  def sayHello(@Param name: String): String =
    s"${hostname}: Hello $name"

object ContextPathTestServices extends AbstractTestServices("local")

class ContextPathSuite extends munit.FunSuite:

  private def httpSend(url: String): String =
    HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build()
      .send(
        HttpRequest.newBuilder(URI.create(url)).build(),
        HttpResponse.BodyHandlers.ofString()
      ).body()

  test("contextPath"){
    val server = Server()
      .serverBuilderSetup{_.contextPath("/v1")
        .service("/foo",  (ctx, req) => common.HttpResponse.of("v1 foo"))
        .service("/bar",  Action {req => s"v1 ${req.path}" })
        .annotatedService(ContextPathTestServices)
        .and
        .contextPath("/v2")
        .service("/foo", (ctx, req) => common.HttpResponse.of("v2 foo"))
      }

    val started = server.start()
    println("Sever started")

    started.printRoutes

    //Test context-path request
    assertNoDiff(httpSend(s"http://localhost:${started.port}/v1/foo"), "v1 foo")
    assertNoDiff(httpSend(s"http://localhost:${started.port}/v1/bar"), "v1 /v1/bar")
    assertNoDiff(httpSend(s"http://localhost:${started.port}/v1/echo/hello"), "local: hello!")
    assertNoDiff(httpSend(s"http://localhost:${started.port}/v1/hi"), """Status: 404
                                                                        |Description: Not Found""".stripMargin)
    assertNoDiff(httpSend(s"http://localhost:${started.port}/v2/foo"), "v2 foo")

    val stopped = started.stop()
    println("Stopped.")
  }
