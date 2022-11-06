package com.greenfossil.thorium.examples

import com.linecorp.armeria.server.annotation.{Get, Param}
import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.common.{HttpRequest, HttpResponse, HttpStatus}
import com.linecorp.armeria.server.{RoutingContext, ServiceConfig, ServiceRequestContext}

object RedirectedServices:

  @Get("/s0")
  def s0 = Action { request =>
    Redirect( s1("howdy").endpoint.prefixedUrl(using request))
  }

  @Get("/s1/:name")
  def s1(@Param name: String) = Action { request =>
    s"Hello ${name}"
  }
