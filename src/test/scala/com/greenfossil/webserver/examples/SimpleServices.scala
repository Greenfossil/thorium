package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{Action, Controller, Request}
import com.linecorp.armeria.server.annotation.{Get, Param}

object SimpleServices extends Controller {

  @Get("/simple-text/{name}/:date")
  def simpleText(@Param name: String)(@Param date: java.time.LocalDate)(using request: Request) =
    s"HelloWorld ${name}! $date path:${request.requestContext.path()}"
  
}
