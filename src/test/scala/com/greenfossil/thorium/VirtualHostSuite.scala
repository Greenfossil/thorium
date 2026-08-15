package com.greenfossil.thorium

import com.linecorp.armeria.server.ServerPort
import com.linecorp.armeria.common.SessionProtocol

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

object Host1Services extends AbstractTestServices("host1")

object Host2Services extends AbstractTestServices("host2")


class VirtualHostSuite extends munit.FunSuite:

  @volatile private var server: Server = uninitialized

  private def httpSend(url: String): String =
    HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build()
      .send(
        HttpRequest.newBuilder(URI.create(url)).build(),
        HttpResponse.BodyHandlers.ofString()
      ).body()

  override def beforeAll(): Unit =
    // Use ServerPort(0, HTTP) for ephemeral ports. The same ServerPort object
    // is passed to both .port() and .virtualHost() so Armeria can match them
    // after the OS assigns actual port numbers.
    val port1 = ServerPort(0, SessionProtocol.HTTP)
    val port2 = ServerPort(0, SessionProtocol.HTTP)
    server = Server(0)
      .serverBuilderSetup {
        _.port(port1)
         .port(port2)
          .virtualHost(port1)
          .annotatedService(Host1Services)
          .and
          .virtualHost(port2)
          .annotatedService(Host2Services)
      }
      .start()

  override def afterAll(): Unit =
    if server != null then
      server.stop()

  test("contextPath") {
    // Read the actual bound ports from the started server.
    // Server(0) creates a default port, plus the two virtual host ports.
    // We identify which port serves which host by probing.
    val activePorts = server.server.activePorts().asScala.toSeq.map(_._1.getPort)
    assert(activePorts.size >= 2, s"Expected at least 2 active ports, got ${activePorts.size}")

    server.printRoutes

    // Find which port serves host1 vs host2 by probing /echo/hello
    // host1 returns "host1: hello!", host2 returns "host2: hello!"
    def probe(port: Int): String = httpSend(s"http://localhost:$port/echo/hello")

    val host1Port = activePorts.find(p => probe(p).contains("host1"))
      .getOrElse(throw new AssertionError(s"No port served host1, ports: $activePorts"))
    val host2Port = activePorts.find(p => probe(p).contains("host2"))
      .getOrElse(throw new AssertionError(s"No port served host2, ports: $activePorts"))

    assertNoDiff(httpSend(s"http://localhost:$host1Port/echo/hello"), "host1: hello!")
    assertNoDiff(httpSend(s"http://localhost:$host1Port/hi"), "host1: Hello User")
    assertNoDiff(httpSend(s"http://localhost:$host2Port/echo/hello"), "host2: hello!")
    assertNoDiff(httpSend(s"http://localhost:$host2Port/hi"), "host2: Hello User")
  }