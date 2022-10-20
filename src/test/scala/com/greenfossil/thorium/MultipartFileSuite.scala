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
    assertEquals(mpFile.sizeInBytes, 473L)
  }

  test("MultipartFile extensions for conf") {
    val r = this.getClass.getResource("/logback-test.xml")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    println(s"mpFile.contentType = ${mpFile.contentType}")
    assert(mpFile.contentType.is(MediaType.parse("application/xml")))
    assertEquals(mpFile.sizeInBytes, 473L)
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
