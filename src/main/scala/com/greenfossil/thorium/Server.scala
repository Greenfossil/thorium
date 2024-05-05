/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import com.greenfossil.commons.json.{JsObject, Json}
import com.greenfossil.thorium.decorators.*
import com.linecorp.armeria.common.*
import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.server.annotation.{ExceptionHandlerFunction, RequestConverterFunction, ResponseConverterFunction}
import com.linecorp.armeria.server.docs.DocService
import com.linecorp.armeria.server.logging.AccessLogWriter
import com.linecorp.armeria.server.{Server as AServer, *}
import com.typesafe.config.ConfigObject
import io.netty.util.AttributeKey
import org.slf4j.LoggerFactory

import java.lang.reflect.ParameterizedType
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import scala.language.implicitConversions

private [thorium] val serverLogger = LoggerFactory.getLogger("com.greenfossil.thorium.server")

object Server:
  /**
   * Port is read from app.http.port in application.conf
   * @return
   */
  def apply(): Server = Server(null, Nil, Nil, Nil, None, Configuration())

  def apply(classLoader: ClassLoader): Server =
    Server(null, Nil, Nil, Nil, None, Configuration.from(classLoader))

  def apply(port: Int): Server =
    Server(null, Nil, Nil, Nil, None, Configuration.usingPort(port))

  private def defaultRequestConverter(requestConverterAttrs: Tuple,
                                      defaultRequestConverterFnOpt: Option[ServiceRequestContext => Void]): RequestConverterFunction =
    (svcRequestContext: ServiceRequestContext,
     aggHttpRequest: AggregatedHttpRequest,
     expectedResultType: Class[?],
     expectedParameterizedResultType: ParameterizedType) =>
      requestConverterAttrs.toList.foreach {
        case (key: AttributeKey[Any]@unchecked, value) =>
          svcRequestContext.setAttr[Any](key, value)
      }
      defaultRequestConverterFnOpt.foreach(_.apply(svcRequestContext))
      if expectedResultType == classOf[com.greenfossil.thorium.Request]
      then
        val request = new com.greenfossil.thorium.Request(svcRequestContext, aggHttpRequest) {}
        //Create a request for use in ResponseConverter
        svcRequestContext.setAttr(RequestAttrs.Request, request)
        request
      else RequestConverterFunction.fallthrough()

  private def defaultResponseConverter(responseConverterAttrs: Tuple,
                                       defaultResponseConverterFnOpt: Option[ServiceRequestContext => Void]): ResponseConverterFunction =
    (svcRequestContext: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders) =>
      responseConverterAttrs.toList.foreach {
        case (key: AttributeKey[Any]@unchecked, value) =>
          svcRequestContext.setAttr[Any](key, value)
      }
      defaultResponseConverterFnOpt.foreach(_.apply(svcRequestContext))
      result match
        case action: EssentialAction =>
          action.serve(svcRequestContext, svcRequestContext.request())

        case actionResp: ActionResponse =>
          val futureResp = new CompletableFuture[HttpResponse]()
          svcRequestContext
            .request()
            .aggregate()
            .thenApply: aggregateRequest =>
              svcRequestContext.blockingTaskExecutor().execute(() => {
                val ctxCl = Thread.currentThread().getContextClassLoader
                actionLogger.trace(s"Async Cl:${ctxCl}")
                if ctxCl == null then {
                  val cl = this.getClass.getClassLoader
                  actionLogger.trace(s"Async setContextClassloader:${cl}")
                  Thread.currentThread().setContextClassLoader(cl)
                }
                val req = new com.greenfossil.thorium.Request(svcRequestContext, aggregateRequest) {}
                val httpResp = HttpResponseConverter.convertActionResponseToHttpResponse(req, actionResp)
                futureResp.complete(httpResp)
              })
          HttpResponse.of(futureResp)

        case _ =>
          ResponseConverterFunction.fallthrough()

case class Server(server: AServer,
                  httpServices: Seq[(String, HttpService)],
                  annotatedServices: Seq[AnyRef] = Nil,
                  routeFnList: Seq[ServiceBindingBuilder => Unit] = Nil,
                  errorHandlerOpt: Option[ServerErrorHandler],
                  configuration: Configuration,
                  requestConverters: Seq[RequestConverterFunction] = Nil,
                  responseConverters: Seq[ResponseConverterFunction] = Nil,
                  exceptionHandlers: Seq[ExceptionHandlerFunction] = Nil,
                  serverBuilderSetupFn: Option[ServerBuilder => Unit] = None,
                  defaultRequestConverterFnOpt: Option[ServiceRequestContext => Void] = None,
                  defaultResponseConverterFnOpt: Option[ServiceRequestContext => Void] = None,
                  requestConverterAttrs: Tuple = EmptyTuple,
                  responseConverterAttrs: Tuple = EmptyTuple,
                  threatGuardModuleOpt: Option[ThreatGuardModule] = None,
                  docServiceOpt: Option[(String, DocService)] = None
                 ):

  def mode: Mode = configuration.environment.mode

  def isDev: Boolean = mode == Mode.Dev
  def isTest: Boolean = mode == Mode.Test
  def isProd: Boolean = mode == Mode.Prod

  export server.{start as _, toString as _,  stop as _, serviceConfigs => _,  *}

  val defaultRequestConverter =
    Server.defaultRequestConverter(requestConverterAttrs, defaultRequestConverterFnOpt)

  val defaultResponseConverter =
    Server.defaultResponseConverter(responseConverterAttrs, defaultResponseConverterFnOpt)

  def port: Int = server.activeLocalPort()

  def addHttpService(endpoint: String, action: HttpService): Server =
    copy(httpServices = httpServices :+ (endpoint, action))

  def addHttpServices(newHttpServices: (String, HttpService)*): Server =
    copy(httpServices = httpServices ++ newHttpServices)

  def addServices(newServices: AnyRef*): Server  =
    copy(annotatedServices = annotatedServices ++ newServices)

  def addRoute(route: ServiceBindingBuilder => Unit): Server =
    copy(routeFnList = routeFnList :+ route)

  /**
   * Add CSRFGuard
   * @return
   */
  def addCSRFGuard(): Server =
    addThreatGuardModule(CSRFGuardModule())

  /**
   * Add CSRFGuard
   * @param allowWhiteListPredicate
   * @return
   */
  def addCSRFGuard(allowWhiteListPredicate: (String, ServiceRequestContext) => Boolean): Server =
    addThreatGuardModule(CSRFGuardModule(allowWhiteListPredicate))

  /**
   * Add CSRFGuard
   * @param isSameOriginPredicate
   * @return
   */
  def addCSRFGuard(isSameOriginPredicate: (String, String, ServiceRequestContext) => Boolean): Server =
    addThreatGuardModule(CSRFGuardModule(isSameOriginPredicate))

  /**
   * Add CSRFGuard
   * @param allowWhiteListPredicate
   * @param verifyMethodFn
   * @return
   */
  def addCSRFGuard(allowWhiteListPredicate: (String, ServiceRequestContext) => Boolean, verifyMethodFn: String => Boolean): Server =
    val guardModule = new CSRFGuardModule(allowWhiteListPredicate, (_, _, _) => true, verifyMethodFn)
    addThreatGuardModule(guardModule)

  /**
   * Add CSRFGuard
   * @param allowWhiteListPredicate
   * @param isSameOriginPredicate
   * @return
   */
  def addCSRFGuard(allowWhiteListPredicate: (String, ServiceRequestContext) => Boolean,
                   isSameOriginPredicate: (String, String, ServiceRequestContext) => Boolean): Server =
    addThreatGuardModule(CSRFGuardModule(allowWhiteListPredicate, isSameOriginPredicate))

  /**
   * Add CSRFGuard
   * @param allowWhiteListPredicate
   * @param isSameOriginPredicate
   * @param verifyMethodFn
   * @return
   */
  def addCSRFGuard(allowWhiteListPredicate: (String, ServiceRequestContext) => Boolean,
                   isSameOriginPredicate: (String, String, ServiceRequestContext) => Boolean,
                   verifyMethodFn: String => Boolean): Server =
    addThreatGuardModule(new CSRFGuardModule(allowWhiteListPredicate, isSameOriginPredicate, verifyMethodFn))


  /**
   * Add RecaptchaGuard
   * @return
   */
  def addRecaptchaGuard(): Server =
    addThreatGuardModule(RecaptchaGuardModule())

  /**
   * Add RecaptchaGuard
   * @param pathVerifyPredicate
   * @return
   */
  def addRecaptchaGuard(pathVerifyPredicate: (String, ServiceRequestContext) => Boolean): Server =
    addThreatGuardModule(RecaptchaGuardModule(pathVerifyPredicate))

  /**
   *
   * @param guardModule
   * @return
   */
  def addThreatGuardModule(guardModule: ThreatGuardModule): Server =
    if guardModule == null then this
    else this.copy(threatGuardModuleOpt = Option(guardModule))


  def setErrorHandler(h: ServerErrorHandler): Server =
    copy(errorHandlerOpt = Some(h))

  def addRequestConverters(newRequestConverters: RequestConverterFunction*): Server =
    copy(requestConverters = newRequestConverters ++ this.requestConverters)

  def addResponseConverters(newResponseConverters: ResponseConverterFunction*): Server =
    copy(responseConverters = newResponseConverters ++ this.responseConverters)

  def addExceptionHandlers(newExceptionHandlers: ExceptionHandlerFunction*): Server =
    copy(exceptionHandlers = newExceptionHandlers ++ this.exceptionHandlers)

  def addRequestConverterRequestAttribute[A](attrKeyPair: (AttributeKey[A], A)): Server =
    copy(requestConverterAttrs = attrKeyPair *: requestConverterAttrs)

  def addResponseConverterRequestAttribute[A](attrKeyPair: (AttributeKey[A], A)): Server =
    copy(responseConverterAttrs = attrKeyPair *: responseConverterAttrs)

  def addRequestAttribute[A](attrKeyPair: (AttributeKey[A], A)): Server =
    copy(requestConverterAttrs = attrKeyPair *: requestConverterAttrs, responseConverterAttrs = attrKeyPair *: responseConverterAttrs)

  import scala.jdk.CollectionConverters.*

  def serviceConfigs: Seq[ServiceConfig] = server.serviceConfigs().asScala.toList

  def serviceRoutes: Seq[Route] = serviceConfigs.map(_.route()).distinct

  lazy val allRequestConverters: java.util.List[RequestConverterFunction] =
    (Seq(defaultRequestConverter, FormUrlEncodedRequestConverterFunction) ++ requestConverters).asJava

  lazy val allResponseConverters: java.util.List[ResponseConverterFunction] =
    (defaultResponseConverter +: responseConverters).asJava

  lazy val allExceptionHandlers: java.util.List[ExceptionHandlerFunction] =
    exceptionHandlers.asJava

  private def buildServer(sessionProtocols: SessionProtocol*): AServer =
    buildServer(configuration.httpPort, SessionProtocol.HTTP +: sessionProtocols)

  private def buildSecureServer(sessionProtocols: SessionProtocol*): AServer =
    buildServer(configuration.httpPort, SessionProtocol.HTTPS +: sessionProtocols)

  private def buildServer(port: Int, sessionProtocols: Seq[SessionProtocol]): AServer =
    val sb = AServer.builder()
    //Setup Protocol and ensure at least one of it is either Https or Http
    sb.port(port, sessionProtocols*)

    sb.maxRequestLength(configuration.maxRequestLength)
    configuration.maxNumConnectionOpt.foreach(maxConn => sb.maxNumConnections(maxConn))
    sb.requestTimeout(configuration.requestTimeout)
    httpServices.foreach(sb.service.tupled)
    annotatedServices.foreach(sb.annotatedService)
    routeFnList.foreach(_.apply(sb.route()))
    sb.annotatedServiceExtensions(allRequestConverters, allResponseConverters, allExceptionHandlers)
    errorHandlerOpt.foreach{ handler => sb.errorHandler(handler.orElse(ServerErrorHandler.ofDefault()))}
    serverBuilderSetupFn.foreach(_.apply(sb))
    docServiceOpt.foreach((name, docsService) => sb.serviceUnder(name, docsService))
    //Setup request & response logging
    sb.accessLogWriter(accessLogWriter(_), true)

    /*
     * Setup Decorator, Request First Initializer
     */
    threatGuardModuleOpt.foreach{ guardModule =>
      val guardModuleDecorator = ThreatGuardModuleDecoratingFunction(guardModule)
      sb.routeDecorator().pathPrefix("/").build(guardModuleDecorator)
    }
    
    sb.routeDecorator().pathPrefix("/").build(FirstResponderDecoratingFunction(configuration))

    sb.build

  private def accessLogWriter(requestLog: RequestLog): Unit =
    val serviceRequestContext = requestLog.context().asInstanceOf[ServiceRequestContext]
    val proxiedAddresses = serviceRequestContext.proxiedAddresses()

    val requestObject = configuration.config.getObject("app.accessLogger.requestProperties")
    val responseObject = configuration.config.getObject("app.accessLogger.responseProperties")

    def configObjectToJson(configObject: ConfigObject): JsObject =
      import scala.jdk.CollectionConverters.*
      Json.toJson(
        configObject.asScala.keys.map { key =>
          key -> configObject.toConfig.getString(key)
        }.toMap
      )

    def defaultJson(): JsObject = Json.obj(
      "timestamp" -> LocalDateTime.now.toString,
      "requestId" -> requestLog.context().id.text(),
      "clientIP" -> serviceRequestContext.clientAddress().getHostAddress,
      "remoteIP" -> serviceRequestContext.remoteAddress().toString,
      "proxiedDestinationAddresses" -> proxiedAddresses.destinationAddresses().asScala.toSeq.mkString("[", ", ", "]")
    )

    val requestJson: JsObject = defaultJson() ++
      Json.obj("headers" -> requestLog.requestHeaders().toString) ++ configObjectToJson(requestObject)
    AccessLogWriter.custom(Json.obj("request" -> requestJson).toString).log(requestLog)

    val responseJson: JsObject = defaultJson() ++
      Json.obj("headers" -> requestLog.responseHeaders().toString) ++ configObjectToJson(responseObject)
    AccessLogWriter.custom(Json.obj("response" -> responseJson).toString).log(requestLog)

  def serverBuilderSetup(setupFn: ServerBuilder => Unit): Server =
    copy(serverBuilderSetupFn = Option(setupFn))

  def addDocService(): Server = addDocService("/docs", new DocService())

  def addDocService(name: String): Server = addDocService(name, new DocService())

  def addDocService(docService: DocService): Server = addDocService("/docs", docService)

  def addDocService(name: String, docService: DocService): Server = copy(docServiceOpt = Option((name, docService)))

  private val thoriumBanner  =
    """
      |████████ ██   ██  ██████  ██████  ██ ██    ██ ███    ███
      |   ██    ██   ██ ██    ██ ██   ██ ██ ██    ██ ████  ████
      |   ██    ███████ ██    ██ ██████  ██ ██    ██ ██ ████ ██
      |   ██    ██   ██ ██    ██ ██   ██ ██ ██    ██ ██  ██  ██
      |   ██    ██   ██  ██████  ██   ██ ██  ██████  ██      ██ by Greenfossil Pte Ltd
     """.stripMargin


  def banner =
    thoriumBanner +
    s"""
         |  version: ${ThoriumBuildInfo.version}
         |  java.version: ${System.getProperty("java.version")}
         |  java.home: ${System.getProperty("java.home")}
         |  runtime version: ${Runtime.version()}
         |  free memory : ${Runtime.getRuntime.freeMemory().humanize}
         |  total memory : ${Runtime.getRuntime.totalMemory().humanize}
         |  max memory : ${Runtime.getRuntime.maxMemory().humanize}
         |  user.home: ${System.getProperty("user.home")}
         |  mode: ${Configuration().mode}
         |  maxNumConnection: ${Configuration().maxNumConnectionOpt}
         |""".stripMargin

  def start(sessionProtocols: SessionProtocol*): Server =
    val newServer = buildServer(sessionProtocols*)
    doStartServer(newServer)

  def startSecure(sessionProtocols: SessionProtocol*): Server =
    val newSecureServer = buildSecureServer(sessionProtocols*)
    doStartServer(newSecureServer)

  private def doStartServer(server: AServer): Server =
    Runtime.getRuntime.addShutdownHook(Thread(
      () => {
        serverLogger.info("Stopping server...")
        server.stop().join
        serverLogger.info("Server stopped.")
      }
    ))
    println(banner)
    serverLogger.info(s"Starting Server...")

    server.start().join()
    serverLogger.info("Server started.")
    copy(server = server)

  def stop(): Unit =
    asyncStop().join()
    
  def asyncStop(): CompletableFuture[Void] =
    server.stop()  

  def printRoutes: Server =
    println(s"Service Routes declared: ${serviceRoutes.size}")
    serviceRoutes foreach { route =>
      println(s"${route.methods().asScala.mkString(",")} - $route")
    }
    this
