package com.greenfossil.webserver.examples

import com.greenfossil.data.mapping.*
import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param, Post}

object MultipartServices extends Controller {

  /*
   * Method Multipart, both data and query arg does not work from Armeria 1.19.0 and before
   */
  @Post("/mp-form") //curl -v -F name=homer http://localhost:8080/mp-form\?queryArg\=12345
  def multipartForm(@Param queryArg: Int) =
    Action.multipart{request =>
      s"queryArg:[${queryArg}] req:[${request.asFormUrlEncoded}] uri:[${request.uri}]"
    }

  /*
   * Method Post, both data and query arg will work only from Armeria 1.19.0 onwards
   */
  @Post("/urlencoded-form") //curl -v -d name=homer http://localhost:8080/urlencoded-form\?queryArg\=12345
  def urlencodedForm(@Param queryArg: Int) =
    Action {request =>
      s"queryArg:[${queryArg}] req:[${request.asFormUrlEncoded}] uri:[${request.uri}]"
    }
  
}
