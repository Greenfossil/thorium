package com.greenfossil.webserver.examples

import com.greenfossil.webserver.*

@main def main =
  val server = WebServer(8080)
    .addAnnotatedService(BasicServices)
    .addAnnotatedService(FormServices)
    .start()
  println(s"Server started... ${Thread.currentThread()}")