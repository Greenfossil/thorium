package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, HttpResponse}

import java.util.Base64

extension (resp: HttpResponse)
  def withCookies(cookies: Cookie*): HttpResponse =
    resp.mapHeaders(_.toBuilder.cookies(cookies*).build())

  def discardingCookies(cookies: String*): HttpResponse =
    ???

  def withNewSession: HttpResponse = ???

  def withSession(nvPair: Map[String, String]): HttpResponse =
    val sessionCookie = Cookie.ofSecure(RequestAttrs.Session.name(), nvPair.toString)
    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())

  def withSession(newSession: Session): HttpResponse =
    //FIXME - need to merge with existing session
    val value = Base64.getUrlEncoder.encodeToString(Json.toJson(newSession.data).toString.getBytes())
    val sessionCookie = Cookie.ofSecure(RequestAttrs.Session.name(),value)
    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())


  def withSession(nvPair: (String, Any)): HttpResponse =
    //TODO - need to improve Json library
    val value = Base64.getUrlEncoder.encodeToString(Json.obj(nvPair._1 -> nvPair._2.toString).toString.getBytes())
    val sessionCookie = Cookie.ofSecure(RequestAttrs.Session.name(),value)
    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())

  def flashing(flash: Flash): HttpResponse =
    //FIXME - need to merge with newly flashed message
    val value = Base64.getUrlEncoder.encodeToString(Json.toJson(flash.data).toString.getBytes())
    val sessionCookie = Cookie.ofSecure(RequestAttrs.Flash.name(),value)
    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())
  
  def flashing(tups: (String, String)*): HttpResponse =
    flashing(Flash(tups.toMap))

  def withHeaders(tup: (String, String)*): HttpResponse = ???
