package com.greenfossil.webserver

import com.linecorp.armeria.server.{HttpService, Server}
import org.slf4j.LoggerFactory

import java.util.concurrent.CompletableFuture

object WebServer {
  def apply(): WebServer = WebServer(0, null, Nil)
  def apply(port: Int): WebServer = WebServer(port, null, Nil)
}

case class WebServer(_port: Int, server: Server, routes: Seq[(String, HttpService)]) {
  private val logger = LoggerFactory.getLogger("webserver")

  export server.{start as _, toString as _ , *}
  
  def port: Int = server.activeLocalPort()

  def addService(endpoint: String, action: HttpService): WebServer =
    copy(routes = routes :+ (endpoint, action))

  def addServices(newRoutes: Seq[(String, HttpService)]): WebServer  =
    copy(routes = routes ++ newRoutes)

  private def buildServer: Server =
    val sb = Server.builder()
    if _port > 0 then sb.http(_port)

    routes.foreach{route => sb.service(route._1, route._2)}
    sb.build

  def start(): WebServer =
    val newServer = buildServer
    Runtime.getRuntime.addShutdownHook(Thread(
      () => {
        newServer.stop().join
        logger.info("Server stopped.")
      }
    ))
    logger.info(s"Starting Server.")
    newServer.start().join()
    copy(server = newServer)

}

//TODO - https://armeria.dev/docs/advanced-production-checklist
class ServerConfig(val maxNumConnections: Int, maxRequestLength: Int, requestTimeoutInSecs: Int)




