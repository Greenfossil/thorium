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

import java.io.{ByteArrayInputStream, InputStream}
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

  /**
   * Simulates the full DB callback + manageResource pattern without a DB.
   *
   * A "connection" (TestConnection) is created, an InputStream is obtained
   * from it, and the connection is registered with manageResource via the
   * onBlockExit callback. The InputStream reads from the connection — if
   * the connection is closed before streaming completes, the stream fails.
   *
   * This mirrors:
   *   DB.readOnlyWithCallback(onBlockExit = req.manageResource(_)) {
   *     implicit conn => conn.binaryStream("SELECT ...")
   *   }
   */
  class TestConnection extends AutoCloseable:
    val closed = AtomicBoolean(false)
    def createStream(data: String): InputStream =
      new SlowFailingInputStream(data.getBytes, delayMs = 100)
    def close(): Unit = closed.set(true)

  class ConnectionBackedInputStream(conn: TestConnection, data: String) extends java.io.InputStream with AutoCloseable:
    private val delegate = conn.createStream(data)
    def read(): Int =
      if conn.closed.get() then throw new java.io.IOException("Connection closed — cannot read from stream")
      delegate.read()
    override def close(): Unit =
      delegate.close()
      conn.close()

  @Get("/resource-callback-sync")
  def callbackSync: Action = Action { req =>
    // Simulate: create connection, get stream, register connection via callback
    val conn = new TestConnection
    val stream = new ConnectionBackedInputStream(conn, "callback-sync-data")
    // onBlockExit callback — registers the connection for deferred close
    req.manageResource(conn)
    // Also register the stream (it will be closed too)
    req.manageResource(stream)
    stream
  }

  @Get("/resource-callback-async")
  def callbackAsync: Action = Action.async { req =>
    import scala.concurrent.{ExecutionContext, Future}
    given ExecutionContext = ExecutionContext.global
    Future {
      val conn = new TestConnection
      val stream = new ConnectionBackedInputStream(conn, "callback-async-data")
      req.manageResource(conn)
      req.manageResource(stream)
      stream
    }
  }

  @Get("/resource-callback-no-timeout")
  def callbackNoTimeout: Action = Action.noTimeout { req =>
    val conn = new TestConnection
    val stream = new ConnectionBackedInputStream(conn, "callback-notimeout-data")
    req.manageResource(conn)
    req.manageResource(stream)
    stream
  }

  @Get("/resource-exception")
  def exceptionAction: Action = Action { req =>
    req.manageResource(new TestResource("exception-test"))
    throw new RuntimeException("action-failure")
  }

  /**
   * Tests that resources are closed even when the action throws.
   */
  @Get("/resource-exception-verify-close")
  def exceptionVerifyClose: Action = Action { req =>
    val res = req.manageResource(new TestResource("exception-verify"))
    throw new RuntimeException("verify-close-failure")
  }

  /**
   * Tests that close() failure on one resource does not prevent
   * the remaining resources from being closed.
   */
  class FailingCloseResource(val name: String) extends AutoCloseable:
    val closed = AtomicBoolean(false)
    def close(): Unit =
      closed.set(true)
      throw new RuntimeException("close-failure-" + name)

  @Get("/resource-continue-on-failure")
  def continueOnFailure: Action = Action { req =>
    val res1 = req.manageResource(new FailingCloseResource("first"))
    val res2 = req.manageResource(new FailingCloseResource("second"))
    val res3 = req.manageResource(new TestResource("third"))
    s"registered: ${res1.name}, ${res2.name}, ${res3.name}"
  }

  /**
   * Tests that an already-closed resource is handled gracefully.
   */
  class PreClosedResource(val name: String) extends AutoCloseable:
    val closed = AtomicBoolean(true)  // already closed
    def close(): Unit =
      if closed.get() then throw new IllegalStateException("already closed")
      closed.set(true)

  @Get("/resource-already-closed")
  def alreadyClosed: Action = Action { req =>
    req.manageResource(new PreClosedResource("pre-closed"))
    req.manageResource(new TestResource("normal"))
    "ok"
  }

  /**
   * Tests manageResource with label parameter.
   */
  @Get("/resource-with-label")
  def withLabel: Action = Action { req =>
    val res = req.manageResource(new TestResource("labeled"), label = "my-db-conn")
    s"registered:${res.name}"
  }

  /**
   * Tests manageResource with maxLease parameter.
   * The slow stream takes ~1s (5 bytes x 200ms), maxLease is 100ms,
   * so a WARN should be logged at close time.
   */
  @Get("/resource-max-lease-exceeded")
  def maxLeaseExceeded: Action = Action { req =>
    val is = new SlowFailingInputStream("12345".getBytes, delayMs = 200)
    req.manageResource(is, label = "slow-stream", maxLease = java.time.Duration.ofMillis(100))
    is
  }

  /**
   * Tests manageResource with maxLease that is NOT exceeded.
   * Quick response, maxLease is 10s — no WARN should be logged.
   */
  @Get("/resource-max-lease-ok")
  def maxLeaseOk: Action = Action { req =>
    val res = req.manageResource(new TestResource("fast"), label = "fast-res", maxLease = java.time.Duration.ofSeconds(10))
    s"registered:${res.name}"
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

  // ===========================================================================
  // Callback + manageResource pattern tests
  // Simulates DB.readOnlyWithCallback(onBlockExit = req.manageResource(_))
  // ===========================================================================

  test("callback + manageResource (sync) — connection stays open during streaming"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-callback-sync"))
        .GET()
        .timeout(java.time.Duration.ofSeconds(10))
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200 — if connection was closed before streaming, this would be 500: ${resp.body()}")
    assertNoDiff(resp.body(), "callback-sync-data")

  test("callback + manageResource (async Future) — connection stays open during streaming"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-callback-async"))
        .GET()
        .timeout(java.time.Duration.ofSeconds(10))
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200 — if connection was closed before streaming, this would be 500: ${resp.body()}")
    assertNoDiff(resp.body(), "callback-async-data")

  test("callback + manageResource + noTimeout — connection stays open during streaming"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-callback-no-timeout"))
        .GET()
        .timeout(java.time.Duration.ofSeconds(10))
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200 — if connection was closed before streaming, this would be 500: ${resp.body()}")
    assertNoDiff(resp.body(), "callback-notimeout-data")

  test("manageResource closes resources even when action throws"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-exception"))
        .GET()
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 500, s"Expected 500 from thrown exception: ${resp.body()}")

  test("callback pattern — LIFO close order (stream closed before connection)"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-callback-sync"))
        .GET()
        .timeout(java.time.Duration.ofSeconds(10))
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200: ${resp.body()}")

  test("manageResource continues closing remaining resources when one close() throws"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-continue-on-failure"))
        .GET()
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200: ${resp.body()}")
    assert(resp.body().contains("first"), s"body=${resp.body()}")
    assert(resp.body().contains("second"), s"body=${resp.body()}")
    assert(resp.body().contains("third"), s"body=${resp.body()}")

  test("manageResource handles already-closed resources gracefully"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-already-closed"))
        .GET()
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200: ${resp.body()}")
    assertNoDiff(resp.body(), "ok")

  test("manageResource with label registers successfully"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-with-label"))
        .GET()
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200: ${resp.body()}")
    assert(resp.body().contains("labeled"), s"body=${resp.body()}")

  test("manageResource with maxLease exceeded still streams correctly"):
    // Slow stream takes ~1s, maxLease is 100ms — a WARN is logged at close
    // but the streaming should still work correctly.
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-max-lease-exceeded"))
        .GET()
        .timeout(java.time.Duration.ofSeconds(10))
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200: ${resp.body()}")
    assertNoDiff(resp.body(), "12345")

  test("manageResource with maxLease not exceeded works normally"):
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/resource-max-lease-ok"))
        .GET()
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200, s"Expected 200: ${resp.body()}")
    assert(resp.body().contains("fast"), s"body=${resp.body()}")

end ResourceManagementSuite