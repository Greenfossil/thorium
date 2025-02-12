package com.greenfossil.thorium.macros

import com.greenfossil.thorium.*
import com.linecorp.armeria.server.annotation.decorator.RequestTimeout
import com.linecorp.armeria.server.annotation.{Get, Param, Path, Post}

import java.util.concurrent.TimeUnit

class AnnotatedActionMacroSupportSuite extends munit.FunSuite:

  @RequestTimeout(value=10, unit= TimeUnit.MINUTES) /*This is ignored, as only HttpMethod Annotation is processed*/
  @Get("/action-ep")
  def actionEp = Action { _ => "action-endpoint" }

  @Get("/req-func-ep1")
  def reqFuncEp1(request: Request) = s"using endpoint Int:${request.path}"

  @Get("/req-func-ep2/:msg")
  def reqFuncEp2(@Param msg: String)(req: Request) = s"${req.path} content:$msg"

  @Get("/req-func-ep3")
  def reqFuncEp3(@Param msg: String) = (req: Request) => s"${req.path} content:$msg"

  @Post
  @Path("/req-func-ep4/:id/:name")
  def reqFuncEp4(@Param id: Long, @Param name: String) = Action { _ => "reqFuncEp4" }

  @Get
  @Path("/req-func-ep5/:id/:name")
  def reqFuncEp5(@Param id: Long, @Param name: String) = Action { _ => "reqFuncEp5" }

  @Get("/endpoint-params/:int/:str")
  def endpointParams(@Param int: Int)(@Param str: String) = "endpoint1"

  def fn = "string"

  val x = "abc"


  given Request = null

  test("convertPathToPathParts") {
    val paths = List(
      "/endpoint1" -> List("/endpoint1"),
      "/endpoint2/:name" -> List("/endpoint2", ":name"),
      "/endpoint3/:name/info/age/:num" -> List("/endpoint3", ":name", "info", "age", ":num"),
      "/endpoint5/:id/:name" -> List("/endpoint5", ":id", ":name"),
      "prefix:/howdy" -> List("/howdy"),
      "/braced-params/{name}/{age}/:contact" -> List("/braced-params", ":name", ":age", ":contact"),
      "regex:^/string/(?<name>[^0-9]+)$" -> List("/string", ":name"), /*This path is different without ^$ "regex:/string/(?<name>[^0-9]+)"*/
      "regex:^/number/(?<n1>[0-9]+)$" -> List("/number", ":n1"),
      "regex:/string2/(?<min>\\w+)/(?<max>\\w+)" -> List("/string2", ":min", ":max"),
      "regex:/number2/(?<min>\\d+)/(?<max>\\d+)" -> List("/number2", ":min", ":max"),
      "regex:/mix/(?<min>\\d+)/(?<max>\\w+)" -> List("/mix", ":min", ":max"),
      "regex:/mix/(?<min>\\w+)/(?<max>\\d+)" -> List("/mix", ":min", ":max"),
      "regex:/mix/(?<min>\\w+)/max/(?<max>\\d+)" -> List("/mix", ":min", "max", ":max")
    )

    val results = paths.map { (path, expectation) =>
      (path, expectation, AnnotatedActionMacroSupport.convertPathToPathParts(path))
    }

    results.foreach{ (path, expectation, result) =>
      assertEquals(result, expectation)
    }
  }

  test("verifyActionTypeMcr"){
    val results = List(
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(actionEp) -> actionEp,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(reqFuncEp1) -> reqFuncEp1,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(reqFuncEp2) -> reqFuncEp2,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(reqFuncEp3) -> reqFuncEp3,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(reqFuncEp4) -> reqFuncEp4,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(reqFuncEp5) -> reqFuncEp5,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(endpointParams) -> endpointParams,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(fn) -> fn,
      AnnotatedActionMacroSupportTestImpl.verifyActionTypeMcr(x) -> x,
    )

    results.zipWithIndex.foreach{(x, index) =>
      val (result, expectation) = x
      expectation match
        case _: String => assertEquals(result, expectation)
        case _ => ()
    }
  }

  test("extractActionAnnotationsMcr"){
    val results = List(
      AnnotatedActionMacroSupportTestImpl.extractActionAnnotationsMcr(actionEp) -> (2, 0),
      AnnotatedActionMacroSupportTestImpl.extractActionAnnotationsMcr(reqFuncEp1) -> (1, 0),
      AnnotatedActionMacroSupportTestImpl.extractActionAnnotationsMcr(reqFuncEp2) -> (1, 1),
      AnnotatedActionMacroSupportTestImpl.extractActionAnnotationsMcr(reqFuncEp3) -> (1, 1),
      AnnotatedActionMacroSupportTestImpl.extractActionAnnotationsMcr(reqFuncEp4) -> (2, 2),
      AnnotatedActionMacroSupportTestImpl.extractActionAnnotationsMcr(reqFuncEp5) -> (2, 2),
      AnnotatedActionMacroSupportTestImpl.extractActionAnnotationsMcr(endpointParams) -> (1, 2),
    )

    results.zipWithIndex.foreach { (result, index) =>
      val (obtained, expected) = result
      assertEquals(obtained, expected)
    }
  }

  test("extractHttpVerbMcr") {
    val results = List(
      AnnotatedActionMacroSupportTestImpl.extractHttpVerbMcr(actionEp) -> "Method: Get, Path: /action-ep" ,
      AnnotatedActionMacroSupportTestImpl.extractHttpVerbMcr(reqFuncEp1) -> "Method: Get, Path: /req-func-ep1",
      AnnotatedActionMacroSupportTestImpl.extractHttpVerbMcr(reqFuncEp2) -> "Method: Get, Path: /req-func-ep2/:msg",
      AnnotatedActionMacroSupportTestImpl.extractHttpVerbMcr(reqFuncEp3) -> "Method: Get, Path: /req-func-ep3",
      AnnotatedActionMacroSupportTestImpl.extractHttpVerbMcr(reqFuncEp4) -> "Method: Post, Path: /req-func-ep4/:id/:name",
      AnnotatedActionMacroSupportTestImpl.extractHttpVerbMcr(reqFuncEp5) -> "Method: Get, Path: /req-func-ep5/:id/:name",
      AnnotatedActionMacroSupportTestImpl.extractHttpVerbMcr(endpointParams) -> "Method: Get, Path: /endpoint-params/:int/:str",
    )

    results.zipWithIndex.foreach { (result, index) =>
      val (obtained, expected) = result
      assertEquals(obtained, expected)
    }
  }

  test("computeActionAnnotatedPathMcr") {
    val results = List(
      AnnotatedActionMacroSupportTestImpl.computeActionAnnotatedPathMcr(actionEp) -> "/action-ep",
      AnnotatedActionMacroSupportTestImpl.computeActionAnnotatedPathMcr(reqFuncEp1) -> "/req-func-ep1",
      AnnotatedActionMacroSupportTestImpl.computeActionAnnotatedPathMcr(reqFuncEp2("homer!")) -> "/req-func-ep2/homer%21",
      AnnotatedActionMacroSupportTestImpl.computeActionAnnotatedPathMcr(reqFuncEp3("howdy!")) -> "/req-func-ep3?msg=howdy%21",
      AnnotatedActionMacroSupportTestImpl.computeActionAnnotatedPathMcr(reqFuncEp4(1, "hello")) -> "/req-func-ep4/1/hello",
      AnnotatedActionMacroSupportTestImpl.computeActionAnnotatedPathMcr(reqFuncEp5(1, "marge")) -> "/req-func-ep5/1/marge",
      AnnotatedActionMacroSupportTestImpl.computeActionAnnotatedPathMcr(endpointParams(1)("bart")) -> "/endpoint-params/1/bart",
    )

    results.zipWithIndex.foreach { (result, index) =>
      val (endpoint, expected) = result
      assertNoDiff(endpoint.url, expected)
    }
  }
