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
 * Tests for the AsyncAction type hierarchy.
 *
 * Tests cover:
 *   - AsyncAction.apply: sync returns (String, Result, Option, Try, Either, JsValue, etc.)
 *   - AsyncAction.apply: Future returns (Future[ActionResponse])
 *   - AsyncAction.multipart: multipart form handling
 *   - AsyncAction.noTimeout: long-running operations
 *   - AsyncAction.timeout: explicit timeout
 *   - Action (sync): sync-only factory
 *   - Action.noTimeout: sync long-running
 *   - Action.timeout: sync explicit timeout
 *   - Type hierarchy: Action <: AsyncAction
 */
object AsyncActionServices:
  given ExecutionContext = ExecutionContext.global

  // ===========================================================================
  // AsyncAction with sync returns (no Future wrapping needed)
  // ===========================================================================

  @Get("/async-string-sync")
  def asyncStringSync: AsyncAction = AsyncAction { _ =>
    "hello-async"                        // String → 200 OK
  }

  @Get("/async-result-sync")
  def asyncResultSync: AsyncAction = AsyncAction { _ =>
    Ok("async-result-ok")               // Result → 200 OK
  }

  @Get("/async-redirect-sync")
  def asyncRedirectSync: AsyncAction = AsyncAction { _ =>
    Redirect("/new-path")               // Result → 303 Redirect
  }

  @Get("/async-accept-sync")
  def asyncAcceptSync: AsyncAction = AsyncAction { _ =>
    Accepted("job submitted")           // Result → 202 Accepted
  }

  @Get("/async-service-unavailable-sync")
  def asyncServiceUnavailableSync: AsyncAction = AsyncAction { _ =>
    ServiceUnavailable("temporarily down") // Result → 503
  }

  @Get("/async-option-some-sync")
  def asyncOptionSomeSync: AsyncAction = AsyncAction { _ =>
    Some("found-async")                 // Option[String] → 200 OK
  }

  @Get("/async-option-none-sync")
  def asyncOptionNoneSync: AsyncAction = AsyncAction { _ =>
    None: Option[String]                // Option[String] → 404
  }

  @Get("/async-try-success-sync")
  def asyncTrySuccessSync: AsyncAction = AsyncAction { _ =>
    Success("try-async-ok")            // Try[String] → 200 OK
  }

  @Get("/async-try-failure-sync")
  def asyncTryFailureSync: AsyncAction = AsyncAction { _ =>
    Failure(new RuntimeException("try-async-boom"))  // Try → re-throw → 500
  }

  @Get("/async-either-right-sync")
  def asyncEitherRightSync: AsyncAction = AsyncAction { _ =>
    Right("either-async-ok")           // Either[String, String] → 200 OK
  }

  @Get("/async-either-left-sync")
  def asyncEitherLeftSync: AsyncAction = AsyncAction { _ =>
    Left("validation-async-failed")    // Either[String, String] → 400
  }

  @Get("/async-json-sync")
  def asyncJsonSync: AsyncAction = AsyncAction { _ =>
    Json.obj("status" -> "ok")         // JsValue → 200 JSON
  }

  // --- Resultable extension in async ---

  @Get("/async-resultable-as")
  def asyncResultableAs: AsyncAction = AsyncAction { _ =>
    "Bad Request 2".as(HttpStatus.BAD_REQUEST, MediaType.JSON)  // String → Result via Resultable
  }

  @Get("/async-resultable-withheaders")
  def asyncResultableWithHeaders: AsyncAction = AsyncAction { _ =>
    "plain text".withHeaders("X-Custom" -> "yes")  // String → Result via Resultable
  }

  // ===========================================================================
  // AsyncAction with Future returns
  // ===========================================================================

  @Get("/async-future-result")
  def asyncFutureResult: AsyncAction = AsyncAction { _ =>
    Future(Ok("async-future-ok"))        // Future[Result] → 200
  }

  @Get("/async-future-string")
  def asyncFutureString: AsyncAction = AsyncAction { _ =>
    Future("hello-future")              // Future[String] → 200
  }

  @Get("/async-future-json")
  def asyncFutureJson: AsyncAction = AsyncAction { _ =>
    Future(Json.obj("status" -> "ok"))  // Future[JsValue] → 200 JSON
  }

  @Get("/async-future-failure")
  def asyncFutureFailure: AsyncAction = AsyncAction { _ =>
    Future.failed(new RuntimeException("async-boom"))  // Future.failed → 500
  }

  @Get("/async-future-option-some")
  def asyncFutureOptionSome: AsyncAction = AsyncAction { _ =>
    Future(Some("found-future"))        // Future[Some] → 200
  }

  @Get("/async-future-option-none")
  def asyncFutureOptionNone: AsyncAction = AsyncAction { _ =>
    Future(None: Option[String])        // Future[None] → 404
  }

  @Get("/async-future-try-success")
  def asyncFutureTrySuccess: AsyncAction = AsyncAction { _ =>
    Future(Success("try-future-ok"))   // Future[Success] → 200
  }

  @Get("/async-future-try-failure")
  def asyncFutureTryFailure: AsyncAction = AsyncAction { _ =>
    Future(Failure(new RuntimeException("try-future-boom")))  // Future[Failure] → 500
  }

  @Get("/async-future-either-right")
  def asyncFutureEitherRight: AsyncAction = AsyncAction { _ =>
    Future(Right("either-future-ok"))  // Future[Right] → 200
  }

  @Get("/async-future-either-left")
  def asyncFutureEitherLeft: AsyncAction = AsyncAction { _ =>
    Future(Left("validation-future-failed"))  // Future[Left] → 400
  }

  // ===========================================================================
  // Action (sync factory) — must return ActionResponse only
  // ===========================================================================

  @Get("/sync-factory-string")
  def syncFactoryString: Action = Action { _ =>
    "sync-factory-string-ok"             // String from sync factory
  }

  @Get("/sync-factory-result")
  def syncFactoryResult: Action = Action { _ =>
    Ok("sync-factory-result-ok")         // Result from sync factory
  }

  // ===========================================================================
  // AsyncAction.noTimeout / timeout
  // ===========================================================================

  @Get("/async-no-timeout-future")
  def asyncNoTimeoutFuture: AsyncAction = AsyncAction.noTimeout { req =>
    Future(Ok("async-no-timeout-future-ok"))
  }

  @Get("/async-no-timeout-sync")
  def asyncNoTimeoutSync: AsyncAction = AsyncAction.noTimeout { req =>
    "async-no-timeout-sync-ok"
  }

  @Get("/async-timeout-future")
  def asyncTimeoutFuture: AsyncAction = AsyncAction.timeout(Duration.ofSeconds(30)) { req =>
    Future(Ok("async-timeout-future-ok"))
  }

  @Get("/async-timeout-sync")
  def asyncTimeoutSync: AsyncAction = AsyncAction.timeout(Duration.ofSeconds(30)) { req =>
    "async-timeout-sync-ok"
  }

  // ===========================================================================
  // Action.noTimeout / timeout (sync-only)
  // ===========================================================================

  @Get("/sync-no-timeout")
  def syncNoTimeout: Action = Action.noTimeout { req =>
    "sync-no-timeout-ok"
  }

  @Get("/sync-timeout")
  def syncTimeout: Action = Action.timeout(Duration.ofSeconds(30)) { req =>
    "sync-timeout-ok"
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
  def asyncDelayed: AsyncAction = AsyncAction { _ =>
    Future {
      Thread.sleep(100) // simulate I/O
      Ok("delayed-ok")
    }
  }

  // ===========================================================================
  // AsyncAction.multipart — async multipart with Future returns
  // ===========================================================================

  @Post("/multipart-async-sync-return")
  def multipartAsyncSyncReturn: AsyncAction = AsyncAction.multipart { mpReq =>
    // Sync return (String) from multipart — wrapped in Future.successful internally
    s"files:${mpReq.multipartFormData.names.size}"
  }

  @Post("/multipart-async-future-return")
  def multipartAsyncFutureReturn: AsyncAction = AsyncAction.multipart { mpReq =>
    // Future return — non-blocking
    Future(s"async-files:${mpReq.multipartFormData.names.size}")
  }

  @Post("/multipart-async-result")
  def multipartAsyncResult: AsyncAction = AsyncAction.multipart { mpReq =>
    Ok(s"result-files:${mpReq.multipartFormData.names.size}")
  }

  @Post("/multipart-async-find-files")
  def multipartAsyncFindFiles: AsyncAction = AsyncAction.multipart { mpReq =>
    mpReq.findFiles((_, _, _, _) => true)
      .map(files => Ok(s"found:${files.size}"))
      .getOrElse(BadRequest("no files"))
  }

  @Post("/multipart-async-failure")
  def multipartAsyncFailure: AsyncAction = AsyncAction.multipart { _ =>
    Future.failed(new RuntimeException("multipart-async-boom"))
  }

  // ===========================================================================
  // Action.multipart (sync)
  // ===========================================================================

  @Post("/multipart-sync-return")
  def multipartSyncReturn: Action = Action.multipart { mpReq =>
    s"sync-files:${mpReq.multipartFormData.names.size}"
  }

  // ===========================================================================
  // AsyncAction.noTimeout.multipart / timeout.multipart
  // ===========================================================================

  @Post("/multipart-no-timeout-async")
  def multipartNoTimeoutAsync: AsyncAction = AsyncAction.noTimeout.multipart { mpReq =>
    Future(s"no-timeout-async-files:${mpReq.multipartFormData.names.size}")
  }

  @Post("/multipart-timeout-async")
  def multipartTimeoutAsync: AsyncAction = AsyncAction.timeout(Duration.ofSeconds(30)).multipart { mpReq =>
    s"timeout-async-files:${mpReq.multipartFormData.names.size}"
  }

  // ===========================================================================
  // Action.noTimeout.multipart / timeout.multipart (sync)
  // ===========================================================================

  @Post("/multipart-no-timeout-sync")
  def multipartNoTimeoutSync: Action = Action.noTimeout.multipart { mpReq =>
    s"no-timeout-sync-files:${mpReq.multipartFormData.names.size}"
  }

  @Post("/multipart-timeout-sync")
  def multipartTimeoutSync: Action = Action.timeout(Duration.ofSeconds(30)).multipart { mpReq =>
    s"timeout-sync-files:${mpReq.multipartFormData.names.size}"
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
  // 1. AsyncAction with sync returns — no Future wrapping needed
  // ===========================================================================

  test("AsyncAction with String return → 200 OK") {
    val body = getBody("/async-string-sync")
    assertNoDiff(body, "hello-async")
  }

  test("AsyncAction with Ok Result return → 200 OK") {
    val body = getBody("/async-result-sync")
    assertNoDiff(body, "async-result-ok")
  }

  test("AsyncAction with Redirect return → 303") {
    val resp = get("/async-redirect-sync")
    assertEquals(resp.statusCode(), 303, "Redirect should → 303")
    assertEquals(resp.headers().firstValue("location").orElse(""), "/new-path")
  }

  test("AsyncAction with Accept return → 202") {
    val resp = get("/async-accept-sync")
    assertEquals(resp.statusCode(), 202, "Accept should → 202")
    assert(resp.body().contains("job submitted"))
  }

  test("AsyncAction with ServiceUnavailable return → 503") {
    val resp = get("/async-service-unavailable-sync")
    assertEquals(resp.statusCode(), 503, "ServiceUnavailable should → 503")
  }

  test("AsyncAction with Some return → 200") {
    val body = getBody("/async-option-some-sync")
    assertNoDiff(body, "found-async")
  }

  test("AsyncAction with None return → 404") {
    val (status, _) = getWithStatus("/async-option-none-sync")
    assertEquals(status, 404, "None should → 404")
  }

  test("AsyncAction with Success return → 200") {
    val body = getBody("/async-try-success-sync")
    assertNoDiff(body, "try-async-ok")
  }

  test("AsyncAction with Failure return → 500") {
    val (status, _) = getWithStatus("/async-try-failure-sync")
    assertEquals(status, 500, "Failure should re-throw → 500")
  }

  test("AsyncAction with Right return → 200") {
    val body = getBody("/async-either-right-sync")
    assertNoDiff(body, "either-async-ok")
  }

  test("AsyncAction with Left return → 400") {
    val (status, body) = getWithStatus("/async-either-left-sync")
    assertEquals(status, 400, "Left should → 400")
    assert(body.contains("validation-async-failed"))
  }

  test("AsyncAction with JsValue return → 200 JSON") {
    val resp = get("/async-json-sync")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"status\":\"ok\""))
  }

  // ===========================================================================
  // 2. Resultable extensions in AsyncAction
  // ===========================================================================

  test("AsyncAction with .as(status, contentType) → 400 JSON") {
    val resp = get("/async-resultable-as")
    assertEquals(resp.statusCode(), 400, ".as(BAD_REQUEST, JSON) should → 400")
    val ct = resp.headers().firstValue("content-type").orElse("")
    assert(ct.contains("application/json"), s"Expected JSON content-type, got: $ct")
    assert(resp.body().contains("Bad Request 2"))
  }

  test("AsyncAction with .withHeaders → 200 with custom header") {
    val resp = get("/async-resultable-withheaders")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assertEquals(resp.headers().firstValue("x-custom").orElse(""), "yes")
    assertNoDiff(resp.body(), "plain text")
  }

  // ===========================================================================
  // 3. AsyncAction with Future returns
  // ===========================================================================

  test("AsyncAction with Future[Result] → 200") {
    val body = getBody("/async-future-result")
    assertNoDiff(body, "async-future-ok")
  }

  test("AsyncAction with Future[String] → 200") {
    val body = getBody("/async-future-string")
    assertNoDiff(body, "hello-future")
  }

  test("AsyncAction with Future[JsValue] → 200 JSON") {
    val resp = get("/async-future-json")
    assertEquals(resp.statusCode(), 200, "Expected 200")
    assert(resp.body().contains("\"status\":\"ok\""))
  }

  test("AsyncAction with Future.failed → 500") {
    val (status, _) = getWithStatus("/async-future-failure")
    assertEquals(status, 500, "Future.failed should → 500")
  }

  test("AsyncAction with Future[Some] → 200") {
    val body = getBody("/async-future-option-some")
    assertNoDiff(body, "found-future")
  }

  test("AsyncAction with Future[None] → 404") {
    val (status, _) = getWithStatus("/async-future-option-none")
    assertEquals(status, 404, "Future(None) should → 404")
  }

  test("AsyncAction with Future[Success] → 200") {
    val body = getBody("/async-future-try-success")
    assertNoDiff(body, "try-future-ok")
  }

  test("AsyncAction with Future[Failure] → 500") {
    val (status, _) = getWithStatus("/async-future-try-failure")
    assertEquals(status, 500, "Future(Failure) should → 500")
  }

  test("AsyncAction with Future[Right] → 200") {
    val body = getBody("/async-future-either-right")
    assertNoDiff(body, "either-future-ok")
  }

  test("AsyncAction with Future[Left] → 400") {
    val (status, body) = getWithStatus("/async-future-either-left")
    assertEquals(status, 400, "Future(Left) should → 400")
    assert(body.contains("validation-future-failed"))
  }

  // ===========================================================================
  // 4. Action (sync factory) returning ActionResponse
  // ===========================================================================

  test("Action (sync factory) returning String → 200") {
    val body = getBody("/sync-factory-string")
    assertNoDiff(body, "sync-factory-string-ok")
  }

  test("Action (sync factory) returning Result → 200") {
    val body = getBody("/sync-factory-result")
    assertNoDiff(body, "sync-factory-result-ok")
  }

  // ===========================================================================
  // 5. AsyncAction.noTimeout / timeout
  // ===========================================================================

  test("AsyncAction.noTimeout with Future return → 200") {
    val body = getBody("/async-no-timeout-future")
    assertNoDiff(body, "async-no-timeout-future-ok")
  }

  test("AsyncAction.noTimeout with sync return → 200") {
    val body = getBody("/async-no-timeout-sync")
    assertNoDiff(body, "async-no-timeout-sync-ok")
  }

  test("AsyncAction.timeout with Future return → 200") {
    val body = getBody("/async-timeout-future")
    assertNoDiff(body, "async-timeout-future-ok")
  }

  test("AsyncAction.timeout with sync return → 200") {
    val body = getBody("/async-timeout-sync")
    assertNoDiff(body, "async-timeout-sync-ok")
  }

  // ===========================================================================
  // 6. Action.noTimeout / timeout (sync)
  // ===========================================================================

  test("Action.noTimeout with sync return → 200") {
    val body = getBody("/sync-no-timeout")
    assertNoDiff(body, "sync-no-timeout-ok")
  }

  test("Action.timeout with sync return → 200") {
    val body = getBody("/sync-timeout")
    assertNoDiff(body, "sync-timeout-ok")
  }

  // ===========================================================================
  // 7. Sync actions still work
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
  // 8. Async with simulated I/O delay
  // ===========================================================================

  test("AsyncAction with delayed Future → 200") {
    val body = getBody("/async-delayed")
    assertNoDiff(body, "delayed-ok")
  }

  // ===========================================================================
  // 9. AsyncAction.multipart — async multipart with Future returns
  // ===========================================================================

  test("AsyncAction.multipart with sync String return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-async-sync-return", mpPub)
    assert(body.contains("files:1"), s"Expected files:1, got: $body")
  }

  test("AsyncAction.multipart with Future return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-async-future-return", mpPub)
    assert(body.contains("async-files:1"), s"Expected async-files:1, got: $body")
  }

  test("AsyncAction.multipart with Ok Result return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-async-result", mpPub)
    assert(body.contains("result-files:1"), s"Expected result-files:1, got: $body")
  }

  test("AsyncAction.multipart with findFiles → 200") {
    Files.write(Paths.get("/tmp/async-multipart-test.txt"), "test data".getBytes(StandardCharsets.UTF_8))
    val mpPub = MultipartFormDataBodyPublisher()
      .addFile("resourceFile", Paths.get("/tmp/async-multipart-test.txt"), "text/plain")
    val body = postMultipartBody("/multipart-async-find-files", mpPub)
    assert(body.contains("found:1"), s"Expected found:1, got: $body")
  }

  test("AsyncAction.multipart with Future.failed → 500") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val (status, _) = postMultipartWithStatus("/multipart-async-failure", mpPub)
    assertEquals(status, 500, "Future.failed should → 500")
  }

  // ===========================================================================
  // 10. Action.multipart (sync)
  // ===========================================================================

  test("Action.multipart with sync String return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-sync-return", mpPub)
    assert(body.contains("sync-files:1"), s"Expected sync-files:1, got: $body")
  }

  // ===========================================================================
  // 11. Type hierarchy
  // ===========================================================================

  test("Action is a subtype of AsyncAction") {
    val action: AsyncAction = AsyncActionServices.syncString
    assert(action != null)
  }

  // ===========================================================================
  // 12. AsyncAction.noTimeout.multipart / timeout.multipart
  // ===========================================================================

  test("AsyncAction.noTimeout.multipart with Future return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-no-timeout-async", mpPub)
    assert(body.contains("no-timeout-async-files:1"), s"Expected no-timeout-async-files:1, got: $body")
  }

  test("AsyncAction.timeout.multipart with sync return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-timeout-async", mpPub)
    assert(body.contains("timeout-async-files:1"), s"Expected timeout-async-files:1, got: $body")
  }

  // ===========================================================================
  // 13. Action.noTimeout.multipart / timeout.multipart (sync)
  // ===========================================================================

  test("Action.noTimeout.multipart with sync return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-no-timeout-sync", mpPub)
    assert(body.contains("no-timeout-sync-files:1"), s"Expected no-timeout-sync-files:1, got: $body")
  }

  test("Action.timeout.multipart with sync return → 200") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val body = postMultipartBody("/multipart-timeout-sync", mpPub)
    assert(body.contains("timeout-sync-files:1"), s"Expected timeout-sync-files:1, got: $body")
  }

end AsyncActionSuite
