package com.greenfossil.thorium

import com.linecorp.armeria.common.multipart.MultipartFile

import java.io.File

class MultipartFileSuite extends munit.FunSuite {

  test("MultipartFile extensions for image-no-ext/png") {
    val r = ClassLoader.getSystemResource("favicon.png")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file",new  File(r.getFile))
    assertNoDiff(mpFile.contentType, "image/png")
    assertEquals(mpFile.sizeInBytes, 3711L)
    assertEquals(mpFile.sizeInKB, 3L)
    assertEquals(mpFile.sizeInMB, 0L)
    assertEquals(mpFile.sizeInGB, 0L)
  }

  test("MultipartFile extensions for image-no-ext without extension") {
    val r = ClassLoader.getSystemResource("image-no-ext")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    assertNoDiff(mpFile.contentType, "image/png")
    assertEquals(mpFile.sizeInBytes, 3711L)
  }

  test("MultipartFile extensions for text/xml") {
    val r = ClassLoader.getSystemResource("logback-test.xml")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    assertNoDiff(mpFile.contentType, "application/xml")
    assertEquals(mpFile.sizeInBytes, 473L)
  }

  test("MultipartFile extensions for conf") {
    val r = ClassLoader.getSystemResource("logback-test.xml")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    println(s"mpFile.contentType = ${mpFile.contentType}")
    assertNoDiff(mpFile.contentType, "application/xml")
    assertEquals(mpFile.sizeInBytes, 473L)
  }

  test("Multipart inputStream") {
    val r = ClassLoader.getSystemResource("favicon.png")
    assert(r != null, "Resource not found")
    val mpFile = MultipartFile.of("file", "file", new File(r.getFile))
    1 to 5 foreach { i =>
      val is = mpFile.inputStream
      val size = is.available()
      println(s"i = ${i} ${is.available()}")
      val bytes = is.readNBytes(is.available())
      assertEquals(bytes.length, size)
      assertEquals(is.available(), 0)
      is.close()
    }
  }

}
