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
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Tests for Action.async / Future[ActionResponse] support.
 *
 * Verifies that:
 * 1. Action.async returns Future[Result] without blocking
 * 2. Future[String] works (wraps as OK via Result.apply)
 * 3. Future[Result] works (returned as-is)
 * 4. Future failures → 500 (via re-throw → ServerErrorHandler)
 * 5. Action.async + noTimeout / timeout compose correctly
 * 6. Sync actions still work alongside async actions
 */
object AsyncActionServices:
  given ExecutionContext = ExecutionContext.global

  // --- Async: Future[Result] ---

  @Get("/async-result")
  def asyncResult: Action = Action.async { _ =>
    Future(Ok("async-result-ok"))
  }

  // --- Async: Future[String] (natural type, no Ok wrapper) ---

  @Get("/async-string")
  def asyncString: Action = Action.async { _ =>
    Future("hello-async")
  }

  // --- Async: Future[JsValue] ---

  @Get("/async-json")
  def asyncJson: Action = Action.async { _ =>
    Future(Json.obj("status" -> "ok"))
  }

  // --- Async: Future failure → 500 ---

  @Get("/async-failure")
  def asyncFailure: Action = Action.async { _ =>
    Future.failed(new RuntimeException("async-boom"))
  }

  // --- Async: Future[Option[String]] — Option conversion inside Future ---

  @Get("/async-some")
  def asyncSome: Action = Action.async { _ =>
    Future(Some("found-async"))
  }

  @Get("/async-none")
  def asyncNone: Action = Action.async { _ =>
    Future(None: Option[String])
  }

  // --- Async: Future[Try[String]] — Try conversion inside Future ---

  @Get("/async-try-success")
  def asyncTrySuccess: Action = Action.async { _ =>
    Future(Success("try-async-ok"))
  }

  @Get("/async-try-failure")
  def asyncTryFailure: Action = Action.async { _ =>
    Future(Failure(new RuntimeException("try-async-boom")))
  }

  // --- Async: Future[Either[String, String]] ---

  @Get("/async-either-right")
  def asyncEitherRight: Action = Action.async { _ =>
    Future(Right("either-async-ok"))
  }

  @Get("/async-either-left")
  def asyncEitherLeft: Action = Action.async { _ =>
    Future(Left("validation-async-failed"))
  }

  // --- Async + noTimeout ---

  @Get("/async-no-timeout")
  def asyncNoTimeout: Action = Action.noTimeout { req =>
    Future(Ok("async-no-timeout-ok"))
  }

  // --- Async + timeout ---

  @Get("/async-timeout")
  def asyncTimeout: Action = Action.timeout(Duration.ofSeconds(30)) { req =>
    Future(Ok("async-timeout-ok"))
  }

  // --- Sync actions (must still work alongside async) ---

  @Get("/sync-string")
  def syncString: Action = Action { _ =>
    "sync-ok"
  }

  @Get("/sync-result")
  def syncResult: Action = Action { _ =>
    Ok("sync-result-ok")
  }

  // --- Async: simulated I/O delay ---

  @Get("/async-delayed")
  def asyncDelayed: Action = Action.async { _ =>
    Future {
      Thread.sleep(100) // simulate I/O
      Ok("delayed-ok")
    }
  }

end AsyncActionServices

class AsyncActionSuite extends munit.FunSuite:

  @volatile var server: Server = uninitialized

  override def beforeAll(): Unit =
    server = Server(0)
      .addServices(AsyncActionServices)
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
  // 1. Basic async: Future[Result]
  // ===========================================================================

  test("Action.async with Future[Result] returns the result") {
    val body = getBody("/async-result")
    assertNoDiff(body, "async-result-ok")
  }

  // ===========================================================================
  // 2. Async: Future[String] (natural type)
  // ===========================================================================

  test("Action.async with Future[String] returns the string as OK") {
    val body = getBody("/async-string")
    assertNoDiff(body, "hello-async")
  }

  // ===========================================================================
  // 3. Async: Future[JsValue]
  // ===========================================================================

  test("Action.async with Future[JsValue] returns JSON") {
    val resp = get("/async-json")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"status\":\"ok\""))
  }

  // ===========================================================================
  // 4. Async: Future failure → 500
  // ===========================================================================

  test("Action.async with Future.failed → 500") {
    val (status, _) = getWithStatus("/async-failure")
    assertEquals(status, 500, "Failed Future should → 500")
  }

  // ===========================================================================
  // 5. Async + Option conversion inside Future
  // ===========================================================================

  test("Action.async with Future[Some[String]] returns the string") {
    val body = getBody("/async-some")
    assertNoDiff(body, "found-async")
  }

  test("Action.async with Future[None] → 404") {
    val (status, _) = getWithStatus("/async-none")
    assertEquals(status, 404, "Future(None) should → 404")
  }

  // ===========================================================================
  // 6. Async + Try conversion inside Future
  // ===========================================================================

  test("Action.async with Future[Success[String]] returns the string") {
    val body = getBody("/async-try-success")
    assertNoDiff(body, "try-async-ok")
  }

  test("Action.async with Future[Failure] → 500") {
    val (status, _) = getWithStatus("/async-try-failure")
    assertEquals(status, 500, "Future(Failure) should → 500")
  }

  // ===========================================================================
  // 7. Async + Either conversion inside Future
  // ===========================================================================

  test("Action.async with Future[Right[String]] returns the string") {
    val body = getBody("/async-either-right")
    assertNoDiff(body, "either-async-ok")
  }

  test("Action.async with Future[Left[String]] → 400") {
    val (status, body) = getWithStatus("/async-either-left")
    assertEquals(status, 400, "Future(Left) should → 400")
    assert(body.contains("validation-async-failed"))
  }

  // ===========================================================================
  // 8. Async + noTimeout / timeout
  // ===========================================================================

  test("Action.noTimeout with Future returns the result") {
    val body = getBody("/async-no-timeout")
    assertNoDiff(body, "async-no-timeout-ok")
  }

  test("Action.timeout with Future returns the result") {
    val body = getBody("/async-timeout")
    assertNoDiff(body, "async-timeout-ok")
  }

  // ===========================================================================
  // 9. Sync actions still work
  // ===========================================================================

  test("Sync Action returning String still works") {
    val body = getBody("/sync-string")
    assertNoDiff(body, "sync-ok")
  }

  test("Sync Action returning Result still works") {
    val body = getBody("/sync-result")
    assertNoDiff(body, "sync-result-ok")
  }

  // ===========================================================================
  // 10. Async with simulated I/O delay
  // ===========================================================================

  test("Action.async with delayed Future returns successfully") {
    val body = getBody("/async-delayed")
    assertNoDiff(body, "delayed-ok")
  }

end AsyncActionSuite