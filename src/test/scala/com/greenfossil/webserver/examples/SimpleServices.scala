package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{Action, Controller, Request}
import com.linecorp.armeria.server.annotation.{Get, Param}

object SimpleServices extends Controller {

  @Get("/simple-text/{name}")
  def simpleText(@Param name: String, request: Request) =
    s"HelloWorld ${name}! path:${request.requestContext.path()}"


}
