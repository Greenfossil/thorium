package com.greenfossil.thorium.examples

import com.greenfossil.thorium.{*, given}

@main def main =
  val server = WebServer(8080)
    .addService("/simpleHttpService", Action{ request =>
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