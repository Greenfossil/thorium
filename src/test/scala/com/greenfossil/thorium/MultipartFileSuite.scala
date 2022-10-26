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
import com.linecorp.armeria.common.multipart.MultipartFile

import java.io.File

class MultipartFileSuite extends munit.FunSuite {

  test("MultipartFile extensions for image-no-ext/png") {
    val r = this.getClass.getResource("/favicon.png")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file",new  File(r.getFile))
    assert(mpFile.contentType.is(MediaType.ANY_IMAGE_TYPE))
    assert(mpFile.contentType.is(MediaType.PNG))
    assertEquals(mpFile.sizeInBytes, 3711L)
    assertEquals(mpFile.sizeInKB, 3L)
    assertEquals(mpFile.sizeInMB, 0L)
    assertEquals(mpFile.sizeInGB, 0L)
  }

  test("MultipartFile extensions for image-no-ext without extension") {
    val r = this.getClass.getResource("/image-no-ext")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    assert(mpFile.contentType.is(MediaType.ANY_IMAGE_TYPE))
    assert(mpFile.contentType.is(MediaType.PNG))
    assertEquals(mpFile.sizeInBytes, 3711L)
  }

  test("MultipartFile extensions for text/xml") {
    val r = this.getClass.getResource("/logback-test.xml")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    assert(mpFile.contentType.is(MediaType.parse("application/xml")))
    assertEquals(mpFile.sizeInBytes, 1094L)
  }

  test("MultipartFile extensions for conf") {
    val r = this.getClass.getResource("/logback-test.xml")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    println(s"mpFile.contentType = ${mpFile.contentType}")
    assert(mpFile.contentType.is(MediaType.parse("application/xml")))
    assertEquals(mpFile.sizeInBytes, 1094L)
  }

  test("Multipart inputStream") {
    val r = this.getClass.getResource("/favicon.png")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    1 to 5 foreach { i =>
      val is = mpFile.inputStream
      val size = is.available()
      println(s"i = ${i} ${is.available()}")
      val bytes = is.readAllBytes()
      assertEquals(bytes.length, size)
      assertEquals(is.available(), 0)
      is.close()
    }
  }

}
