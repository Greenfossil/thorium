package com.greenfossil.webserver.examples

import com.greenfossil.webserver.*

@main def main =
  val server = WebServer(8080)
    .addService("/simpleHttpService", Action{ request =>
      import com.greenfossil.webserver.given
      Ok(s"Howdy! env:${request.env.mode}")
    })
    .addServices(BasicServices,FormServices,SimpleServices)
    .start()
  println(s"Server started... ${Thread.currentThread()}")