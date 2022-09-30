package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.{Get, Post, Param}

import java.io.ByteArrayInputStream
import scala.language.implicitConversions

/*
 * Please ensure com.greenfossil.webserver.examples.main is started
 */
object BasicServices extends Controller {

  //curl http://localhost:8080/simple
  @Get("/simple")
  def simple = Action { request =>
    "HelloWorld!"
  }

  //curl http://localhost:8080/simple2?name=Homer
  @Get("/simple2")
  def simpleQueryString = Action { request =>
    s"HelloWorld! - ${request.uri} ${request.uri}"
  }

  //curl http://localhost:8080/hello
  @Get("/hello")
  def helloText = Action { request =>
    "HelloWorld!"
  }

  //curl http://localhost:8080/hello-json
  @Get("/hello-json")
  def helloJson = Action { request =>
    import com.greenfossil.commons.json.Json
    val json = Json.obj("greetings" -> "HelloWorld!")
    Ok(json)
  }

  @Get("/redirect") //curl -v -L http://localhost:8080/redirect
  def redirectText = Action { request =>
    Redirect(redirectText2)
  }

  @Get("/redirectText2")
  def redirectText2 = Action { request =>
    "You are at Text2!"
  }

  //This method should not be called, it is here to ensure compilation works for InputStream as direct return
  @Get("/image")
  def image = Action {request =>
    import com.linecorp.armeria.common.HttpHeaderNames.CACHE_CONTROL
    val is: java.io.InputStream = new ByteArrayInputStream(null)
    is.withHeaders(CACHE_CONTROL -> "no-store").as(MediaType.PNG)
  }

  //This method should not be called, it is here to ensure compilation works for InputStream as direct return
  @Get("/bytes")
  def bytes = Action { request =>
    import com.linecorp.armeria.common.HttpHeaderNames.CACHE_CONTROL
    val bytes: Array[Byte] = Array.emptyByteArray
    bytes.withHeaders(CACHE_CONTROL -> "no-store").as(MediaType.PNG)
  }

}

