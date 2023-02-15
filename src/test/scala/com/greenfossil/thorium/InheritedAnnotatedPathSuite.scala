package com.greenfossil.thorium

import com.greenfossil.thorium.Sub2.foo
import com.linecorp.armeria.server.annotation.Get

trait Base:
  @Get("/base/foo")
  def foo = Action { request =>
    "hello foo"
  }

  @Get("/base/bar")
  def bar = Action{ request =>
    Redirect(foo)
  }


object Sub1 extends Base:
  @Get("/sub1/foo")
  override def foo = Action { request =>
    "hello sub1 foo"
  }

  @Get("/sub1/foobaz")
  def foobaz = Action {request =>
    Redirect(foo)
  }

  def foobazRedirect =  EndpointMcr(foo).url

object Sub2 extends Base:

  @Get("/sub2/foobaz")
  def foobaz = Action {request =>
    Redirect(foo)
  }

  def foobazRedirect = EndpointMcr(foo).url



class InheritedAnnotatedPathSuite extends munit.FunSuite:

  test("inherited Impl1.foo"){
    val ep = Sub1.foo.endpoint
    println(s"ep.url = ${ep.url}")
    assertNoDiff(ep.url, "/sub1/foo")
  }

  test("inherited Impl1.toFoo"){
    val redirect1 = Sub1.foobazRedirect
    println(s"redirect1 = ${redirect1}")
    assertNoDiff(redirect1, "/sub1/foo")


    val redirect2 = Sub2.foobazRedirect
    println(s"redirect2 = ${redirect2}")
    assertNoDiff(redirect2, "/base/foo")
  }


