package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Post}

object BasicServices extends Controller {

  @Get("/simple")
  def simple = Action { request =>
    "HelloWorld!"
  }
  
  @Get("/hello")
  def helloText = Action { request =>
    Ok("HelloWorld!")
  }

  @Get("/hello-json")
  def helloJson = Action { request =>
    import com.greenfossil.commons.json.Json
    val json = Json.obj("greetings" -> "HelloWorld!")
    Ok(json)
  }

  @Get("/redirect") //curl -v -L http://localhost:8080/redirect
  def redirectText = Action { request =>
//    Redirect(redirectText2)
    Redirect("/redirectText2")
  }

  @Get("/redirectText2")
  def redirectText2 = Action { request =>
    Ok("You are at Text2!")
  }

  //curl -v -d "user=user1&pass=abcd" -X POST  http://localhost:8080/form 
  @Post("/form")
  def form = Action { request =>
    val f = request.asFormUrlEncoded
    println(s"form = ${f}")
    Ok(s"Form f ${f}")
  }

  //https://stackoverflow.com/questions/19116016/what-is-the-right-way-to-post-multipart-form-data-using-curl
  //https://everything.curl.dev/http/multipart
  //curl -v -F person=anonymous -F secret=file.txt http://localhost:8080/multipart
  @Post("/multipart")
  def multipartForm = Action {request =>
    request
      .asMultipartFormData(form =>
        val names  = form.names()
        println(s"names = $names")
        val map = form.asFormUrlEncoded
        println(s"map = ${map}")
        val files = form.files
        println(s"files = ${files}")
        Ok("Received Multiopart")
    )
  }

}

