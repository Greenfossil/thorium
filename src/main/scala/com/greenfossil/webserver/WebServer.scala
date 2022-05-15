package com.greenfossil.webserver

import com.greenfossil.config.AppSettings
import com.linecorp.armeria.server.{HttpService, Server, ServerErrorHandler}
import org.slf4j.LoggerFactory

import scala.util.Try

object WebServer:
  lazy val defaultHttpPort = AppSettings.instance.config.getInt("http.port")
  def apply(): WebServer = WebServer(defaultHttpPort, null, Nil, Nil, None)
  def apply(port: Int): WebServer = WebServer(port, null, Nil, Nil, None)

case class WebServer(_port: Int,
                     server: Server,
                     routes: Seq[(String, HttpService)],
                     annotatedServices: Seq[Any] = Nil,
                     errorHandlerOpt: Option[ServerErrorHandler]) {

  private val logger = LoggerFactory.getLogger("webserver")

  export server.{start as _, toString as _ , *}
  
  def port: Int = server.activeLocalPort()

  def addService(endpoint: String, action: HttpService): WebServer =
    copy(routes = routes :+ (endpoint, action))

  def addServices(newRoutes: Seq[(String, HttpService)]): WebServer  =
    copy(routes = routes ++ newRoutes)

  def addAnnotatedService(annotatedService: Controller): WebServer =
    copy(annotatedServices = annotatedServices :+ annotatedService)

  def setErrorHandler(h: ServerErrorHandler): WebServer =
    copy(errorHandlerOpt = Some(h))

  import scala.jdk.CollectionConverters.*

  private def buildServer: Server =
    val sb = Server.builder()
    if _port > 0 then sb.http(_port)
    routes.foreach{route => sb.service(route._1, route._2)}
    annotatedServices.foreach{s => sb.annotatedService(s)}
    sb.annotatedServiceExtensions(List(ArmeriaConverters).asJava, List(ArmeriaConverters).asJava,
      Nil.asJava)
    errorHandlerOpt.foreach{ handler => sb.errorHandler(handler.orElse(ServerErrorHandler.ofDefault()))}
    sb.build

  private def buildSecureServer: Server =
    val sb = Server.builder()
    if _port > 0 then {
      sb.https(_port)
      sb.tlsSelfSigned()
    }
    routes.foreach{route => sb.service(route._1, route._2)}
    annotatedServices.foreach{s => sb.annotatedService(s)}
    sb.annotatedServiceExtensions(List(ArmeriaConverters).asJava, List(ArmeriaConverters).asJava,
      Nil.asJava)
    errorHandlerOpt.foreach{ handler => sb.errorHandler(handler.orElse(ServerErrorHandler.ofDefault()))}
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

  def startSecure(): WebServer =
    val newSecureServer = buildSecureServer
    Runtime.getRuntime.addShutdownHook(Thread(
      () => {
        newSecureServer.stop().join
        logger.info("Server stopped.")
      }
    ))
    logger.info(s"Starting Server.")
    newSecureServer.start().join()
    copy(server = newSecureServer)

}

//TODO - https://armeria.dev/docs/advanced-production-checklist
class ServerConfig(val maxNumConnections: Int, maxRequestLength: Int, requestTimeoutInSecs: Int)
