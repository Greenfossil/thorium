package com.greenfossil.webserver

import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpHeaders, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.annotation.{ExceptionHandlerFunction, RequestConverterFunction, ResponseConverterFunction}
import com.linecorp.armeria.server.{HttpService, Server, ServerErrorHandler, ServiceRequestContext}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.lang.reflect.ParameterizedType
import java.util
import scala.util.Try

object WebServer:
  /**
   * A random port will be created
   * @return
   */
  def apply(): WebServer = WebServer(0, null, Nil, Nil, None, Environment.simple(), HttpConfiguration())

  def apply(config: Config): WebServer =
    val environment = Environment.simple()
    WebServer(0, null, Nil, Nil, None, environment, HttpConfiguration.fromConfig(config, environment))

  /**
   *
   * @param port
   * @return
   */
  def apply(port: Int): WebServer = WebServer(port, null, Nil, Nil, None, Environment.simple(), HttpConfiguration())

case class WebServer(_port: Int,
                     server: Server,
                     routes: Seq[(String, HttpService)],
                     annotatedServices: Seq[Any] = Nil,
                     errorHandlerOpt: Option[ServerErrorHandler],
                     environment: Environment,
                     httpConfiguration: HttpConfiguration,
                     requestConverters: Seq[RequestConverterFunction] = Nil,
                     responseConverters: Seq[ResponseConverterFunction] = Nil,
                     exceptionHandlers: Seq[ExceptionHandlerFunction] = Nil) {

  private val logger = LoggerFactory.getLogger("webserver")

  def mode = environment.mode

  def isDev  = mode == Mode.Dev
  def isTest = mode == Mode.Test
  def isProd = mode == Mode.Prod

  export server.{start as _, toString as _ , *}

  lazy val defaultRequestConverter: RequestConverterFunction =
    (svcRequestContext: ServiceRequestContext,
     aggHttpRequest: AggregatedHttpRequest,
     expectedResultType: Class[_],
     expectedParameterizedResultType: ParameterizedType) =>
          //embed the env and http config
          svcRequestContext.setAttr(RequestAttrs.Env, environment)
          svcRequestContext.setAttr(RequestAttrs.HttpConfig, httpConfiguration)
          if expectedResultType == classOf[com.greenfossil.webserver.Request]
          then new com.greenfossil.webserver.Request(svcRequestContext, aggHttpRequest) {}
          else RequestConverterFunction.fallthrough()

  lazy val defaultResponseConverter: ResponseConverterFunction =
    (svcRequestContext: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders) =>
      //embed the env and http config
      svcRequestContext.setAttr(RequestAttrs.Env, environment)
      svcRequestContext.setAttr(RequestAttrs.HttpConfig, httpConfiguration)
      result match
        case action: EssentialAction => action.serve(svcRequestContext, null)
        case _ => ResponseConverterFunction.fallthrough()

  def port: Int = server.activeLocalPort()

  def addService(endpoint: String, action: HttpService): WebServer =
    copy(routes = routes :+ (endpoint, action))

  def addServices(newRoutes: Seq[(String, HttpService)]): WebServer  =
    copy(routes = routes ++ newRoutes)

  def addAnnotatedService(annotatedService: Controller): WebServer =
    copy(annotatedServices = annotatedServices :+ annotatedService)

  def setErrorHandler(h: ServerErrorHandler): WebServer =
    copy(errorHandlerOpt = Some(h))

  def addRequestConverters(newRequestConverters: RequestConverterFunction*): WebServer =
    copy(requestConverters = newRequestConverters ++ this.requestConverters)

  def addResponseConverters(newResponseConverters: ResponseConverterFunction*): WebServer =
    copy(responseConverters = newResponseConverters ++ this.responseConverters)

  def addExceptionHandlers(newExceptionHandlers: ExceptionHandlerFunction*): WebServer =
    copy(exceptionHandlers = newExceptionHandlers ++ this.exceptionHandlers)

  import scala.jdk.CollectionConverters.*

  lazy val allRequestConverters: util.List[RequestConverterFunction] =
    (defaultRequestConverter +: requestConverters).asJava

  lazy val allResponseConverters: util.List[ResponseConverterFunction] =
    (defaultResponseConverter +: responseConverters).asJava

  lazy val allExceptionHandlers: util.List[ExceptionHandlerFunction] =
    exceptionHandlers.asJava

  private def buildServer: Server =
    val sb = Server.builder()
    if _port > 0 then sb.http(_port)
    routes.foreach{route => sb.service(route._1, route._2)}
    annotatedServices.foreach{s => sb.annotatedService(s)}
    sb.annotatedServiceExtensions(allRequestConverters, allResponseConverters, allExceptionHandlers)
    errorHandlerOpt.foreach{ handler => sb.errorHandler(handler.orElse(ServerErrorHandler.ofDefault()))}
    sb.build

  private def buildSecureServer: Server =
    val sb = Server.builder()
    if _port > 0 then {
      sb.https(_port)
    }
    sb.tlsSelfSigned()
    routes.foreach{route => sb.service(route._1, route._2)}
    annotatedServices.foreach{s => sb.annotatedService(s)}
    sb.annotatedServiceExtensions(allRequestConverters, allResponseConverters, allExceptionHandlers)
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
