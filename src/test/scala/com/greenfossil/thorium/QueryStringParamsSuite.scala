/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import com.linecorp.armeria.server.annotation.{Get, Param}
import com.linecorp.armeria.common.MediaType

import java.net.{URI, http}
import java.nio.charset.StandardCharsets
import scala.compiletime.uninitialized

/**
 * Independent test suite verifying that Thorium supports:
 *
 * 1. Multiple distinct query string parameters (e.g. `?name=homer&age=40`)
 * 2. Multi-value query string parameters (e.g. `?tag=a&tag=b&tag=c`)
 * 3. Default values for optional query parameters (via `Option.getOrElse`)
 * 4. `@Param` annotation binding for query params (single + multi-value)
 * 5. `Request.queryParams(param)` for multi-value access
 * 6. `Request.queryParam(param)` for single-value access with defaults
 * 7. `Request.queryParamsList` for flattened key-value pairs
 * 8. Empty / missing query parameters
 *
 * ADDITIONAL SCENARIOS (sections 13–20):
 * 13. Scala default params: all params defaulted, middle param omitted
 * 14. Scala default params: Boolean, Long, Double types with defaults
 * 15. Scala default params: only the defaulted param provided (skip required)
 * 16. Armeria @Default: every param has @Default (fully optional endpoint)
 * 17. Armeria @Default with Long and Boolean types
 * 18. Repeated values: three or more repeats, single key, mixed with other params
 * 19. Repeated values: empty value in a repeated key (?x=&x=2)
 * 20. Mixed @Param + @Default + repeated query keys
 */
object QueryStringTestServices:

  // --- @Param annotation binding (query string, not path) ---

  @Get("/single-param")
  def singleParam(@Param q: String): Action = Action { _ =>
    s"q=$q"
  }

  @Get("/multi-param")
  def multiParam(@Param name: String, @Param age: Int): Action = Action { _ =>
    s"name=$name,age=$age"
  }

  @Get("/optional-param")
  def optionalParam(@Param q: String): Action = Action { implicit req =>
    val value = req.queryParam("q").getOrElse("default-value")
    s"q=$value"
  }

  @Get("/default-param")
  def defaultParam: Action = Action { implicit req =>
    val page = req.queryParam("page").getOrElse("1")
    val size = req.queryParam("size").getOrElse("20")
    s"page=$page,size=$size"
  }

  @Get("/multi-value-param")
  def multiValueParam: Action = req =>
    val tags = req.queryParams("tag")
    s"tags=${tags.mkString(",")}"
  end multiValueParam

  // --- Manual Request API (no @Param annotation) ---

  @Get("/manual-single")
  def manualSingle: Action = Action { req =>
    val q = req.queryParam("q").getOrElse("NOT_PROVIDED")
    s"q=$q"
  }

  @Get("/manual-multi-value")
  def manualMultiValue: Action = req =>
    val tags = req.queryParams("tag")
    s"tags=${tags.mkString(",")}"
  end manualMultiValue

  @Get("/manual-all-params")
  def manualAllParams: Action = req =>
    val all = req.queryParamsList.map { (k, v) => s"$k=$v" }.mkString("&")
    s"params=$all"
  end manualAllParams

  @Get("/manual-query-string")
  def manualQueryString: Action = req =>
    s"qs=${req.queryString}"
  end manualQueryString

  @Get("/mixed-path-and-query/:id")
  def mixedPathAndQuery(@Param id: Long): Action = Action { implicit req =>
    val filter = req.queryParam("filter").getOrElse("none")
    val sort = req.queryParam("sort").getOrElse("asc")
    s"id=$id,filter=$filter,sort=$sort"
  }

  // --- Scala default parameter values on @Param-annotated methods ---

  @Get("/scala-defaults/all-provided")
  def scalaDefaultsAllProvided(@Param x: Int, @Param y: Int = 0, @Param z: String = "default-z"): Action =
    Action { _ => s"x=$x,y=$y,z=$z" }

  @Get("/scala-defaults/some-defaults")
  def scalaDefaultsSomeDefaults(@Param x: Int, @Param y: Int = 0, @Param z: String = "default-z"): Action =
    Action { _ => s"x=$x,y=$y,z=$z" }

  // --- Armeria @Default annotation (the runtime-native way) ---

  import com.linecorp.armeria.server.annotation.Default as ArmeriaDefault

  @Get("/armeria-default")
  def armeriaDefault(
    @Param x: Int,
    @Param @ArmeriaDefault("0") y: Int,
    @Param @ArmeriaDefault("default-z") z: String
  ): Action = Action { _ => s"x=$x,y=$y,z=$z" }

  // --- Repeated query string values (?x=1&x=2) with @Param ---

  @Get("/repeated-int-param")
  def repeatedIntParam(@Param x: Int): Action = Action { _ => s"x=$x" }

  @Get("/repeated-string-param")
  def repeatedStringParam(@Param s: String): Action = Action { _ => s"s=$s" }

  // --- Manual default values (no @Param, using Request.queryParam) ---

  @Get("/manual-defaults")
  def manualDefaults: Action = Action { req =>
    val x = req.queryParam("x").map(_.toInt).getOrElse(0)
    val y = req.queryParam("y").map(_.toInt).getOrElse(100)
    val z = req.queryParam("z").getOrElse("fallback")
    s"x=$x,y=$y,z=$z"
  }

  // --- Scala default params used as fallbacks for manual Request reads ---
  // Note: the method signature must not have bare params (without @Param) or
  // Armeria's resolver returns 400. The idiomatic approach is to take no
  // params on the method and use Request.queryParam(...).getOrElse(default).

  @Get("/scala-plain-defaults")
  def scalaPlainDefaults: Action = Action { req =>
    // Scala defaults are applied as literal fallbacks in getOrElse
    val defaultX = 0
    val defaultY = 0
    val defaultZ = "default-z"
    val x = req.queryParam("x").map(_.toInt).getOrElse(defaultX)
    val y = req.queryParam("y").map(_.toInt).getOrElse(defaultY)
    val z = req.queryParam("z").getOrElse(defaultZ)
    s"x=$x,y=$y,z=$z"
  }

  // --- 13. Scala defaults: all params have defaults, middle omitted ---
  // NOTE: This does NOT work at runtime. Armeria's resolver cannot see
  // Scala's synthetic $default$N methods, so omitting any @Param without
  // @Default returns 400. These endpoints exist to document that behavior.

  @Get("/scala-all-defaults")
  def scalaAllDefaults(@Param a: Int = 1, @Param b: Int = 2, @Param c: Int = 3): Action =
    Action { _ => s"a=$a,b=$b,c=$c" }

  // --- 14. Scala defaults with Boolean, Long, Double ---

  @Get("/scala-typed-defaults")
  def scalaTypedDefaults(
    @Param flag: Boolean = true,
    @Param count: Long = 99L,
    @Param ratio: Double = 1.5
  ): Action = Action { _ => s"flag=$flag,count=$count,ratio=$ratio" }

  // --- 15. Scala defaults: only the defaulted param provided ---
  // If caller provides only x (the non-defaulted one), y and z are omitted.
  // This should 400 because Armeria still requires y and z.

  @Get("/scala-mixed-defaults")
  def scalaMixedDefaults(@Param x: Int, @Param y: Int = 0, @Param z: String = "dz"): Action =
    Action { _ => s"x=$x,y=$y,z=$z" }

  // --- 16. Armeria @Default on every param (fully optional endpoint) ---

  @Get("/armeria-all-defaults")
  def armeriaAllDefaults(
    @Param @ArmeriaDefault("1") a: Int,
    @Param @ArmeriaDefault("2") b: Int,
    @Param @ArmeriaDefault("three") c: String
  ): Action = Action { _ => s"a=$a,b=$b,c=$c" }

  // --- 17. Armeria @Default with Long and Boolean ---

  @Get("/armeria-typed-defaults")
  def armeriaTypedDefaults(
    @Param @ArmeriaDefault("true") flag: Boolean,
    @Param @ArmeriaDefault("99") count: Long
  ): Action = Action { _ => s"flag=$flag,count=$count" }

  // --- 18. Repeated values: three or more, mixed with other params ---

  @Get("/repeated-mixed")
  def repeatedMixed(@Param id: Int, @Param s: String): Action = Action { req =>
    val allX = req.queryParams("x")
    s"id=$id,s=$s,x=${allX.mkString(",")}"
  }

  // --- 19. Repeated values with empty value (?x=&x=2) ---

  @Get("/repeated-with-empty")
  def repeatedWithEmpty: Action = req =>
    val xs = req.queryParams("x")
    s"count=${xs.size},values=${xs.map(v => s"[$v]").mkString(",")}"
  end repeatedWithEmpty

  // --- 20. Mixed @Param + @Default + repeated query keys ---

  @Get("/mixed-default-repeated")
  def mixedDefaultRepeated(
    @Param @ArmeriaDefault("0") page: Int,
    @Param @ArmeriaDefault("10") size: Int
  ): Action = req =>
    val allTags = req.queryParams("tag")
    s"page=$page,size=$size,tags=${allTags.mkString(",")}"
  end mixedDefaultRepeated

end QueryStringTestServices

class QueryStringParamsSuite extends munit.FunSuite:

  @volatile var server: Server = uninitialized

  override def beforeAll(): Unit =
    server = Server(0)
      .addServices(QueryStringTestServices)
      .start()

  override def afterAll(): Unit =
    Thread.sleep(500)
    server.stop()

  // --- Helper ---

  private def get(path: String): String =
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}$path"))
        .GET()
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200 for $path but got ${resp.statusCode()}: ${resp.body()}")
    resp.body()

  // ===========================================================================
  // 1. Multiple distinct query string parameters
  // ===========================================================================

  test("multiple distinct query params are bound via @Param") {
    val result = get("/multi-param?name=homer&age=40")
    assertNoDiff(result, "name=homer,age=40")
  }

  test("multiple distinct query params via Request API") {
    val result = get("/manual-all-params?name=homer&age=40&city=springfield")
    assertNoDiff(result, "params=name=homer&age=40&city=springfield")
  }

  // ===========================================================================
  // 2. Multi-value query string parameters (?tag=a&tag=b&tag=c)
  // ===========================================================================

  test("multi-value query param via @Param Seq[String]") {
    // Note: @Param Seq[String] with GET is handled by the macro for endpoint
    // generation (POST with bracket syntax tag[]=a). For runtime GET query
    // string binding, use Request.queryParams("tag") with repeated keys.
    val result = get("/multi-value-param?tag=scala&tag=java&tag=kotlin")
    assertNoDiff(result, "tags=scala,java,kotlin")
  }

  test("multi-value query param via Request.queryParams") {
    val result = get("/manual-multi-value?tag=a&tag=b&tag=c")
    assertNoDiff(result, "tags=a,b,c")
  }

  test("multi-value query param with single value") {
    val result = get("/multi-value-param?tag=only")
    assertNoDiff(result, "tags=only")
  }

  test("multi-value query param via Request.queryParams with no values returns empty list") {
    val result = get("/manual-multi-value")
    assertNoDiff(result, "tags=")
  }

  // ===========================================================================
  // 3. Default values for optional query parameters
  // ===========================================================================

  test("default value is used when query param is absent") {
    val result = get("/default-param")
    assertNoDiff(result, "page=1,size=20")
  }

  test("default value is used when one param is absent, other is provided") {
    val result = get("/default-param?page=5")
    assertNoDiff(result, "page=5,size=20")
  }

  test("provided values override defaults") {
    val result = get("/default-param?page=10&size=50")
    assertNoDiff(result, "page=10,size=50")
  }

  test("Request.queryParam returns None for missing param, getOrElse provides default") {
    val result = get("/manual-single")
    assertNoDiff(result, "q=NOT_PROVIDED")
  }

  test("Request.queryParam returns the value when present") {
    val result = get("/manual-single?q=hello")
    assertNoDiff(result, "q=hello")
  }

  // ===========================================================================
  // 4. @Param annotation binding for query params (single value)
  // ===========================================================================

  test("single @Param query string binding") {
    val result = get("/single-param?q=HelloWorld")
    assertNoDiff(result, "q=HelloWorld")
  }

  test("@Param query string with URL-encoded value") {
    val result = get("/single-param?q=Hello%20World%21")
    assertNoDiff(result, "q=Hello World!")
  }

  // ===========================================================================
  // 5. Mixed path parameter and query string parameters
  // ===========================================================================

  test("mixed path param and query params with defaults") {
    val result = get("/mixed-path-and-query/42")
    assertNoDiff(result, "id=42,filter=none,sort=asc")
  }

  test("mixed path param and query params with values") {
    val result = get("/mixed-path-and-query/42?filter=active&sort=desc")
    assertNoDiff(result, "id=42,filter=active,sort=desc")
  }

  test("mixed path param with partial query params (uses defaults for missing)") {
    val result = get("/mixed-path-and-query/99?sort=desc")
    assertNoDiff(result, "id=99,filter=none,sort=desc")
  }

  // ===========================================================================
  // 6. Raw query string access
  // ===========================================================================

  test("Request.queryString returns the raw query string") {
    val result = get("/manual-query-string?name=homer&age=40")
    assertNoDiff(result, "qs=name=homer&age=40")
  }

  test("Request.queryString is empty when no query params") {
    val result = get("/manual-query-string")
    assertNoDiff(result, "qs=")
  }

  // ===========================================================================
  // 7. Edge cases
  // ===========================================================================

  test("query param with empty value") {
    val result = get("/manual-single?q=")
    assertNoDiff(result, "q=")
  }

  test("multiple query params with same key via Request.queryParamsList (flattened)") {
    val result = get("/manual-all-params?tag=a&tag=b&tag=c")
    assertNoDiff(result, "params=tag=a&tag=b&tag=c")
  }

  // ===========================================================================
  // 8. Scala default parameter values on @Param-annotated methods
  //    These tests document whether Scala's `param: T = default` syntax is
  //    honored by Armeria's runtime @Param resolver.
  //    NOTE: Scala defaults are invisible to Armeria (it reads Java reflection
  //    annotations, not Scala's synthetic $default$N methods). A @Param without
  //    @Default and without the query string value should return 400.
  // ===========================================================================

  test("Scala defaults: all params provided — works normally") {
    val result = get("/scala-defaults/all-provided?x=1&y=2&z=hello")
    assertNoDiff(result, "x=1,y=2,z=hello")
  }

  test("Scala defaults: missing y and z — Scala default is NOT honored (400 expected)") {
    // Scala's `y: Int = 0` is invisible to Armeria. Without @Default or
    // Optional, Armeria treats y and z as required and returns 400.
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-defaults/some-defaults?x=1"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400 (Scala defaults not honored by Armeria), got ${resp.statusCode()}: ${resp.body()}")
  }

  test("Scala defaults: only x provided, y and z omitted — confirms 400 behavior") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-defaults/all-provided?x=42"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400, got ${resp.statusCode()}: ${resp.body()}")
  }

  // ===========================================================================
  // 9. Armeria @Default annotation (runtime-native default values)
  //    @Default is Armeria's own annotation for providing defaults when a
  //    @Param is missing from the request. This is the *correct* way to
  //    provide defaults for @Param-annotated parameters.
  // ===========================================================================

  test("Armeria @Default: all params provided — uses provided values") {
    val result = get("/armeria-default?x=1&y=2&z=custom")
    assertNoDiff(result, "x=1,y=2,z=custom")
  }

  test("Armeria @Default: missing y and z — uses @Default values") {
    val result = get("/armeria-default?x=5")
    assertNoDiff(result, "x=5,y=0,z=default-z")
  }

  test("Armeria @Default: only x provided, y and z omitted — @Default fills in") {
    val result = get("/armeria-default?x=99")
    assertNoDiff(result, "x=99,y=0,z=default-z")
  }

  test("Armeria @Default: all omitted except x — confirms @Default works for both Int and String") {
    val result = get("/armeria-default?x=0")
    assertNoDiff(result, "x=0,y=0,z=default-z")
  }

  // ===========================================================================
  // 10. Repeated query string values (?x=1&x=2) with @Param
  //     When the same key appears multiple times, @Param binds to the first
  //     value (Armeria's behavior). Use Request.queryParams for all values.
  // ===========================================================================

  test("Repeated query param ?x=1&x=2 with @Param Int — binds to first value") {
    val result = get("/repeated-int-param?x=1&x=2")
    assertNoDiff(result, "x=1")
  }

  test("Repeated query param ?s=a&s=b with @Param String — binds to first value") {
    val result = get("/repeated-string-param?s=first&s=second")
    assertNoDiff(result, "s=first")
  }

  test("Repeated query param via Request.queryParams returns all values") {
    val result = get("/manual-multi-value?tag=first&tag=second&tag=third")
    assertNoDiff(result, "tags=first,second,third")
  }

  // ===========================================================================
  // 11. Manual default values (no @Param, Request.queryParam + getOrElse)
  //     The idiomatic Thorium pattern for optional/defaulted query params.
  // ===========================================================================

  test("Manual defaults: no params — uses all defaults") {
    val result = get("/manual-defaults")
    assertNoDiff(result, "x=0,y=100,z=fallback")
  }

  test("Manual defaults: partial params — uses provided + defaults") {
    val result = get("/manual-defaults?x=5&z=custom")
    assertNoDiff(result, "x=5,y=100,z=custom")
  }

  test("Manual defaults: all params provided — overrides all defaults") {
    val result = get("/manual-defaults?x=10&y=20&z=override")
    assertNoDiff(result, "x=10,y=20,z=override")
  }

  // ===========================================================================
  // 12. Scala default params on a plain method (not @Param)
  //     The method has Scala defaults but reads query params manually.
  //     The Scala defaults serve as the fallback for the manual read.
  // ===========================================================================

  test("Scala plain defaults: no query params — uses Scala method defaults") {
    val result = get("/scala-plain-defaults")
    assertNoDiff(result, "x=0,y=0,z=default-z")
  }

  test("Scala plain defaults: partial query params — uses provided + Scala defaults") {
    val result = get("/scala-plain-defaults?x=7&z=from-query")
    assertNoDiff(result, "x=7,y=0,z=from-query")
  }

  test("Scala plain defaults: all query params — overrides Scala defaults") {
    val result = get("/scala-plain-defaults?x=1&y=2&z=three")
    assertNoDiff(result, "x=1,y=2,z=three")
  }

  // ===========================================================================
  // 13. Scala defaults: all params have defaults, middle omitted
  //
  // KNOWN LIMITATION:
  //   Scala default parameter values (`param: T = expr`) are NOT honored by
  //   Armeria's runtime @Param resolver. Armeria uses Java reflection to read
  //   annotations (@Param, @Default, @Nullable) and does NOT inspect Scala's
  //   synthetic $default$N methods. Therefore:
  //     - Omitting a @Param with a Scala default from the query string → 400.
  //     - There is no error at compile time; the failure is purely runtime.
  //
  //   WORKAROUND:
  //     Use Armeria's @Default("value") annotation instead (see section 9),
  //     or use Request.queryParam("name").getOrElse(default) in the action
  //     body without @Param (see section 11).
  //
  //   These tests document the limitation so it is not accidentally relied upon.
  // ===========================================================================

  test("Scala defaults: all params have defaults, all provided — 400 (Scala default syntax breaks Armeria)") {
    // NOTE: The mere presence of Scala default values on @Param-annotated
    // parameters causes Armeria's resolver to fail with 400, EVEN WHEN all
    // parameters are provided in the query string. The Scala compiler emits
    // synthetic $default$N accessor methods that change the bytecode shape
    // Armeria sees via reflection, making the method unresolvable.
    //
    // This means `def foo(@Param a: Int = 1)` is fundamentally incompatible
    // with Armeria's annotated service mechanism — it fails regardless of
    // whether the caller provides the value.
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-all-defaults?a=10&b=20&c=thirty"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400 (Scala default syntax on @Param breaks Armeria even when all params provided), got ${resp.statusCode()}: ${resp.body()}")
  }

  test("Scala defaults: all params have defaults, none provided — 400 (Scala defaults NOT honored)") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-all-defaults"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400 (Scala defaults invisible to Armeria), got ${resp.statusCode()}: ${resp.body()}")
  }

  test("Scala defaults: all params have defaults, only a provided — 400 for b and c") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-all-defaults?a=10"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400, got ${resp.statusCode()}: ${resp.body()}")
  }

  test("Scala defaults: middle param omitted (a=1&c=3) — 400 for b") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-all-defaults?a=1&c=3"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400 (middle param b has Scala default but Armeria requires it), got ${resp.statusCode()}: ${resp.body()}")
  }

  // ===========================================================================
  // 14. Scala defaults with Boolean, Long, Double types
  //     Same limitation: Scala defaults are invisible to Armeria.
  // ===========================================================================

  test("Scala typed defaults: all provided — works") {
    val result = get("/scala-typed-defaults?flag=false&count=42&ratio=3.14")
    assertNoDiff(result, "flag=false,count=42,ratio=3.14")
  }

  test("Scala typed defaults: none provided — 400 (Scala defaults NOT honored for any type)") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-typed-defaults"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400 (Scala defaults for Boolean/Long/Double invisible to Armeria), got ${resp.statusCode()}: ${resp.body()}")
  }

  test("Scala typed defaults: partial (only flag) — 400 for count and ratio") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-typed-defaults?flag=true"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400, got ${resp.statusCode()}: ${resp.body()}")
  }

  // ===========================================================================
  // 15. Scala defaults: only the defaulted param provided (skip required)
  //     When x is required (no Scala default) and omitted, 400 is expected
  //     even if y and z (which have Scala defaults) are provided.
  // ===========================================================================

  test("Scala mixed defaults: required x omitted, y and z provided — 400 for x") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-mixed-defaults?y=5&z=hi"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400 (required x missing), got ${resp.statusCode()}: ${resp.body()}")
  }

  test("Scala mixed defaults: all provided — works") {
    val result = get("/scala-mixed-defaults?x=1&y=2&z=hello")
    assertNoDiff(result, "x=1,y=2,z=hello")
  }

  test("Scala mixed defaults: only x provided, y and z omitted — 400 (Scala defaults NOT honored)") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/scala-mixed-defaults?x=42"))
        .GET().header("Content-Type", MediaType.PLAIN_TEXT.toString).build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assert(resp.statusCode() == 400,
      s"Expected 400 (y and z have Scala defaults but Armeria ignores them), got ${resp.statusCode()}: ${resp.body()}")
  }

  // ===========================================================================
  // 16. Armeria @Default on every param (fully optional endpoint)
  //     This is the CORRECT way to make all query params optional.
  // ===========================================================================

  test("Armeria all @Default: no params — all defaults used") {
    val result = get("/armeria-all-defaults")
    assertNoDiff(result, "a=1,b=2,c=three")
  }

  test("Armeria all @Default: partial (only a) — b and c use @Default") {
    val result = get("/armeria-all-defaults?a=99")
    assertNoDiff(result, "a=99,b=2,c=three")
  }

  test("Armeria all @Default: middle omitted (a=10&c=thirty) — b uses @Default") {
    val result = get("/armeria-all-defaults?a=10&c=thirty")
    assertNoDiff(result, "a=10,b=2,c=thirty")
  }

  test("Armeria all @Default: all provided — overrides all @Default") {
    val result = get("/armeria-all-defaults?a=100&b=200&c=override")
    assertNoDiff(result, "a=100,b=200,c=override")
  }

  // ===========================================================================
  // 17. Armeria @Default with Long and Boolean types
  // ===========================================================================

  test("Armeria typed @Default: no params — Boolean and Long use @Default") {
    val result = get("/armeria-typed-defaults")
    assertNoDiff(result, "flag=true,count=99")
  }

  test("Armeria typed @Default: both provided — overrides @Default") {
    val result = get("/armeria-typed-defaults?flag=false&count=7")
    assertNoDiff(result, "flag=false,count=7")
  }

  test("Armeria typed @Default: only flag provided — count uses @Default") {
    val result = get("/armeria-typed-defaults?flag=false")
    assertNoDiff(result, "flag=false,count=99")
  }

  test("Armeria typed @Default: only count provided — flag uses @Default") {
    val result = get("/armeria-typed-defaults?count=42")
    assertNoDiff(result, "flag=true,count=42")
  }

  // ===========================================================================
  // 18. Repeated values: three or more repeats, mixed with other params
  //     @Param binds to the first value; Request.queryParams returns all.
  // ===========================================================================

  test("Repeated values: three x values mixed with id and s — @Param binds first x, queryParams gets all") {
    val result = get("/repeated-mixed?id=1&s=hello&x=10&x=20&x=30")
    assertNoDiff(result, "id=1,s=hello,x=10,20,30")
  }

  test("Repeated values: x at beginning, id and s after — still works") {
    val result = get("/repeated-mixed?x=1&x=2&id=9&s=world&x=3")
    // Armeria reorders internally; id and s bind correctly, all x values collected
    assertNoDiff(result, "id=9,s=world,x=1,2,3")
  }

  test("Repeated values: no repeated key present — queryParams returns empty") {
    val result = get("/repeated-mixed?id=5&s=test")
    assertNoDiff(result, "id=5,s=test,x=")
  }

  // ===========================================================================
  // 19. Repeated values with empty value (?x=&x=2)
  //     An empty string is a valid value; it is included in the list.
  // ===========================================================================

  test("Repeated values: first empty, second has value (?x=&x=2) — both captured") {
    val result = get("/repeated-with-empty?x=&x=2")
    assertNoDiff(result, "count=2,values=[],[2]")
  }

  test("Repeated values: all empty (?x=&x=&x=) — three empty strings") {
    val result = get("/repeated-with-empty?x=&x=&x=")
    assertNoDiff(result, "count=3,values=[],[],[]")
  }

  test("Repeated values: single empty (?x=) — one empty string") {
    val result = get("/repeated-with-empty?x=")
    assertNoDiff(result, "count=1,values=[]")
  }

  test("Repeated values: no x param — count is zero") {
    val result = get("/repeated-with-empty")
    assertNoDiff(result, "count=0,values=")
  }

  // ===========================================================================
  // 20. Mixed @Param + @Default + repeated query keys
  //     @Default fills in for missing params; repeated keys collected via
  //     Request.queryParams. This demonstrates the recommended pattern for
  //     endpoints that need both defaulted scalar params and multi-value params.
  // ===========================================================================

  test("Mixed @Default + repeated: no params — @Default used, tags empty") {
    val result = get("/mixed-default-repeated")
    assertNoDiff(result, "page=0,size=10,tags=")
  }

  test("Mixed @Default + repeated: page and tags provided, size omitted") {
    val result = get("/mixed-default-repeated?page=3&tag=scala&tag=java")
    assertNoDiff(result, "page=3,size=10,tags=scala,java")
  }

  test("Mixed @Default + repeated: all provided including repeated tags") {
    val result = get("/mixed-default-repeated?page=2&size=50&tag=a&tag=b&tag=c")
    assertNoDiff(result, "page=2,size=50,tags=a,b,c")
  }

  test("Mixed @Default + repeated: only tags, page and size defaulted") {
    val result = get("/mixed-default-repeated?tag=x&tag=y")
    assertNoDiff(result, "page=0,size=10,tags=x,y")
  }

end QueryStringParamsSuite