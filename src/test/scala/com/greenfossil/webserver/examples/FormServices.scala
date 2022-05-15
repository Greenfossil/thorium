package com.greenfossil.webserver.examples

import com.greenfossil.data.mapping.*
import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Post}

object FormServices extends Controller {
  import com.greenfossil.data.mapping.Mapping.*

  @Post("/form") //curl -v -d "name=homer&id=8" -X POST  http://localhost:8080/form
  def tupleForm = Action { implicit request =>
    val form = tuple(
      "name" -> text,
      "id" -> longNumber
    )
    val boundForm = form.bindFromRequest()
    boundForm.fold(
      error => BadRequest("Errors"),
      value => Ok(value.toString)
    )
  }

  @Post("/class")
  def classForm = Action { implicit request =>
    case class Foo(name: String, id: Long)
    val form = mapping[Foo](
      "name" -> text,
      "id" -> longNumber
    )
    val boundForm = form.bindFromRequest()
    boundForm.fold(
      error => BadRequest("Errors"),
      value => Ok(s"HelloWorld $value")
    )
  }

}
