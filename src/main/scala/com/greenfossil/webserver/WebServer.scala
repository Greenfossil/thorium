package com.greenfossil.webserver

import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpData, HttpHeaders, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.*
import com.linecorp.armeria.server.annotation.{ExceptionHandlerFunction, RequestConverterFunction, ResponseConverterFunction}
import com.linecorp.armeria.server.docs.DocService
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.lang.reflect.ParameterizedType
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
                     annotatedServices: Seq[Controller] = Nil,
                     errorHandlerOpt: Option[ServerErrorHandler],
                     configuration: Configuration,
                     requestConverters: Seq[RequestConverterFunction] = Nil,
                     responseConverters: Seq[ResponseConverterFunction] = Nil,
                     exceptionHandlers: Seq[ExceptionHandlerFunction] = Nil,
                     beforeStartInitOpt: Option[ServerBuilder => Unit] = None,
                     docServiceNameOpt: Option[String] = None) {

  private val logger = LoggerFactory.getLogger("webserver")

  def mode: Mode = configuration.environment.mode

  def isDev: Boolean = mode == Mode.Dev
  def isTest: Boolean = mode == Mode.Test
  def isProd: Boolean = mode == Mode.Prod

  export server.{start as _, toString as _ , serviceConfigs => _,  *}

  lazy val defaultRequestConverter: RequestConverterFunction =
    (svcRequestContext: ServiceRequestContext,
     aggHttpRequest: AggregatedHttpRequest,
     expectedResultType: Class[_],
     expectedParameterizedResultType: ParameterizedType) =>
          //embed the env and http config
          svcRequestContext.setAttr(RequestAttrs.Config, configuration)
          if expectedResultType == classOf[com.greenfossil.webserver.Request]
          then {
            val request = new com.greenfossil.webserver.Request(svcRequestContext, aggHttpRequest) {}
            //Create a request for use in ResponseConverter
            svcRequestContext.setAttr(RequestAttrs.Request, request)
            request
          }
          else RequestConverterFunction.fallthrough()

  lazy val defaultResponseConverter: ResponseConverterFunction =
    (svcRequestContext: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders) =>
      //embed the env and http config
      svcRequestContext.setAttr(RequestAttrs.Config, configuration)
      result match
        case action: EssentialAction =>
          action.serve(svcRequestContext, svcRequestContext.request())
        case s: String =>
          HttpResponse.of(s)
        case hr: HttpResponse =>
          hr
        case bytes: Array[Byte] =>
          HttpResponse.of(HttpData.wrap(bytes))
        case result: Result =>
          val req = svcRequestContext.attr(RequestAttrs.Request)
          if req != null then result.toHttpResponse(req)
          else {
            //Handle a when no RequestConverterFunction has been invoked
            val f = svcRequestContext.request().aggregate().thenApply { aggReq =>
              val request = new Request(svcRequestContext, aggReq){}
              result.toHttpResponse(request)
            }
            HttpResponse.from(f)
          }

        case _ =>
          ResponseConverterFunction.fallthrough()

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

  lazy val allRequestConverters: java.util.List[RequestConverterFunction] =
    (defaultRequestConverter +: requestConverters).asJava

  lazy val allResponseConverters: java.util.List[ResponseConverterFunction] =
    (defaultResponseConverter +: responseConverters).asJava

  lazy val allExceptionHandlers: java.util.List[ExceptionHandlerFunction] =
    exceptionHandlers.asJava

  private def buildServer(): Server =
    buildServer(false)

  private def buildSecureServer(): Server =
    buildServer(true)

  private def buildServer(secure:Boolean): Server =
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
    beforeStartInitOpt.foreach(_.apply(sb))
    docServiceNameOpt.foreach(name => sb.serviceUnder(name, new DocService()))
    sb.build

  def addBeforeStartInit(initFn: ServerBuilder => Unit): WebServer =
    copy(beforeStartInitOpt = Option(initFn))

  def addDocService(): WebServer = addDocService("/docs")

  def addDocService(prefix: String): WebServer = copy(docServiceNameOpt = Option(prefix))

  def start(): WebServer =
    val newServer = buildServer()
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
    val newSecureServer = buildSecureServer()
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

