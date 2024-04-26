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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.read.ListAppender
import com.greenfossil.commons.json.{JsObject, Json}
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.{Get, Param}
import org.slf4j.LoggerFactory

import java.net.{URI, http}
import java.time.Duration
import java.util.Optional
import scala.language.implicitConversions

private object LoggerService:
  @Get("/log-appender")
  def logAppender(@Param msg:String)(req: Request) =
    s"content:$msg"
end LoggerService


class AccessLoggerSuite extends munit.FunSuite:

  var server: Server = null

  //List appender for Logging events
  lazy val logger: Logger = LoggerFactory.getLogger("com.linecorp.armeria.logging.access").asInstanceOf[ch.qos.logback.classic.Logger]

  lazy val listAppender: ListAppender[ILoggingEvent] = new ListAppender()

  lazy val logs: java.util.List[ILoggingEvent] = listAppender.list

  override def beforeAll(): Unit =
    listAppender.start()
    logger.addAppender(listAppender)
    logger.setLevel(Level.INFO)
    server= Server(0).addServices(LoggerService).start()

  override def afterAll(): Unit =
    server.stop()
    listAppender.stop()


  test("Request Response Access Logging".flaky) {

    http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/log-appender?msg=HelloWorld!"))
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )

    Thread.sleep(2000)
    def findEvent(dir: String, method: String, path: String): Optional[ILoggingEvent] =
      logs.stream()
        .filter(x => x.getMessage.contains(dir) && x.getMessage.contains(method) && x.getMessage.contains(path))
        .findFirst()

    import util.control.Breaks.*
    var cnt = 0
    breakable{
      while(true) {
        println(s"cnt = ${cnt} log.size:${logs.size}")
        if  findEvent("request", "GET", "/log-appender").isPresent && findEvent("response", "GET", "/log-appender").isPresent || cnt > 5 then
          println(s"Breaking out of loop. log.size:${logs.size}")
          break()
        else
          cnt += 1
          println(s"Sleeping for 2 seconds.. cnt:${cnt}, log.size:${logs.size}")
          Thread.sleep(2000)
      }
    }
    val requestMsg = findEvent("request", "GET", "/log-appender").get.getMessage
    val responseMsg = findEvent("response", "GET", "/log-appender").get.getMessage
    println(s"requestMsg = ${requestMsg}")
    println(s"responseMsg = ${responseMsg}")

    val requestJson = (Json.parse(requestMsg) \ "request").as[JsObject]
    val responseJson = (Json.parse(responseMsg) \ "response").as[JsObject]

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
    println(s""" = ${(requestJson \ "path").as[String]}""")
    assert((requestJson \ "path").as[String].equalsIgnoreCase("/log-appender"))
    assert((requestJson \ "requestLength").as[Int].equals(0))

    //checks the response data
    assert((responseJson \ "timestamp").as[String].nonEmpty)
    assert((responseJson \ "requestId").as[String].nonEmpty)
    assert((responseJson \ "clientIP").as[String].nonEmpty)
    assert((responseJson \ "remoteIP").as[String].nonEmpty)
    assert((responseJson \ "proxiedDestinationAddresses").toString.nonEmpty)

    assert((responseJson \ "headers").as[String].nonEmpty)
    assert((responseJson \ "statusCode").as[Int].equals(200))
    assert((responseJson \ "method").as[String].equalsIgnoreCase("GET"))
    assert((responseJson \ "query").as[String].nonEmpty)
    assert((responseJson \ "scheme").as[String].nonEmpty)
    assert((responseJson \ "responseStartTimeMillis").as[String].nonEmpty)
    assert((responseJson \ "path").as[String].equalsIgnoreCase("/log-appender"))
    assert((responseJson \ "responseLength").as[Int].equals(19))

  }

