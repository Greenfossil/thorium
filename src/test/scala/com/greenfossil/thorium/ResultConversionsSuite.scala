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

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.Get

import java.net.{URI, http}
import java.time.Duration
import scala.compiletime.uninitialized
import scala.util.{Failure, Success, Try}
import scala.language.implicitConversions

/**
 * Tests for the new Thorium response-type conversions and action timeout helpers.
 *
 * The conversions let controllers return natural Scala types directly:
 *
 * - `Try[String]` → `Result` (re-throw on Failure → 500)
 * - `Try[Result]` → `Result` (re-throw on Failure → 500)
 * - `Option[String]` → `Result` (NotFound on None)
 * - `Option[Result]` → `Result` (NotFound on None)
 * - `Either[String, String]` → `Result` (BadRequest on Left)
 * - `Either[String, Result]` → `Result` (BadRequest on Left)
 *
 * Also tests `Action.noTimeout` and `Action.timeout(duration)`.
 */
object ResultConversionsServices:

  // --- Try[String] — the natural form: no Ok(...) wrapper needed ---

  @Get("/try-success-string")
  def trySuccessString: Action = Action { _ =>
    Success("hello from try"): Try[String]
  }

  @Get("/try-failure")
  def tryFailure: Action = Action { _ =>
    Failure(new RuntimeException("boom")): Try[String]
  }

  @Get("/try-success-json")
  def trySuccessJson: Action = Action { _ =>
    Success(Json.obj("status" -> "ok")): Try[Result]
  }

  @Get("/try-success-result")
  def trySuccessResult: Action = Action { _ =>
    Success(Ok("explicit result")): Try[Result]
  }

  // --- Option[String] — the natural form ---

  @Get("/option-some-string")
  def optionSomeString: Action = Action { _ =>
    Some("found it"): Option[String]
  }

  @Get("/option-none")
  def optionNone: Action = Action { _ =>
    None: Option[String]
  }

  @Get("/option-some-json")
  def optionSomeJson: Action = Action { _ =>
    Some(Json.obj("found" -> true)): Option[Result]
  }

  @Get("/option-some-result")
  def optionSomeResult: Action = Action { _ =>
    Some(Ok("explicit result")): Option[Result]
  }

  // --- Either[String, String] — the natural form ---

  @Get("/either-right-string")
  def eitherRightString: Action = Action { _ =>
    Right("all good"): Either[String, String]
  }

  @Get("/either-left")
  def eitherLeft: Action = Action { _ =>
    Left("validation failed"): Either[String, String]
  }

  @Get("/either-right-json")
  def eitherRightJson: Action = Action { _ =>
    Right(Json.obj("result" -> "success")): Either[String, Result]
  }

  @Get("/either-right-result")
  def eitherRightResult: Action = Action { _ =>
    Right(Ok("explicit result")): Either[String, Result]
  }

  // --- Action.noTimeout ---

  @Get("/no-timeout")
  def noTimeoutAction: Action = Action.noTimeout { _ =>
    "no-timeout-ok"
  }

  // --- Action.timeout ---

  @Get("/explicit-timeout")
  def explicitTimeoutAction: Action = Action.timeout(Duration.ofSeconds(30)) { _ =>
    "timeout-set"
  }

  // --- Combined: Try + noTimeout ---

  @Get("/try-no-timeout")
  def tryNoTimeout: Action = Action.noTimeout { _ =>
    Success("try-no-timeout-ok"): Try[String]
  }

  // --- Combined: Option + timeout ---

  @Get("/option-timeout")
  def optionTimeout: Action = Action.timeout(Duration.ofSeconds(60)) { _ =>
    Some("option-timeout-ok"): Option[String]
  }

end ResultConversionsServices

class ResultConversionsSuite extends munit.FunSuite:

  @volatile var server: Server = uninitialized

  override def beforeAll(): Unit =
    server = Server(0)
      .addServices(ResultConversionsServices)
      .start()

  override def afterAll(): Unit =
    Thread.sleep(500)
    server.stop()

  private def get(path: String): http.HttpResponse[String] =
    http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}$path"))
        .GET()
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )

  private def getBody(path: String): String =
    val resp = get(path)
    assertEquals(resp.statusCode(), 200, s"Expected 200 for $path but got ${resp.statusCode()}: ${resp.body()}")
    resp.body()

  private def getWithStatus(path: String): (Int, String) =
    val resp = get(path)
    (resp.statusCode(), resp.body())

  // ===========================================================================
  // 1. Try[A] → Result (re-throw on Failure)
  // ===========================================================================

  test("Try[String] Success returns the string as OK") {
    val body = getBody("/try-success-string")
    assertNoDiff(body, "hello from try")
  }

  test("Try[String] Failure re-throws → 500") {
    val (status, _) = getWithStatus("/try-failure")
    assertEquals(status, 500, "Failure should re-throw → 500")
  }

  test("Try[Result] Success with JsValue body works") {
    val resp = get("/try-success-json")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"status\":\"ok\""))
  }

  test("Try[Result] Success with explicit Ok result works") {
    val body = getBody("/try-success-result")
    assertNoDiff(body, "explicit result")
  }

  // ===========================================================================
  // 2. Option[A] → Result (NotFound on None)
  // ===========================================================================

  test("Option[String] Some returns the string as OK") {
    val body = getBody("/option-some-string")
    assertNoDiff(body, "found it")
  }

  test("Option[String] None returns 404") {
    val (status, _) = getWithStatus("/option-none")
    assertEquals(status, 404, "None should become NotFound")
  }

  test("Option[Result] Some with JsValue body works") {
    val resp = get("/option-some-json")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"found\":true"))
  }

  test("Option[Result] Some with explicit Ok result works") {
    val body = getBody("/option-some-result")
    assertNoDiff(body, "explicit result")
  }

  // ===========================================================================
  // 3. Either[String, A] → Result (BadRequest on Left)
  // ===========================================================================

  test("Either[String, String] Right returns the string as OK") {
    val body = getBody("/either-right-string")
    assertNoDiff(body, "all good")
  }

  test("Either[String, String] Left returns 400 BadRequest") {
    val (status, body) = getWithStatus("/either-left")
    assertEquals(status, 400, "Left should become BadRequest")
    assert(body.contains("validation failed"))
  }

  test("Either[String, Result] Right with JsValue body works") {
    val resp = get("/either-right-json")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"result\":\"success\""))
  }

  test("Either[String, Result] Right with explicit Ok result works") {
    val body = getBody("/either-right-result")
    assertNoDiff(body, "explicit result")
  }

  // ===========================================================================
  // 4. Action.noTimeout
  // ===========================================================================

  test("Action.noTimeout clears the request timeout and runs the action") {
    val body = getBody("/no-timeout")
    assertNoDiff(body, "no-timeout-ok")
  }

  // ===========================================================================
  // 5. Action.timeout
  // ===========================================================================

  test("Action.timeout sets an explicit timeout and runs the action") {
    val body = getBody("/explicit-timeout")
    assertNoDiff(body, "timeout-set")
  }

  // ===========================================================================
  // 6. Combined: conversions + timeout helpers
  // ===========================================================================

  test("Try[String] inside Action.noTimeout works") {
    val body = getBody("/try-no-timeout")
    assertNoDiff(body, "try-no-timeout-ok")
  }

  test("Option[String] inside Action.timeout works") {
    val body = getBody("/option-timeout")
    assertNoDiff(body, "option-timeout-ok")
  }

end ResultConversionsSuite