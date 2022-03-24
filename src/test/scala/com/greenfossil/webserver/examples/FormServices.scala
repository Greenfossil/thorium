package com.greenfossil.webserver.examples

import com.greenfossil.webserver.data.*
import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.Get

object FormServices extends Controller {

  @Get("/form")
  def tupleForm = Action { implicit request =>
    val form = Form.tuple(
      "name" -> text,
      "id" -> longNumber
    )
    val bindedForm = form.bindFromRequest()
    bindedForm.fold(
      error => BadRequest("Errors"),
      value => Ok(value.toString)
    )
  }

//  def classForm = Action { implicit request =>
//    case class Foo(name: String, id: Long)
//    val form = Form.asClass[Foo](
//      "name" -> text,
//      "id" -> longNumber
//    )
//    val bindedForm = form.bindFromRequest()
//    bindedForm.fold(
//      error => BadRequest("Errors"),
//      value => Ok("HelloWorld!")
//    )
//  }

}
