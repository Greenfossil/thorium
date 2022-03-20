package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, HttpResponse}

import java.util.Base64

//extension (resp: HttpResponse)
//  def withCookies(cookies: Cookie*): HttpResponse =
//    resp.mapHeaders(_.toBuilder.cookies(cookies*).build())
//
//  def discardingCookies(cookies: String*): HttpResponse =
//    ???
//
//  def withNewSession: HttpResponse = ???
//
//  def withSession(nvPair: Map[String, String]): HttpResponse =
//    val jwt = Json.toJson(nvPair).encodeBase64URL
//    val sessionCookie = Cookie.ofSecure(RequestAttrs.Session.name(), jwt)
//    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())
//
//  def withSession(newSession: Session): HttpResponse =
//    //FIXME - need to merge with existing session
//    val jwt = Json.toJson(newSession.data).encodeBase64URL
//    val sessionCookie = Cookie.ofSecure(RequestAttrs.Session.name(),jwt)
//    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())
//
//  def withSession(nvPair: (String, Any)): HttpResponse =
//    //TODO - need to improve Json library
//    val jwt = Json.toJson(nvPair).encodeBase64URL
//    val sessionCookie = Cookie.ofSecure(RequestAttrs.Session.name(),jwt)
//    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())
//
//  def flashing(flash: Flash): HttpResponse =
//    //FIXME - need to merge with newly flashed message
//    val jwt = Json.toJson(flash.data).encodeBase64URL
//    val sessionCookie = Cookie.ofSecure(RequestAttrs.Flash.name(),jwt)
//    resp.mapHeaders(_.toBuilder.cookies(sessionCookie).build())
//  
//  def flashing(tups: (String, String)*): HttpResponse =
//    flashing(Flash(tups.toMap))
//
//  def withHeaders(tup: (String, String)*): HttpResponse = ???
