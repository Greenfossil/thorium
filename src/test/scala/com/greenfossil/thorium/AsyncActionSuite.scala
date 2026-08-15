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
import com.linecorp.armeria.common.{HttpStatus, MediaType}
import com.linecorp.armeria.server.annotation.{Get, Post}
import io.github.yskszk63.jnhttpmultipartformdatabodypublisher.MultipartFormDataBodyPublisher

import java.net.{URI, http}
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import java.time.Duration
import scala.compiletime.uninitialized
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.language.implicitConversions

/**
 * Tests for the unified serve() pipeline — Action.async and Future[ActionResponse] support.
 *
 * The unified pipeline uses `Future(apply(req)).flatMap` to handle both sync and async:
 *   - Sync returns (String, Result, Option, Try, Either, JsValue, etc.) are wrapped in
 *     `Future.successful` — zero scheduling overhead.
 *   - Async returns (Future[ActionResponse]) are chained directly — the blocking-pool
 *     thread is freed immediately after construction.
 *
 * Key ergonomics tested:
 *   - `Action.async { req => "hello" }` — sync return in async factory (no Future wrapping needed)
 *   - `Action.async { req => Ok("hello") }` — Result return
 *   - `Action.async { req => Redirect("/x") }` — redirect
 *   - `Action.async { req => Some("found") }` — Option conversion
 *   - `Action.async { req => "text".as(400, MediaType.JSON) }` — Resultable extension
 *   - `Action.async { req => Future(Ok("hello")) }` — Future return
 *   - `Action { req => Future(Ok("hello")) }` — Future return in sync factory (also works)
 *   - `Action.noTimeout { req => Future(Ok("hello")) }` — Future + noTimeout
 */
object AsyncActionServices:
  given ExecutionContext = ExecutionContext.global

  // ===========================================================================
  // Action.async with sync returns (no Future wrapping needed)
  // ===========================================================================

  @Get("/async-string-sync")
  def asyncStringSync: Action = Action.async { _ =>
    "hello-async"                        // String → 200 OK
  }

  @Get("/async-result-sync")
  def asyncResultSync: Action = Action.async { _ =>
    Ok("async-result-ok")               // Result → 200 OK
  }

  @Get("/async-redirect-sync")
  def asyncRedirectSync: Action = Action.async { _ =>
    Redirect("/new-path")               // Result → 303 Redirect
  }

  @Get("/async-accept-sync")
  def asyncAcceptSync: Action = Action.async { _ =>
    Accepted("job submitted")           // Result → 202 Accepted
  }

  @Get("/async-service-unavailable-sync")
  def asyncServiceUnavailableSync: Action = Action.async { _ =>
    ServiceUnavailable("temporarily down") // Result → 503
  }

  @Get("/async-option-some-sync")
  def asyncOptionSomeSync: Action = Action.async { _ =>
    Some("found-async")                 // Option[String] → 200 OK
  }

  @Get("/async-option-none-sync")
  def asyncOptionNoneSync: Action = Action.async { _ =>
    None: Option[String]                // Option[String] → 404
  }

  @Get("/async-try-success-sync")
  def asyncTrySuccessSync: Action = Action.async { _ =>
    Success("try-async-ok")            // Try[String] → 200 OK
  }

  @Get("/async-try-failure-sync")
  def asyncTryFailureSync: Action = Action.async { _ =>
    Failure(new RuntimeException("try-async-boom"))  // Try → re-throw → 500
  }

  @Get("/async-either-right-sync")
  def asyncEitherRightSync: Action = Action.async { _ =>
    Right("either-async-ok")           // Either[String, String] → 200 OK
  }

  @Get("/async-either-left-sync")
  def asyncEitherLeftSync: Action = Action.async { _ =>
    Left("validation-async-failed")    // Either[String, String] → 400
  }

  @Get("/async-json-sync")
  def asyncJsonSync: Action = Action.async { _ =>
    Json.obj("status" -> "ok")         // JsValue → 200 JSON
  }

  // --- Resultable extension in async ---

  @Get("/async-resultable-as")
  def asyncResultableAs: Action = Action.async { _ =>
    "Bad Request 2".as(HttpStatus.BAD_REQUEST, MediaType.JSON)  // String → Result via Resultable
  }

  @Get("/async-resultable-withheaders")
  def asyncResultableWithHeaders: Action = Action.async { _ =>
    "plain text".withHeaders("X-Custom" -> "yes")  // String → Result via Resultable
  }

  // ===========================================================================
  // Action.async with Future returns
  // ===========================================================================

  @Get("/async-future-result")
  def asyncFutureResult: Action = Action.async { _ =>
    Future(Ok("async-future-ok"))        // Future[Result] → 200
  }

  @Get("/async-future-string")
  def asyncFutureString: Action = Action.async { _ =>
    Future("hello-future")              // Future[String] → 200
  }

  @Get("/async-future-json")
  def asyncFutureJson: Action = Action.async { _ =>
    Future(Json.obj("status" -> "ok"))  // Future[JsValue] → 200 JSON
  }

  @Get("/async-future-failure")
  def asyncFutureFailure: Action = Action.async { _ =>
    Future.failed(new RuntimeException("async-boom"))  // Future.failed → 500
  }

  @Get("/async-future-option-some")
  def asyncFutureOptionSome: Action = Action.async { _ =>
    Future(Some("found-future"))        // Future[Some] → 200
  }

  @Get("/async-future-option-none")
  def asyncFutureOptionNone: Action = Action.async { _ =>
    Future(None: Option[String])        // Future[None] → 404
  }

  @Get("/async-future-try-success")
  def asyncFutureTrySuccess: Action = Action.async { _ =>
    Future(Success("try-future-ok"))   // Future[Success] → 200
  }

  @Get("/async-future-try-failure")
  def asyncFutureTryFailure: Action = Action.async { _ =>
    Future(Failure(new RuntimeException("try-future-boom")))  // Future[Failure] → 500
  }

  @Get("/async-future-either-right")
  def asyncFutureEitherRight: Action = Action.async { _ =>
    Future(Right("either-future-ok"))  // Future[Right] → 200
  }

  @Get("/async-future-either-left")
  def asyncFutureEitherLeft: Action = Action.async { _ =>
    Future(Left("validation-future-failed"))  // Future[Left] → 400
  }

  // ===========================================================================
  // Action (sync factory) returning Future — also works via unified pipeline
  // ===========================================================================

  @Get("/sync-factory-future")
  def syncFactoryFuture: Action = Action { _ =>
    Future(Ok("sync-factory-future-ok"))  // Future[Result] from sync factory
  }

  @Get("/sync-factory-string")
  def syncFactoryString: Action = Action { _ =>
    "sync-factory-string-ok"             // String from sync factory
  }

  // ===========================================================================
  // Action.async + noTimeout / timeout
  // ===========================================================================

  @Get("/async-no-timeout-future")
  def asyncNoTimeoutFuture: Action = Action.noTimeout { req =>
    Future(Ok("async-no-timeout-future-ok"))
  }

  @Get("/async-no-timeout-sync")
  def asyncNoTimeoutSync: Action = Action.noTimeout { req =>
    "async-no-timeout-sync-ok"
  }

  @Get("/async-timeout-future")
  def asyncTimeoutFuture: Action = Action.timeout(Duration.ofSeconds(30)) { req =>
    Future(Ok("async-timeout-future-ok"))
  }

  @Get("/async-timeout-sync")
  def asyncTimeoutSync: Action = Action.timeout(Duration.ofSeconds(30)) { req =>
    "async-timeout-sync-ok"
  }

  // ===========================================================================
  // Sync actions (must still work)
  // ===========================================================================

  @Get("/sync-string")
  def syncString: Action = Action { _ =>
    "sync-ok"
  }

  @Get("/sync-result")
  def syncResult: Action = Action { _ =>
    Ok("sync-result-ok")
  }

  @Get("/sync-option")
  def syncOption: Action = Action { _ =>
    Some("sync-option-ok")
  }

  @Get("/sync-try")
  def syncTry: Action = Action { _ =>
    Success("sync-try-ok")
  }

  @Get("/sync-either")
  def syncEither: Action = Action { _ =>
    Right("sync-either-ok")
  }

  // ===========================================================================
  // Async with simulated I/O delay
  // ===========================================================================

  @Get("/async-delayed")
  def asyncDelayed: Action = Action.async { _ =>
    Future {
      Thread.sleep(100) // simulate I/O
      Ok("delayed-ok")
    }
  }

  // ===========================================================================
  // Action.multipartAsync — async multipart with Future returns
  // ===========================================================================

  @Post("/multipart-async-sync-return")
  def multipartAsyncSyncReturn: Action = Action.multipartAsync { mpReq =>
    // Sync return (String) from multipartAsync — wrapped in Future.successful internally
    s"files:${mpReq.multipartFormData.names.size}"
  }

  @Post("/multipart-async-future-return")
  def multipartAsyncFutureReturn: Action = Action.multipartAsync { mpReq =>
    // Future return — non-blocking
    Future(s"async-files:${mpReq.multipartFormData.names.size}")
  }

  @Post("/multipart-async-result")
  def multipartAsyncResult: Action = Action.multipartAsync { mpReq =>
    Ok(s"result-files:${mpReq.multipartFormData.names.size}")
  }

  @Post("/multipart-async-find-files")
  def multipartAsyncFindFiles: Action = Action.multipartAsync { mpReq =>
    mpReq.findFiles((_, _, _, _) => true)
      .map(files => Ok(s"found:${files.size}"))
      .getOrElse(BadRequest("no files"))
  }

  @Post("/multipart-async-failure")
  def multipartAsyncFailure: Action = Action.multipartAsync { _ =>
    Future.failed(new RuntimeException("multipart-async-boom"))
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

  private def postMultipart(path: String, mpPub: MultipartFormDataBodyPublisher): http.HttpResponse[String] =
    http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}$path"))
        .POST(mpPub)
        .header("Content-Type", mpPub.contentType())
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )

  private def postMultipartBody(path: String, mpPub: MultipartFormDataBodyPublisher): String =
    val resp = postMultipart(path, mpPub)
    assertEquals(resp.statusCode(), 200, s"Expected 200 for $path but got ${resp.statusCode()}: ${resp.body()}")
    resp.body()

  private def postMultipartWithStatus(path: String, mpPub: MultipartFormDataBodyPublisher): (Int, String) =
    val resp = postMultipart(path, mpPub)
    (resp.statusCode(), resp.body())

  // ===========================================================================
  // 1. Action.async with sync returns — no Future wrapping needed
  // ===========================================================================

  test("Action.async with String return → 200 OK") {
    val body = getBody("/async-string-sync")
    assertNoDiff(body, "hello-async")
  }

  test("Action.async with Ok Result return → 200 OK") {
    val body = getBody("/async-result-sync")
    assertNoDiff(body, "async-result-ok")
  }

  test("Action.async with Redirect return → 303") {
    val resp = get("/async-redirect-sync")
    assertEquals(resp.statusCode(), 303, "Redirect should → 303")
    assertEquals(resp.headers().firstValue("location").orElse(""), "/new-path")
  }

  test("Action.async with Accept return → 202") {
    val resp = get("/async-accept-sync")
    assertEquals(resp.statusCode(), 202, "Accept should → 202")
    assert(resp.body().contains("job submitted"))
  }

  test("Action.async with ServiceUnavailable return → 503") {
    val resp = get("/async-service-unavailable-sync")
    assertEquals(resp.statusCode(), 503, "ServiceUnavailable should → 503")
  }

  test("Action.async with Some return → 200") {
    val body = getBody("/async-option-some-sync")
    assertNoDiff(body, "found-async")
  }

  test("Action.async with None return → 404") {
    val (status, _) = getWithStatus("/async-option-none-sync")
    assertEquals(status, 404, "None should → 404")
  }

  test("Action.async with Success return → 200") {
    val body = getBody("/async-try-success-sync")
    assertNoDiff(body, "try-async-ok")
  }

  test("Action.async with Failure return → 500") {
    val (status, _) = getWithStatus("/async-try-failure-sync")
    assertEquals(status, 500, "Failure should re-throw → 500")
  }

  test("Action.async with Right return → 200") {
    val body = getBody("/async-either-right-sync")
    assertNoDiff(body, "either-async-ok")
  }

  test("Action.async with Left return → 400") {
    val (status, body) = getWithStatus("/async-either-left-sync")
    assertEquals(status, 400, "Left should → 400")
    assert(body.contains("validation-async-failed"))
  }

  test("Action.async with JsValue return → 200 JSON") {
    val resp = get("/async-json-sync")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"status\":\"ok\""))
  }

  // ===========================================================================
  // 2. Resultable extensions in Action.async
  // ===========================================================================

  test("Action.async with .as(status, contentType) → 400 JSON") {
    val resp = get("/async-resultable-as")
    assertEquals(resp.statusCode(), 400, ".as(BAD_REQUEST, JSON) should → 400")
    val ct = resp.headers().firstValue("content-type").orElse("")
    assert(ct.contains("application/json"), s"Expected JSON content-type, got: $ct")
    assert(resp.body().contains("Bad Request 2"))
  }

  test("Action.async with .withHeaders → 200 with custom header") {
    val resp = get("/async-resultable-withheaders")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assertEquals(resp.headers().firstValue("x-custom").orElse(""), "yes")
    assertNoDiff(resp.body(), "plain text")
  }

  // ===========================================================================
  // 3. Action.async with Future returns
  // ===========================================================================

  test("Action.async with Future[Result] → 200") {
    val body = getBody("/async-future-result")
    assertNoDiff(body, "async-future-ok")
  }

  test("Action.async with Future[String] → 200") {
    val body = getBody("/async-future-string")
    assertNoDiff(body, "hello-future")
  }

  test("Action.async with Future[JsValue] → 200 JSON") {
    val resp = get("/async-future-json")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"status\":\"ok\""))
  }

  test("Action.async with Future.failed → 500") {
    val (status, _) = getWithStatus("/async-future-failure")
    assertEquals(status, 500, "Future.failed should → 500")
  }

  test("Action.async with Future[Some] → 200") {
    val body = getBody("/async-future-option-some")
    assertNoDiff(body, "found-future")
  }

  test("Action.async with Future[None] → 404") {
    val (status, _) = getWithStatus("/async-future-option-none")
    assertEquals(status, 404, "Future(None) should → 404")
  }

  test("Action.async with Future[Success] → 200") {
    val body = getBody("/async-future-try-success")
    assertNoDiff(body, "try-future-ok")
  }

  test("Action.async with Future[Failure] → 500") {
    val (status, _) = getWithStatus("/async-future-try-failure")
    assertEquals(status, 500, "Future(Failure) should → 500")
  }

  test("Action.async with Future[Right] → 200") {
    val body = getBody("/async-future-either-right")
    assertNoDiff(body, "either-future-ok")
  }

  test("Action.async with Future[Left] → 400") {
    val (status, body) = getWithStatus("/async-future-either-left")
    assertEquals(status, 400, "Future(Left) should → 400")
    assert(body.contains("validation-future-failed"))
  }

  // ===========================================================================
  // 4. Sync factory returning Future — also works via unified pipeline
  // ===========================================================================

  test("Action (sync factory) returning Future[Result] → 200") {
    val body = getBody("/sync-factory-future")
    assertNoDiff(body, "sync-factory-future-ok")
  }

  test("Action (sync factory) returning String → 200") {
    val body = getBody("/sync-factory-string")
    assertNoDiff(body, "sync-factory-string-ok")
  }

  // ===========================================================================
  // 5. Action.async + noTimeout / timeout (with both sync and Future returns)
  // ===========================================================================

  test("Action.noTimeout with Future return → 200") {
    val body = getBody("/async-no-timeout-future")
    assertNoDiff(body, "async-no-timeout-future-ok")
  }

  test("Action.noTimeout with sync return → 200") {
    val body = getBody("/async-no-timeout-sync")
    assertNoDiff(body, "async-no-timeout-sync-ok")
  }

  test("Action.timeout with Future return → 200") {
    val body = getBody("/async-timeout-future")
    assertNoDiff(body, "async-timeout-future-ok")
  }

  test("Action.timeout with sync return → 200") {
    val body = getBody("/async-timeout-sync")
    assertNoDiff(body, "async-timeout-sync-ok")
  }

  // ===========================================================================
  // 6. Sync actions still work
  // ===========================================================================

  test("Sync Action returning String → 200") {
    val body = getBody("/sync-string")
    assertNoDiff(body, "sync-ok")
  }

  test("Sync Action returning Result → 200") {
    val body = getBody("/sync-result")
    assertNoDiff(body, "sync-result-ok")
  }

  test("Sync Action returning Option → 200") {
    val body = getBody("/sync-option")
    assertNoDiff(body, "sync-option-ok")
  }

  test("Sync Action returning Try → 200") {
    val body = getBody("/sync-try")
    assertNoDiff(body, "sync-try-ok")
  }

  test("Sync Action returning Either → 200") {
    val body = getBody("/sync-either")
    assertNoDiff(body, "sync-either-ok")
  }

  // ===========================================================================
  // 7. Async with simulated I/O delay
  // ===========================================================================

  test("Action.async with delayed Future → 200") {
    val body = getBody("/async-delayed")
    assertNoDiff(body, "delayed-ok")
  }

  // ===========================================================================
  // 8. Action.multipartAsync — async multipart with Future returns
  // ===========================================================================

  test("Action.multipartAsync with sync String return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-async-sync-return", mpPub)
    assert(body.contains("files:1"), s"Expected files:1, got: $body")
  }

  test("Action.multipartAsync with Future return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-async-future-return", mpPub)
    assert(body.contains("async-files:1"), s"Expected async-files:1, got: $body")
  }

  test("Action.multipartAsync with Ok Result return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-async-result", mpPub)
    assert(body.contains("result-files:1"), s"Expected result-files:1, got: $body")
  }

  test("Action.multipartAsync with findFiles → 200") {
    Files.write(Paths.get("/tmp/async-multipart-test.txt"), "test data".getBytes(StandardCharsets.UTF_8))
    val mpPub = MultipartFormDataBodyPublisher()
      .addFile("resourceFile", Paths.get("/tmp/async-multipart-test.txt"), "text/plain")
    val body = postMultipartBody("/multipart-async-find-files", mpPub)
    assert(body.contains("found:1"), s"Expected found:1, got: $body")
  }

  test("Action.multipartAsync with Future.failed → 500") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val (status, _) = postMultipartWithStatus("/multipart-async-failure", mpPub)
    assertEquals(status, 500, "Future.failed should → 500")
  }

end AsyncActionSuite