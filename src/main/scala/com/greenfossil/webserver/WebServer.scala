package com.greenfossil.webserver

import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpHeaders, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.*
import com.linecorp.armeria.server.annotation.{ExceptionHandlerFunction, RequestConverterFunction, ResponseConverterFunction}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.lang.reflect.ParameterizedType
import java.util
import scala.util.Try

object WebServer:
  /**
   * Port is read from app.http.port in application.conf
   * @return
   */
  def apply(): WebServer = WebServer(null, Nil, Nil, None, Configuration())

  def apply(classLoader: ClassLoader): WebServer =
    WebServer(null, Nil, Nil, None, Configuration.from(classLoader))
    
  def apply(port: Int): WebServer =
    WebServer(null, Nil, Nil, None, Configuration.usingPort(port))

case class WebServer(server: Server,
                     services: Seq[(String, HttpService)],
                     annotatedServices: Seq[Any] = Nil,
                     errorHandlerOpt: Option[ServerErrorHandler],
                     configuration: Configuration,
                     requestConverters: Seq[RequestConverterFunction] = Nil,
                     responseConverters: Seq[ResponseConverterFunction] = Nil,
                     exceptionHandlers: Seq[ExceptionHandlerFunction] = Nil) {

  private val logger = LoggerFactory.getLogger("webserver")

  def mode = configuration.environment.mode

  def isDev  = mode == Mode.Dev
  def isTest = mode == Mode.Test
  def isProd = mode == Mode.Prod

  export server.{start as _, toString as _ , serviceConfigs => _,  *}

  lazy val defaultRequestConverter: RequestConverterFunction =
    (svcRequestContext: ServiceRequestContext,
     aggHttpRequest: AggregatedHttpRequest,
     expectedResultType: Class[_],
     expectedParameterizedResultType: ParameterizedType) =>
          //embed the env and http config
          svcRequestContext.setAttr(RequestAttrs.Config, configuration)
          if expectedResultType == classOf[com.greenfossil.webserver.Request]
          then new com.greenfossil.webserver.Request(svcRequestContext, aggHttpRequest) {}
          else RequestConverterFunction.fallthrough()

  lazy val defaultResponseConverter: ResponseConverterFunction =
    (svcRequestContext: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders) =>
      //embed the env and http config
      svcRequestContext.setAttr(RequestAttrs.Config, configuration)
      result match
        case action: EssentialAction => action.serve(svcRequestContext, null)
        case _ => ResponseConverterFunction.fallthrough()

  def port: Int = server.activeLocalPort()

  def addService(endpoint: String, action: HttpService): WebServer =
    copy(services = services :+ (endpoint, action))

  def addServices(newServices: (Controller| Tuple2[String, HttpService]) *): WebServer  =
    val (controllers: Seq[Controller], newSvcs: Seq[(String, HttpService)]) = newServices.partition(s => s.isInstanceOf[Controller])
    copy(services = services ++ newSvcs, annotatedServices = annotatedServices ++ controllers)

  def setErrorHandler(h: ServerErrorHandler): WebServer =
    copy(errorHandlerOpt = Some(h))

  def addRequestConverters(newRequestConverters: RequestConverterFunction*): WebServer =
    copy(requestConverters = newRequestConverters ++ this.requestConverters)

  def addResponseConverters(newResponseConverters: ResponseConverterFunction*): WebServer =
    copy(responseConverters = newResponseConverters ++ this.responseConverters)

  def addExceptionHandlers(newExceptionHandlers: ExceptionHandlerFunction*): WebServer =
    copy(exceptionHandlers = newExceptionHandlers ++ this.exceptionHandlers)

  import scala.jdk.CollectionConverters.*

  def serviceConfigs: Seq[ServiceConfig] = server.serviceConfigs().asScala.toList

  def serviceRoutes: Seq[Route] = serviceConfigs.map(_.route()).distinct

  import scala.jdk.CollectionConverters.*

  lazy val allRequestConverters: util.List[RequestConverterFunction] =
    (defaultRequestConverter +: requestConverters).asJava

  lazy val allResponseConverters: util.List[ResponseConverterFunction] =
    (defaultResponseConverter +: responseConverters).asJava

  lazy val allExceptionHandlers: util.List[ExceptionHandlerFunction] =
    exceptionHandlers.asJava

  private def buildServer(builderExtFn: ServerBuilder => Unit): Server =
    buildServer(false, builderExtFn)

  private def buildSecureServer(builderExtFn: ServerBuilder => Unit): Server =
    buildServer(true, builderExtFn)

  private def buildServer(secure:Boolean, builderExtFn: ServerBuilder => Unit): Server =
    val sb = Server.builder()
    if configuration.httpPort > 0 then {
      if secure then
        sb.https(configuration.httpPort)
        sb.tlsSelfSigned()
      else sb.http(configuration.httpPort)
    }
    sb.maxRequestLength(configuration.maxRequestLength)
    configuration.maxNumConnectionOpt.foreach(maxConn => sb.maxNumConnections(maxConn))
    sb.requestTimeout(configuration.requestTimeout)
    services.foreach{ route =>
      sb.service(route._1, route._2.decorate((delegate, ctx, req) =>{
        //embed the env and http config
        ctx.setAttr(RequestAttrs.Config, configuration)
        delegate.serve(ctx,req)
      }))
    }
    annotatedServices.foreach{s => sb.annotatedService(s)}
    sb.annotatedServiceExtensions(allRequestConverters, allResponseConverters, allExceptionHandlers)
    errorHandlerOpt.foreach{ handler => sb.errorHandler(handler.orElse(ServerErrorHandler.ofDefault()))}
    builderExtFn(sb)
    sb.build

  def start(): WebServer = start(_ => ())

  def start(builderExtFn: ServerBuilder => Unit): WebServer =
    val newServer = buildServer(builderExtFn)
    Runtime.getRuntime.addShutdownHook(Thread(
      () => {
        newServer.stop().join
        logger.info("Server stopped.")
      }
    ))
    logger.info(s"Starting Server.")
    newServer.start().join()
    copy(server = newServer)

  def startSecure(): WebServer = startSecure(_ => ())

  def startSecure(builderExtFn: ServerBuilder => Unit): WebServer =
    val newSecureServer = buildSecureServer(builderExtFn)
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
