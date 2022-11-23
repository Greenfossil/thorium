package com.greenfossil.thorium.examples

import com.linecorp.armeria.common.{HttpHeaderNames, HttpRequest, HttpResponse, SessionProtocol}
import com.linecorp.armeria.server.{ClientAddressSource, ServiceRequestContext}

import java.net.InetSocketAddress

def svc(ctx: ServiceRequestContext, req: HttpRequest) = {
  println("_".repeat(20))
  println(s"request.headers = ${ctx.request.headers.size()}")
  ctx.request.headers.forEach(header => println(s"header = ${header}"))
  println(s"ctx = ${ctx}")
  //RemoteAddr
  val remoteAddr = ctx.remoteAddress().asInstanceOf[InetSocketAddress]
  println(s"remoteAddr = ${remoteAddr}")
  //ClientAddress
  val clientAddr = ctx.clientAddress()
  println(s"clientAddr = ${clientAddr}")
  println(s"clientAddr.getHostAddress = ${clientAddr.getHostAddress}")
  println(s"clientAddr.getCanonicalHostName = ${clientAddr.getCanonicalHostName}")
  HttpResponse.of("Howdy")
}

@main def clientAddrMain =
  com.linecorp.armeria.server.Server.builder()
    .port(8080, SessionProtocol.HTTP, SessionProtocol.PROXY)
    .clientAddressSources(
      ClientAddressSource.ofHeader("X-REAL-IP"),
      ClientAddressSource.ofHeader(HttpHeaderNames.FORWARDED),
      ClientAddressSource.ofHeader(HttpHeaderNames.X_FORWARDED_FOR),
      ClientAddressSource.ofProxyProtocol()
    )
    .service("/client-addr", svc)
    .build()
    .start()