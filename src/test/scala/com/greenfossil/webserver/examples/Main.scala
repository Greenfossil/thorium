package com.greenfossil.webserver.examples

import com.greenfossil.webserver.*

@main def main =
  val server = WebServer(8080)
    .addServices(BasicServices)
    .addServices(FormServices)
    .addServices(SimpleServices)
    .start()
  println(s"Server started... ${Thread.currentThread()}")