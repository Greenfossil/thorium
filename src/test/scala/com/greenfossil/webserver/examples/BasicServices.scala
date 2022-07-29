package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Post}

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

}

