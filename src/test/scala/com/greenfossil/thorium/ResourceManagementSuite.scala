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

import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.Get

import java.io.ByteArrayInputStream
import java.net.{URI, http}
import java.util.concurrent.atomic.AtomicBoolean
import scala.compiletime.uninitialized

/**
 * Tests for Request.manageResource — deferred resource release after
 * the HTTP response is fully written.
 */
object ResourceManagementServices:

  class TestResource(val name: String) extends AutoCloseable:
    val closed = AtomicBoolean(false)
    def close(): Unit = closed.set(true)

  /**
   * A test InputStream that FAILS if read after close — like a real DB-backed
   * stream. Used to verify that manageResource closes the stream only AFTER
   * streaming completes, not before.
   */
  class FailingAfterCloseInputStream(data: Array[Byte]) extends java.io.InputStream with AutoCloseable:
    private var pos = 0
    private var isClosed = false
    def read(): Int =
      if isClosed then throw new java.io.IOException("Stream already closed — read after close")
      if pos < data.length then
        val b = data(pos)
        pos += 1
        b & 0xFF
      else -1
    override def close(): Unit = isClosed = true

  /**
   * A slow InputStream that sleeps `delayMs` per byte read.
   * Fails if read after close — simulates a real DB-backed stream
   * that takes time to produce data and can't be read after the
   * connection is closed.
   *
   * This tests that manageResource keeps the resource open for the
   * ENTIRE streaming duration (not just until the HttpResponse object
   * is created), and closes it only after the last byte is sent.
   */
  class SlowFailingInputStream(data: Array[Byte], delayMs: Long = 200) extends java.io.InputStream with AutoCloseable:
    private var pos = 0
    private var isClosed = false
    private val readCount = java.util.concurrent.atomic.AtomicInteger(0)
    def read(): Int =
      if isClosed then throw new java.io.IOException("Stream closed — read after close")
      if pos < data.length then
        Thread.sleep(delayMs)
        readCount.incrementAndGet()
        val b = data(pos)
        pos += 1
        b & 0xFF
      else -1
    override def close(): Unit = isClosed = true

  @Get("/resource-sync-string")
  def syncString: Action = Action { req =>
    val res = req.manageResource(TestResource("sync-string"))
    s"registered:${res.name}, closed-immediately:${res.closed.get()}"
  }

  @Get("/resource-stream")
  def streamResource: Action = Action { req =>
    val data = "streaming-data-from-resource".getBytes
    val is = new ByteArrayInputStream(data)
    req.manageResource(is)
    is
  }

  /**
   * Returns a TrackedInputStream registered via manageResource.
   * The test verifies that:
   * 1. The stream is read (streaming completed)
   * 2. The stream is closed AFTER reading (not before)
   */
  @Get("/resource-stream-tracked")
  def streamTracked: Action = Action { req =>
    val is = new FailingAfterCloseInputStream("tracked-streaming-data".getBytes)
    req.manageResource(is)
    is
  }

  /**
   * Returns a SlowFailingInputStream that sleeps 200ms per byte.
   * The data is "12345" (5 bytes × 200ms = ~1 second total).
   * Tests that manageResource keeps the resource open for the entire
   * streaming duration and closes it only after all bytes are sent.
   */
  @Get("/resource-slow-stream")
  def slowStream: Action = Action { req =>
    val is = new SlowFailingInputStream("12345".getBytes, delayMs = 200)
    req.manageResource(is)
    is
  }

  /**
   * Async variant — returns Future[InputStream] where the InputStream is
   * a SlowFailingInputStream registered via manageResource. Tests that
   * manageResource works through the async (Future) path as well:
   * the resource must stay open until streaming completes, even though
   * the action returned a Future.
   */
  @Get("/resource-slow-stream-async")
  def slowStreamAsync: Action = Action.async { req =>
    import scala.concurrent.{ExecutionContext, Future}
    given ExecutionContext = ExecutionContext.global
    Future {
      val is = new SlowFailingInputStream("ABCDE".getBytes, delayMs = 200)
      req.manageResource(is)
      is
    }
  }

  @Get("/resource-multiple")
  def multipleResources: Action = Action { req =>
    val res1 = req.manageResource(TestResource("first"))
    val res2 = req.manageResource(TestResource("second"))
    val res3 = req.manageResource(TestResource("third"))
    s"registered: ${res1.name}, ${res2.name}, ${res3.name}"
  }

  @Get("/resource-result")
  def resultResource: Action = Action { req =>
    val res = req.manageResource(TestResource("result-test"))
    Ok(s"registered:${res.name}")
  }

  @Get("/resource-async")
  def asyncResource: Action = Action.async { req =>
    val res = req.manageResource(TestResource("async-test"))
    import scala.concurrent.{ExecutionContext, Future}
    given ExecutionContext = ExecutionContext.global
    Future(s"registered:${res.name}")
  }

end ResourceManagementServices

class ResourceManagementSuite extends munit.FunSuite:

  @volatile var server: Server = uninitialized

  override def beforeAll(): Unit =
    server = Server(0)
      .addServices(ResourceManagementServices)
      .start()

  override def afterAll(): Unit =
    server.stop()

  private def getBody(path: String): String =
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}$path"))
        .GET()
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200 for $path but got ${resp.statusCode()}: ${resp.body()}")
    resp.body()

  test("manageResource registers and does not close during action"):
    val body = getBody("/resource-sync-string")
    assert(body.contains("registered:sync-string"), s"body=$body")
    assert(body.contains("closed-immediately:false"), s"resource should not be closed during action")

  test("manageResource releases InputStream after streaming"):
    val body = getBody("/resource-stream")
    assertNoDiff(body, "streaming-data-from-resource")

  test("manageResource registers multiple resources"):
    val body = getBody("/resource-multiple")
    assert(body.contains("first"), s"body=$body")
    assert(body.contains("second"), s"body=$body")
    assert(body.contains("third"), s"body=$body")

  test("manageResource with Result return"):
    val body = getBody("/resource-result")
    assertNoDiff(body, "registered:result-test")

  test("manageResource with async action"):
    val body = getBody("/resource-async")
    assertNoDiff(body, "registered:async-test")

  test("manageResource closes InputStream AFTER streaming completes, not before"):
    // This test uses FailingAfterCloseInputStream which throws IOException
    // if read() is called after close(). If manageResource closed the
    // stream before streaming started (the old bug), the response would
    // be a 500 error, not 200. Getting 200 with the full body proves
    // the stream was read BEFORE it was closed.
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-stream-tracked"))
        .GET()
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200 — if resource was closed before streaming, this would be 500: ${resp.body()}")
    assertNoDiff(resp.body(), "tracked-streaming-data")

  test("manageResource keeps resource open during slow streaming (5 bytes × 200ms)"):
    // SlowFailingInputStream sleeps 200ms per byte. Data is "12345" (5 bytes).
    // Total streaming time: ~1 second. If the resource is closed before
    // streaming completes, the stream throws IOException and the response
    // fails. Getting 200 with "12345" proves the resource stayed open
    // for the entire ~1 second streaming duration.
    val start = System.currentTimeMillis()
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-slow-stream"))
        .GET()
        .timeout(java.time.Duration.ofSeconds(10))
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    val elapsed = System.currentTimeMillis() - start
    assertEquals(resp.statusCode(), 200, s"Expected 200 — if resource was closed before streaming completed, this would be 500: ${resp.body()}")
    assertNoDiff(resp.body(), "12345", s"Expected '12345', got: ${resp.body()}")
    assert(elapsed >= 500, s"Streaming should take at least ~1s (5 bytes × 200ms), took ${elapsed}ms — resource may have been closed early")

  test("manageResource with async Future keeps resource open during slow streaming"):
    // Same as above but via Action.async returning Future[InputStream].
    // The InputStream is created inside a Future on ExecutionContext.global.
    // Tests that manageResource works through the async path — the resource
    // must stay open until streaming completes, even though the action
    // returned a Future.
    val start = System.currentTimeMillis()
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-slow-stream-async"))
        .GET()
        .timeout(java.time.Duration.ofSeconds(10))
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    val elapsed = System.currentTimeMillis() - start
    assertEquals(resp.statusCode(), 200, s"Expected 200 — if resource was closed before streaming completed, this would be 500: ${resp.body()}")
    assertNoDiff(resp.body(), "ABCDE", s"Expected 'ABCDE', got: ${resp.body()}")
    assert(elapsed >= 500, s"Streaming should take at least ~1s (5 bytes × 200ms), took ${elapsed}ms — resource may have been closed early")

end ResourceManagementSuite