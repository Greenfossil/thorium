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

import com.linecorp.armeria.common.{HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.time.Duration
import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

type SimpleResponse = String | Array[Byte] | InputStream | HttpResponse

type ActionResponse = SimpleResponse | Result

/**
 * The return type of an action block. An action may return either:
 *   - a synchronous `ActionResponse` (String, Result, JsValue via Conversion, etc.)
 *   - a `Future[ActionResponse]` for async operations
 *
 * The framework's `serve()` handles both:
 *   - Sync returns are converted to `HttpResponse` directly on the blocking pool.
 *   - Async returns (Future) are bridged to `CompletableFuture` and chained —
 *     the blocking-pool thread is freed immediately after construction.
 *
 * `Action.async` is a semantic marker — the pipeline handles both sync and async
 * returns identically. Users can write `Action.async { req => "hello" }` without
 * wrapping in `Future(...)`.
 */
type AsyncActionResponse = ActionResponse | Future[ActionResponse]

private[thorium] val actionLogger = LoggerFactory.getLogger("com.greenfossil.thorium.action")

trait EssentialAction extends HttpService :

  /**
   * An EssentialAction is a `Request => AsyncActionResponse`.
   * All subclasses must implement this function signature.
   *
   * This method will be invoked by [[EssentialAction.serve]].
   *
   * @param request
   * @return
   */
  protected def apply(request: Request): AsyncActionResponse

  /**
   * Armeria invocation during an incoming request.
   *
   * Pipeline (stays in Java CompletableFuture at the low level):
   *   1. `aggregate()` — Armeria aggregates the request body on the event loop.
   *   2. `thenAccept` — on the event loop: construct `Request`, then hop to the
   *      blocking-pool executor to run `apply(req)`.
   *   3. `apply(req)` returns `AsyncActionResponse`:
   *      - Sync `ActionResponse` → convert to `HttpResponse` directly, complete
   *        the Armeria `CompletableFuture` on the blocking pool.
   *      - `Future[ActionResponse]` → bridge to `CompletableFuture` using the
   *        blocking-pool `ExecutionContext` (not `parasitic`), chain via
   *        `thenAccept` to convert + complete. The blocking-pool thread is freed
   *        immediately after the bridge is registered.
   *
   * No `ExecutionContext.parasitic` is used — completion callbacks run on the
   * blocking-pool executor, which is a proper bounded thread pool, not the
   * completing thread. This avoids thread starvation and stack-overflow risks.
   *
   * @param svcRequestContext
   * @param httpRequest
   * @return
   */
  override def serve(svcRequestContext: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    actionLogger.debug(s"Processing EssentialAction.serve - method:${svcRequestContext.method()}, content-type:${httpRequest.contentType()}, path:${httpRequest.uri}")
    val futureResp = new CompletableFuture[HttpResponse]()

    svcRequestContext
      .request()
      .aggregate()
      .thenAccept { aggregateRequest =>
        val req = new Request(svcRequestContext, aggregateRequest) {}
        val blockingEC = ExecutionContext.fromExecutorService(svcRequestContext.blockingTaskExecutor())

        svcRequestContext.blockingTaskExecutor().execute(() => {
          try
            actionLogger.debug(s"Invoke EssentialAction.apply. req:${req.hashCode()}")
            val resp = apply(req)
            actionLogger.debug("Response from EssentialAction.apply")
            resp match
              case f: Future[ActionResponse] @unchecked =>
                f.onComplete {
                  case Success(ar) =>
                    futureResp.complete(
                      HttpResponseConverter.convertActionResponseToHttpResponse(req, ar)
                    )
                  case Failure(ex) =>
                    actionLogger.debug(s"Async action Future failed.", ex)
                    futureResp.complete(HttpResponse.ofFailure(ex))
                }(using blockingEC)
              case ar: ActionResponse =>
                futureResp.complete(
                  HttpResponseConverter.convertActionResponseToHttpResponse(req, ar)
                )
          catch
            case t =>
              actionLogger.debug(s"Exception raised in EssentialAction.apply.", t)
              futureResp.complete(HttpResponse.ofFailure(t))
        })
      }
    // Close all resources registered via req.manageResource after the response
    // is fully written to the client. For InputStream returns, the actual
    // streaming (transferTo) happens AFTER futureResp completes, so we must
    // hook into the HttpResponse's completion future, not futureResp's.
    val resourceScope = svcRequestContext
    futureResp.whenComplete { (httpResp, ex) =>
      if httpResp != null then
        // HttpResponse.whenComplete() returns a CompletableFuture[Void] that
        // completes after all bytes are written to the client (or on error).
        val responseDone = httpResp.whenComplete()
        responseDone.thenRun(() => closeManagedResources(resourceScope))
      else
        // futureResp completed exceptionally — no HttpResponse to stream
        closeManagedResources(resourceScope)
    }
    HttpResponse.of(futureResp)

  /**
   * Closes all resources registered via `req.manageResource` on the given
   * `ServiceRequestContext`. Resources are closed in LIFO (reverse
   * registration) order. Exceptions during close are swallowed.
   */
  private def closeManagedResources(ctx: ServiceRequestContext): Unit =
    val list = ctx.attr(RequestAttrs.ManagedResources)
    if list != null then
      val it = list.iterator()
      val toClose = new java.util.ArrayList[AutoCloseable]()
      while it.hasNext do toClose.add(it.next())
      var i = toClose.size() - 1
      while i >= 0 do
        try toClose.get(i).close() catch case _: Throwable => ()
        i -= 1

end EssentialAction

trait Action extends EssentialAction

object Action:

  /**
   * Creates a synchronous action. The action body may return any `AsyncActionResponse`
   * (including `Future[ActionResponse]` — the pipeline handles both).
   *
   * @param fn the action body
   * @return an [[Action]]
   */
  def apply(fn: Request => AsyncActionResponse): Action =
    actionLogger.debug(s"Processing Action...")
    (request: Request) => fn(request)

  /**
   * Multipart form request. The action body receives a [[MultipartRequest]]
   * and returns an `ActionResponse`.
   *
   * @param fn the action body
   * @return an [[Action]]
   */
  def multipart(fn: MultipartRequest => ActionResponse): Action =
    actionLogger.debug("Processing Multipart Action...")
    (request: Request) => request.asMultipartFormData { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }

  /**
   * Clears the request timeout for this action, allowing long-running
   * operations (e.g. LLM streaming, bulk DB operations) to complete without
   * Armeria's default request timeout interrupting.
   *
   * Use sparingly — only for endpoints that genuinely need unlimited time.
   * Prefer [[Action.timeout]] with an explicit duration where possible.
   *
   * @param fn the action body
   * @return an [[Action]] with no request timeout
   */
  def noTimeout(fn: Request => AsyncActionResponse): Action =
    (request: Request) =>
      request.requestContext.clearRequestTimeout()
      fn(request)

  /**
   * Sets an explicit request timeout for this action, overriding the server's
   * default. Useful for endpoints that need more (or less) time than the
   * global default.
   *
   * @param duration the maximum time the request is allowed to run
   * @param fn the action body
   * @return an [[Action]] with the specified timeout
   */
  def timeout(duration: Duration)(fn: Request => AsyncActionResponse): Action =
    (request: Request) =>
      request.requestContext.setRequestTimeout(duration)
      fn(request)

  /**
   * Creates an async action. The action body may return:
   *   - A `Future[ActionResponse]` for genuinely async work (DB calls, HTTP requests, etc.)
   *   - A synchronous `ActionResponse` (String, Result, Option, Try, Either, JsValue, etc.)
   *     — the framework handles it without wrapping in `Future(...)`.
   *
   * This is a semantic marker — the pipeline handles both sync and async returns
   * identically. The user does not need to wrap sync returns in `Future(...)`.
   *
   * The user's `Future` body runs on the user's own `ExecutionContext` (typically
   * `given ExecutionContext = ExecutionContext.global` declared at the controller level).
   * The framework does not require or capture an `ExecutionContext` — completion
   * is wired via the blocking-pool executor inside `serve()`.
   *
   * Example:
   * {{{
   * given ExecutionContext = ExecutionContext.global
   *
   * @Get("/search")
   * def search: Action = Action.async { req =>
   *   db.findUser(req.queryParam("q")).map(Ok(_))   // Future[Result]
   * }
   *
   * @Get("/sync-in-async")
   * def syncInAsync: Action = Action.async { req =>
   *   "hello"   // String — handled directly, no Future wrapping
   * }
   * }}}
   *
   * @param fn the action body returning `AsyncActionResponse`
   * @return an [[Action]]
   */
  def async(fn: Request => AsyncActionResponse): Action =
    actionLogger.debug(s"Processing Async Action...")
    (request: Request) => fn(request)

  /**
   * Multipart form request with async support. The action body may return
   * sync `ActionResponse` or `Future[ActionResponse]`. Uses
   * `asMultipartFormDataAsync` — non-blocking, returns a `Future`.
   *
   * @param fn the action body returning `AsyncActionResponse`
   * @return an [[Action]]
   */
  def multipartAsync(fn: MultipartRequest => AsyncActionResponse): Action =
    actionLogger.debug("Processing Multipart Async Action...")
    (request: Request) => request.asMultipartFormDataAsync { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }