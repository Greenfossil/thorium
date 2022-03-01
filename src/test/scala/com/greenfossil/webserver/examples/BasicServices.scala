package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.common.HttpData

object BasicServices {

  def helloText = Action { request =>
    Ok("HelloWorld!")
  }

  def helloJson = Action { request =>
    import com.greenfossil.commons.json.Json
    val json = Json.obj("greetings" -> "HelloWorld!")
    Ok(json)
  }

  def redirectText = Action { request =>
    Redirect("/redirectText2")
  }

  def redirectText2 = Action { request =>
    Ok("You are at Text2!")
  }

  def multipartForm = Action {request =>
    val mp = request.asMultipartFormData
    val names  = mp.names()
    println(s"names = $names")
    val map = mp.asFormUrlEncoded
    println(s"map = ${map}")
    val files = mp.files
    println(s"files = ${files}")
    Ok("Received Multiopart")
  }

}

object RouteService {
  import com.linecorp.armeria.server.Route
  val routeText = Route.builder().path("/routeText").build()

}
