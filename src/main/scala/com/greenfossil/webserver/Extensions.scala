package com.greenfossil.webserver

import com.linecorp.armeria.common.{Cookie, HttpResponse}

extension (resp: HttpResponse)
  def withCookies(cookies: Cookie*): HttpResponse =
    resp.mapHeaders(_.toBuilder.cookies(cookies*).build())

  def discardingCookies(cookies: String*): HttpResponse =
    ???

  def withNewSession: HttpResponse = ???

  def withSession(nvPair: Map[String, String]): HttpResponse = ???

  def withSession(nvPair: (String, String)): HttpResponse = ???

  def flashing(flash: Flash): HttpResponse =  ???

  def withHeaders(tup: (String, String)*): HttpResponse = ???
