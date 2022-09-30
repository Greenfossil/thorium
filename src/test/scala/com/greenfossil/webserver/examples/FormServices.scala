package com.greenfossil.webserver.examples

import com.greenfossil.data.mapping.*
import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param, Path, Post}

/*
 * Please ensure com.greenfossil.webserver.examples.main is started
 */
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

  //https://stackoverflow.com/questions/19116016/what-is-the-right-way-to-post-multipart-form-data-using-curl
  //https://everything.curl.dev/http/multipart
  //curl -v -F person=anonymous -F secret=@file.txt http://localhost:8080/multipart2
  @Post("/multipart2")
  def multipartForm2 = Action.multipart { implicit request =>
    val names = request.names()
    println(s"names = $names")
    val map = request.asFormUrlEncoded
    println(s"map = ${map}")
    val files = request.files
    println(s"files = ${files}")
    
    val form = Mapping("person", text).bindFromRequest()
    Ok(s"Received Multipart form - ${form.value}")
  }

  //https://stackoverflow.com/questions/19116016/what-is-the-right-way-to-post-multipart-form-data-using-curl
  //https://everything.curl.dev/http/multipart
  //curl -v -F secret=@file.txt http://localhost:8080/multipart3
  @Post("/multipart3")
  def multipartForm3 = Action.multipart { implicit request =>
    println(s"request.names() = ${request.names()}")

    val files = request.files
    println(s"files = ${files}")

    Ok(s"Received multipart request with files: ${files.size}")
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
    Ok(s"Hello !${name}, postalCode:${postalCode} - uri ${request.uri}")
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

  /*
   * NB: Currently, for Method Post, both data and query arg will work only from Armeria 1.19.0 onwards,
   * the only workround is to use Request as an function argument as in /form-request2
   */
  @Post("/form-request1") //curl -d "name=homer" -X POST  http://localhost:8080/form-request1\?queryArg\=12345
  def formRequest1(@Param queryArg: Int) =
    Action { implicit request =>
      s"queryArg:${queryArg} , req ${request.asFormUrlEncoded} - uri ${request.uri}"
    }

  @Post("/form-request2") //curl -d "name=homer" -X POST  http://localhost:8080/form-request2\?queryArg\=12345
  def formRequest2(@Param queryArg: Int)(using request: Request) =
    s"queryArg:${queryArg} , req ${request.asFormUrlEncoded} - uri ${request.uri}"

  @Post
  @Path("/form-request3/:queryArg")
  def formRequest3(@Param queryArg: Int)(using request: Request) =
    s"queryArg:${queryArg} , req ${request.asFormUrlEncoded} - uri ${request.uri}"

}
