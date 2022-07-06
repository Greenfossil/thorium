package com.greenfossil.webserver.examples

import com.greenfossil.data.mapping.*
import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param, Post}

object FormServices extends Controller {
  import com.greenfossil.data.mapping.Mapping.*

  //curl -v -d "user=user1&pass=abcd" -X POST  http://localhost:8080/form
  @Post("/form")
  def form = Action { request =>
    val f = request.asFormUrlEncoded
    println(s"form = ${f}")
    s"Form f ${f}"
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
          Ok("Received Multipart form")
      )
  }

  @Post("/form-mapping") //curl -d "name=homer&id=8" -X POST  http://localhost:8080/form-mapping
  def tupleForm = Action { implicit request =>
    val form = tuple(
      "name" -> text,
      "id" -> longNumber
    )
    val boundForm = form.bindFromRequest()
    boundForm.fold(
      error => BadRequest("Errors " + error.errors),
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

  @Post("/post-query") //curl -X POST  http://localhost:8080/post-query\?name\=homer\&postalCode\=12345
  def postQuery(@Param name: String, @Param postalCode: Int) = Action { implicit request =>
    Ok(s"Hello !${name}, postalCode:${postalCode}")
  }

  @Post("/post-patharg-query/:arg") //curl -X POST  http://localhost:8080/post-patharg-query/abc\?name\=homer\&postalCode\=12345
  def postPathArgQuery(@Param arg: String, @Param name: String, @Param postalCode: Int) = Action { implicit request =>
    Ok(s"Hello !${name}, postalCode:${postalCode} arg:${arg}")
  }

  @Post("/form-patharg/:arg") //curl -d "name=homer&id=8" -X POST  http://localhost:8080/form-patharg/abc
  def formPathArg(@Param arg: String) = Action { implicit request =>
    val encodedForm = request.asFormUrlEncoded
    Ok(s"arg:${arg}, form:${encodedForm}")
  }

  @Post("/form-patharg-query") //curl -d "name=homer" -X POST  http://localhost:8080/form-patharg-query\?postalCode\=12345
  def formPathArgQuery(@Param postalCode: Int) = Action { implicit request =>
    Ok(s"postalCode:${postalCode}")
  }

}
