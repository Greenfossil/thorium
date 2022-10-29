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

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpHeaders, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.{Server as AServer, *}
import com.linecorp.armeria.server.annotation.{ExceptionHandlerFunction, RequestConverterFunction, ResponseConverterFunction}
import com.linecorp.armeria.server.docs.DocService
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.lang.reflect.ParameterizedType
import java.time.LocalDateTime
import scala.util.Using
import scala.language.implicitConversions

private[thorium] val serverLogger = LoggerFactory.getLogger("com.greenfossil.thorium.server")
private [thorium] val armeriaLogger = LoggerFactory.getLogger("com.linecorp.armeria.logging.access")

object Server:
  /**
   * Port is read from app.http.port in application.conf
   * @return
   */
  def apply(): Server = Server(null, Nil, Nil, None, Configuration())

  def apply(classLoader: ClassLoader): Server =
    Server(null, Nil, Nil, None, Configuration.from(classLoader))

  def apply(port: Int): Server =
    Server(null, Nil, Nil, None, Configuration.usingPort(port))

case class Server(server: AServer,
                  httpServices: Seq[(String, HttpService)],
                  annotatedServices: Seq[AnyRef] = Nil,
                  errorHandlerOpt: Option[ServerErrorHandler],
                  configuration: Configuration,
                  requestConverters: Seq[RequestConverterFunction] = Nil,
                  responseConverters: Seq[ResponseConverterFunction] = Nil,
                  exceptionHandlers: Seq[ExceptionHandlerFunction] = Nil,
                  serverBuilderSetupFn: Option[ServerBuilder => Unit] = None,
                  docServiceNameOpt: Option[String] = None):

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
          if expectedResultType == classOf[Request]
          then
            val request = new Request(svcRequestContext, aggHttpRequest) {}
            //Create a request for use in ResponseConverter
            svcRequestContext.setAttr(RequestAttrs.Request, request)
            request
          else RequestConverterFunction.fallthrough()

  lazy val defaultResponseConverter: ResponseConverterFunction =
    (svcRequestContext: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders) =>
      //embed the env and http config
      svcRequestContext.setAttr(RequestAttrs.Config, configuration)
      result match
        case action: EssentialAction =>
          action.serve(svcRequestContext, svcRequestContext.request())
        case actionResp: ActionResponse  =>
          val f = svcRequestContext
            .request()
            .aggregate()
            .thenApplyAsync { aggregateRequest =>
              val req = new Request(svcRequestContext, aggregateRequest) {}
              HttpResponseConverter.convertActionResponseToHttpResponse(req, actionResp)
            }
          HttpResponse.from(f)

        case _ =>
          ResponseConverterFunction.fallthrough()

  def port: Int = server.activeLocalPort()

  def addHttpService(endpoint: String, action: HttpService): Server =
    copy(httpServices = httpServices :+ (endpoint, action))

  def addHttpServices(newHttpServices: (String, HttpService)*): Server =
    copy(httpServices = httpServices ++ newHttpServices)

  def addServices(newServices: AnyRef*): Server  =
    copy(annotatedServices = annotatedServices ++ newServices)

  def setErrorHandler(h: ServerErrorHandler): Server =
    copy(errorHandlerOpt = Some(h))

  def addRequestConverters(newRequestConverters: RequestConverterFunction*): Server =
    copy(requestConverters = newRequestConverters ++ this.requestConverters)

  def addResponseConverters(newResponseConverters: ResponseConverterFunction*): Server =
    copy(responseConverters = newResponseConverters ++ this.responseConverters)

  def addExceptionHandlers(newExceptionHandlers: ExceptionHandlerFunction*): Server =
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

  private def buildServer(): AServer =
    buildServer(false)

  private def buildSecureServer(): AServer =
    buildServer(true)

  private def buildServer(secure:Boolean): AServer =
    val sb = AServer.builder()
    if configuration.httpPort > 0 then {
      if secure then
        sb.https(configuration.httpPort)
        sb.tlsSelfSigned()
      else sb.http(configuration.httpPort)
    }
    sb.maxRequestLength(configuration.maxRequestLength)
    configuration.maxNumConnectionOpt.foreach(maxConn => sb.maxNumConnections(maxConn))
    sb.requestTimeout(configuration.requestTimeout)
    httpServices.foreach{ route =>
      sb.service(route._1, route._2.decorate((delegate, ctx, req) =>{
        //embed the env and http config
        ctx.setAttr(RequestAttrs.Config, configuration)
        delegate.serve(ctx,req)
      }))
    }
    annotatedServices.foreach{s => sb.annotatedService(s)}
    sb.annotatedServiceExtensions(allRequestConverters, allResponseConverters, allExceptionHandlers)
    errorHandlerOpt.foreach{ handler => sb.errorHandler(handler.orElse(ServerErrorHandler.ofDefault()))}
    serverBuilderSetupFn.foreach(_.apply(sb))
    docServiceNameOpt.foreach(name => sb.serviceUnder(name, new DocService()))
    //Setup request logging
    sb.accessLogWriter(requestLog => {
      armeriaLogger.info(
        Json.obj(
          "timestamp" -> LocalDateTime.now.toString,
          "requestId" -> requestLog.context().id.text(),
          "remoteIP" -> requestLog.context().asInstanceOf[ServiceRequestContext].clientAddress().getHostAddress,
          "status" -> requestLog.responseHeaders().status().code(),
          "method" -> requestLog.context().method().toString,
          "path" -> requestLog.context().path(),
          "query" -> requestLog.context().query(),
          "scheme" -> requestLog.context().request().scheme(),
          "requestLength" -> requestLog.responseLength(),
          "headers" -> requestLog.context().request().headers().toString,
          "requestStartTimeMillis" -> requestLog.responseStartTimeMillis(),
        ).toString
      )
    }, true)
    sb.build

  def serverBuilderSetup(setupFn: ServerBuilder => Unit): Server =
    copy(serverBuilderSetupFn = Option(setupFn))

  def addDocService(): Server = addDocService("/docs")

  def addDocService(prefix: String): Server = copy(docServiceNameOpt = Option(prefix))

  def start(): Server =
    val newServer = buildServer()
    Runtime.getRuntime.addShutdownHook(Thread(
      () => {
        newServer.stop().join
        serverLogger.info("Server stopped.")
      }
    ))
    serverLogger.info(s"Starting Server.")
    newServer.start().join()
    copy(server = newServer)

  def startSecure(): Server =
    val newSecureServer = buildSecureServer()
    Runtime.getRuntime.addShutdownHook(Thread(
      () => {
        newSecureServer.stop().join
        serverLogger.info("Server stopped.")
      }
    ))
    serverLogger.info(s"Starting Server.")
    newSecureServer.start().join()
    copy(server = newSecureServer)

