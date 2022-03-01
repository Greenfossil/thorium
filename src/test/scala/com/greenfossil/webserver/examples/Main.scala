package com.greenfossil.webserver.examples

import com.greenfossil.webserver.*

@main def main =
  val server = WebServer(8080)
    .addService("/text", BasicServices.helloText)
    .addService("/json", BasicServices.helloJson)
    .addService("/redirect", BasicServices.redirectText)
    .addService("/redirectText2", BasicServices.redirectText2)
    .addService("/multipart", BasicServices.multipartForm)
    .start()
  println(s"Server started... ${Thread.currentThread()}")