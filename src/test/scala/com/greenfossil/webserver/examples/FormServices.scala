package com.greenfossil.webserver.examples

import com.greenfossil.data.mapping.*
import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Post}

object FormServices extends Controller {
  import com.greenfossil.data.mapping.Mapping.*

  @Post("/form") //curl -d "name=homer&id=8" -X POST  http://localhost:8080/form
  def tupleForm = Action { implicit request =>
    val form = tuple(
      "name" -> text,
      "id" -> longNumber
    )
    val boundForm = form.bindFromRequest()
    boundForm.fold(
      error => BadRequest("Errors"),
      value => value.toString
    )
  }

  @Post("/class") //curl -X POST -d name=homer -d id=8 http://localhost:8080/class
  def classForm = Action { implicit request =>
    case class Foo(name: String, id: Long)
    val form = mapping[Foo](
      "name" -> text,
      "id" -> longNumber
    )
    val boundForm = form.bindFromRequest()
    boundForm.fold(
      error => BadRequest("Errors"),
      value => s"HelloWorld $value"
    )
  }

}
