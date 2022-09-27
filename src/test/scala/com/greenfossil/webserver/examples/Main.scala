package com.greenfossil.webserver.examples

import com.greenfossil.webserver.*

@main def main =
  val server = WebServer(8080)
    .addService("/simpleHttpService", Action{ request =>
      import com.greenfossil.webserver.given
      Ok(s"Howdy! env:${request.env.mode}")
    })
    .addServices(BasicServices,FormServices,SimpleServices, ParameterizedServices)
    .addServices(MultipartServices)
    .addDocService()
    .addBeforeStartInit(sb => {
      sb.serviceUnder("/docs", new com.linecorp.armeria.server.docs.DocService())
    })
    .start()
  println(s"Server started... ${Thread.currentThread()}")