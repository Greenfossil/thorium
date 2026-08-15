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

end ResourceManagementSuite