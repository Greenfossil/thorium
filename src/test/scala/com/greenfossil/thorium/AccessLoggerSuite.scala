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

import ch.qos.logback.classic.{Level, Logger}
import com.greenfossil.commons.json.{JsObject, Json}
import com.linecorp.armeria.server.annotation.Get
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender

import scala.language.implicitConversions

private object LoggerService:

  @Get("/foo")
  def foo(req: Request) = s"content:${req.asText}"


class AccessLoggerSuite extends munit.FunSuite:

  import com.linecorp.armeria.client.*
  import com.linecorp.armeria.common.*

  var server: Server = Server()
    .addServices(LoggerService)
    .start()

  //List appender for Logging events
  val logger: Logger = LoggerFactory.getLogger("com.linecorp.armeria.logging.access")
    .asInstanceOf[ch.qos.logback.classic.Logger]

  val listAppender: ListAppender[ILoggingEvent] = new ListAppender()

  val logs: java.util.List[ILoggingEvent] = listAppender.list

  var resp: AggregatedHttpResponse = null

  val requestContent = "HelloWorld!"

  override def beforeAll(): Unit =
    listAppender.start()
    logger.addAppender(listAppender)
    logger.setLevel(Level.INFO)

    val client = WebClient.of(s"http://localhost:${server.port}")
    val creq = HttpRequest.of(HttpMethod.GET, "/foo", MediaType.PLAIN_TEXT, requestContent)
    resp = client.execute(creq).aggregate().join()

    Thread.sleep(1000) // waits for armeria to finish its logging

  override def afterAll(): Unit = server.stop()


  test("Request Response Access Logging") {

    val requestJson = (Json.parse(logs.get(0).getMessage) \ "request").as[JsObject]
    val responseJson = (Json.parse(logs.get(1).getMessage) \ "response").as[JsObject]

    //checks the request data
    assert((requestJson \ "timestamp").as[String].nonEmpty)
    assert((requestJson \ "requestId").as[String].nonEmpty)
    assert((requestJson \ "clientIP").as[String].nonEmpty)
    assert((requestJson \ "remoteIP").as[String].nonEmpty)
    assert((requestJson \ "proxiedDestinationAddresses").toString.nonEmpty)

    assert((requestJson \ "headers").as[String].nonEmpty)
    assert((requestJson \ "method").as[String].equalsIgnoreCase("GET"))
    assert((requestJson \ "query").as[String].nonEmpty)
    assert((requestJson \ "scheme").as[String].nonEmpty)
    assert((requestJson \ "requestStartTimeMillis").toString.nonEmpty)
    assert((requestJson \ "path").as[String].equalsIgnoreCase("/foo"))
    assert((requestJson \ "requestLength").as[String].equalsIgnoreCase(requestContent.length.toString))

    //checks the response data
    assert((responseJson \ "timestamp").as[String].nonEmpty)
    assert((responseJson \ "requestId").as[String].nonEmpty)
    assert((responseJson \ "clientIP").as[String].nonEmpty)
    assert((responseJson \ "remoteIP").as[String].nonEmpty)
    assert((responseJson \ "proxiedDestinationAddresses").toString.nonEmpty)

    assert((responseJson \ "headers").as[String].nonEmpty)
    assert((responseJson \ "statusCode").as[String].equalsIgnoreCase(resp.status().codeAsText()))
    assert((responseJson \ "method").as[String].equalsIgnoreCase("GET"))
    assert((responseJson \ "query").as[String].nonEmpty)
    assert((responseJson \ "scheme").as[String].nonEmpty)
    assert((responseJson \ "responseStartTimeMillis").as[String].nonEmpty)
    assert((responseJson \ "path").as[String].equalsIgnoreCase("/foo"))
    assert((responseJson \ "responseLength").as[String].equalsIgnoreCase(resp.contentUtf8().length.toString))
  }

