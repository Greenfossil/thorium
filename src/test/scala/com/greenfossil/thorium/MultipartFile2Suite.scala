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
import com.linecorp.armeria.server.annotation.Post
import munit.FunSuite

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.sys.process.*

/**
 * Test suite for the new multipart file APIs:
 * - findFile(predicate: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean)
 * - findFiles(validatorFn: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean)
 *
 * This test suite focuses on:
 * 1. API ease of use
 * 2. Support for standard formats (PDF, JPG, PNG)
 * 3. Robustness against malicious attacks:
 *    - Bad format files (e.g., EXE, COM, BAT)
 *    - Large file DOS attacks
 *    - Files with incorrect MIME types
 *    - Executable file uploads
 *    - File size validation
 */
object FileValidationServices {

  // ============ Allow-list validator: Only PDF, JPG, PNG ============
  private def isAllowedFormat(fieldName: String, fileName: String, contentType: MediaType, content: InputStream): Boolean = {
    val allowedTypes = Set(
      MediaType.PDF,
      MediaType.JPEG,
      MediaType.PNG,
      MediaType.MICROSOFT_EXCEL,
      MediaType.MICROSOFT_WORD,
      MediaType.MICROSOFT_POWERPOINT
    )
    allowedTypes.exists(ct => contentType.is(ct))
  }

  // ============ Size validator: Max 5MB ============
  private def validateFileSize(fieldName: String, fileName: String, contentType: MediaType, content: InputStream): Boolean = {
    val maxSize = 5 * 1024 * 1024 // 5MB
    val buffer = new Array[Byte](8192)
    var totalSize = 0
    var bytesRead = 0

    while {
      bytesRead = content.read(buffer)
      bytesRead != -1
    } do
      totalSize += bytesRead
      if (totalSize > maxSize) return false
    totalSize <= maxSize
  }

  // ============ Combined validator: Format + Size ============
  private def validateFormatAndSize(fieldName: String, fileName: String, contentType: MediaType, content: InputStream): Boolean = {
    isAllowedFormat(fieldName, fileName, contentType, content) && validateFileSize(fieldName, fileName, contentType, content)
  }

  // ============ Magic number validator: Check actual file content ============
  private def validateByMagicNumber(fieldName: String, fileName: String, contentType: MediaType, content: InputStream): Boolean = {
    val magicBytes = new Array[Byte](4)
    val bytesRead = content.read(magicBytes)

    if (bytesRead < 2) return false

    val byte1 = magicBytes(0) & 0xFF
    val byte2 = magicBytes(1) & 0xFF
    val byte3 = if (bytesRead > 2) magicBytes(2) & 0xFF else 0
    val byte4 = if (bytesRead > 3) magicBytes(3) & 0xFF else 0

    // PDF: 25 50 44 46 (%PDF)
    if (byte1 == 0x25 && byte2 == 0x50 && byte3 == 0x44 && byte4 == 0x46) return true

    // JPEG: FF D8 FF
    if (byte1 == 0xFF && byte2 == 0xD8 && byte3 == 0xFF) return true

    // PNG: 89 50 4E 47
    if (byte1 == 0x89 && byte2 == 0x50 && byte3 == 0x4E && byte4 == 0x47) return true

    // EXE/DLL: 4D 5A (MZ)
    if (byte1 == 0x4D && byte2 == 0x5A) {
      println(s"Blocked: Executable file detected: $fileName")
      return false
    }

    // Zip files (including JAR): 50 4B
    if (byte1 == 0x50 && byte2 == 0x4B) {
      println(s"Blocked: Zip/Archive file detected: $fileName")
      return false
    }

    false
  }

  // ============ Filename validator: Block dangerous extensions ============
  private def validateFileName(fieldName: String, fileName: String, contentType: MediaType, content: InputStream): Boolean = {
    val blockedExtensions = Set(
      "exe", "com", "bat", "cmd", "scr", "vbs", "js", "jar", "zip",
      "rar", "7z", "dll", "sys", "msi", "sh", "bash", "ps1"
    )

    val extension = fileName.split('.').lastOption.getOrElse("").toLowerCase
    !blockedExtensions.contains(extension)
  }

  // ============ Endpoints ============

  @Post("/file/validate-format-only")
  def validateFormatOnly: Action = Action.multipart { implicit request =>
    request.findFile(isAllowedFormat)
      .fold(
        th => BadRequest(s"Invalid file format - ${th.getMessage}"),
        file => Ok(s"File accepted: ${file.filename()} (${file.realContentType})")
      )
  }

  @Post("/file/validate-format-and-size")
  def validateFormatAndSizeEndpoint: Action = Action.multipart { implicit request =>
    request.findFile(validateFormatAndSize)
      .fold(
        th => BadRequest(s"Invalid file format or size exceeds limit- ${th.getMessage}"),
        file => Ok(s"File accepted: ${file.filename()} (Size: ${file.sizeInBytes} bytes)")
      )
  }

  @Post("/file/validate-magic-number")
  def validateMagicNumberEndpoint: Action = Action.multipart { implicit request =>
    request.findFile(validateByMagicNumber)
      .fold(
        th => BadRequest(s"Invalid file content detected - ${th.getMessage}"),
        file => Ok(s"File accepted: ${file.filename()}")
      )
  }

  @Post("/file/validate-filename")
  def validateFilenameEndpoint: Action = Action.multipart { implicit request =>
    request.findFile(validateFileName)
      .fold(
        th => BadRequest(s"Invalid file name - ${th.getMessage}"),
        file => Ok(s"File accepted: ${file.filename()}")
      )
  }

  @Post("/files/validate-multiple")
  def validateMultipleFiles: Action = Action.multipart { implicit request =>
    request.findFiles(validateFormatAndSize)
      .fold(
        th => BadRequest(s"Invalid files detected - ${th.getMessage}"),
        files => Ok(s"Files accepted: ${files.size} files uploaded")
      )
  }

  @Post("/file/always-accept")
  def alwaysAccept: Action = Action.multipart { implicit request =>
    request.findFile((_, _, _, _) => true)
      .fold(
        th => BadRequest(s"Invalid file upload - ${th.getMessage}"),
        file => Ok(s"File accepted: ${file.filename()}")
      )
  }

  @Post("/files/all-or-nothing")
  def allOrNothing: Action = Action.multipart { implicit request =>
    request.findFiles(validateFormatAndSize)
      .fold(
        th => BadRequest(s"Invalid files detected - ${th.getMessage}"),
        files => Ok(s"All ${files.size} files passed validation")
      )
  }

  @Post("/files/empty-content")
  def emptyContent: Action = Action.multipart { implicit request =>
    val files = request.findFiles((fieldname, fileName, contentType, content) => fieldname == "file1")
    println(s"Empty Content - Found files: ${files}")
    files.fold(
        th => BadRequest(s"Invalid files detected - ${th.getMessage}"),
        files => Ok(s"Files uploaded ${files.size}")
      )
  }

  @Post("/files/blank-file-name")
  def blankFileName: Action = Action.multipart { implicit request =>
    val files = request.findFiles((fieldname, fileName, contentType, content) => fieldname == "file1")
    println(s"Blank File Name - Found files: ${files}")
    files.fold(
        th =>
          th.printStackTrace()
          BadRequest(s"Invalid files detected - ${th.getMessage}"),
        files => Ok(s"All ${files.size} files passed validation")
      )
  }

}

import scala.compiletime.uninitialized

class MultipartFile2Suite extends FunSuite {

  @volatile var server: Server = uninitialized
  val testUploadDir: java.nio.file.Path = Paths.get("/tmp/thorium-multipart-test")

  override def beforeAll(): Unit = {
    try {
      // Clean and create test upload directory
      if (Files.exists(testUploadDir)) {
        testUploadDir.toFile.listFiles().foreach(_.delete())
      }
      Files.createDirectories(testUploadDir)

      server = Server(0)
        .addServices(FileValidationServices)
        .start()
    } catch {
      case ex: Throwable =>
        println(s"Error starting server: ${ex.getMessage}")
        ex.printStackTrace()
    }
  }

  override def afterAll(): Unit = {
    if (server != null) server.stop()
    // Cleanup test files
    if (Files.exists(testUploadDir)) {
      testUploadDir.toFile.listFiles().foreach(_.delete())
      Files.delete(testUploadDir)
    }
  }

  // ============ Helper methods ============

  private def createPdfFile(path: String): Unit = {
    // Minimal PDF file structure
    val pdfContent =
      """%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >>
endobj
4 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
5 0 obj
<< /Length 44 >>
stream
BT
/F1 12 Tf
100 700 Td
(Hello PDF) Tj
ET
endstream
endobj
xref
0 6
0000000000 65535 f
0000000009 00000 n
0000000058 00000 n
0000000115 00000 n
0000000229 00000 n
0000000310 00000 n
trailer
<< /Size 6 /Root 1 0 R >>
startxref
406
%%EOF"""
    Files.write(Paths.get(path), pdfContent.getBytes(StandardCharsets.UTF_8))
  }

  private def createJpgFile(path: String): Unit = {
    // Minimal JPEG file (1x1 pixel white image)
    val jpgBytes = Array[Byte](
      0xFF.toByte, 0xD8.toByte, 0xFF.toByte, 0xE0.toByte, 0x00.toByte, 0x10.toByte, 0x4A.toByte, 0x46.toByte,
      0x49.toByte, 0x46.toByte, 0x00.toByte, 0x01.toByte, 0x01.toByte, 0x00.toByte, 0x00.toByte, 0x01.toByte,
      0x00.toByte, 0x01.toByte, 0x00.toByte, 0x00.toByte, 0xFF.toByte, 0xDB.toByte, 0x00.toByte, 0x43.toByte,
      0x00.toByte, 0x08.toByte, 0x06.toByte, 0x06.toByte, 0x07.toByte, 0x06.toByte, 0x05.toByte, 0x08.toByte,
      0x07.toByte, 0x07.toByte, 0x07.toByte, 0x09.toByte, 0x09.toByte, 0x08.toByte, 0x0A.toByte, 0x0C.toByte,
      0x14.toByte, 0x0D.toByte, 0x0C.toByte, 0x0B.toByte, 0x0B.toByte, 0x0C.toByte, 0x19.toByte, 0x12.toByte,
      0x13.toByte, 0x0F.toByte, 0x14.toByte, 0x1D.toByte, 0x1A.toByte, 0x1F.toByte, 0x1E.toByte, 0x1D.toByte,
      0x1A.toByte, 0x1C.toByte, 0x1C.toByte, 0x20.toByte, 0x24.toByte, 0x2E.toByte, 0x27.toByte, 0x20.toByte,
      0x22.toByte, 0x2C.toByte, 0x23.toByte, 0x1C.toByte, 0x1C.toByte, 0x28.toByte, 0x37.toByte, 0x29.toByte,
      0x2C.toByte, 0x30.toByte, 0x31.toByte, 0x34.toByte, 0x34.toByte, 0x34.toByte, 0x1F.toByte, 0x27.toByte,
      0x39.toByte, 0x3D.toByte, 0x38.toByte, 0x32.toByte, 0x3C.toByte, 0x2E.toByte, 0x33.toByte, 0x34.toByte,
      0x32.toByte, 0xFF.toByte, 0xC0.toByte, 0x00.toByte, 0x0B.toByte, 0x08.toByte, 0x00.toByte, 0x01.toByte,
      0x00.toByte, 0x01.toByte, 0x01.toByte, 0x01.toByte, 0x11.toByte, 0x00.toByte, 0xFF.toByte, 0xC4.toByte,
      0x00.toByte, 0x1F.toByte, 0x00.toByte, 0x00.toByte, 0x01.toByte, 0x05.toByte, 0x01.toByte, 0x01.toByte,
      0x01.toByte, 0x01.toByte, 0x01.toByte, 0x01.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte,
      0x00.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, 0x01.toByte, 0x02.toByte, 0x03.toByte, 0x04.toByte,
      0x05.toByte, 0x06.toByte, 0x07.toByte, 0x08.toByte, 0x09.toByte, 0x0A.toByte, 0x0B.toByte, 0xFF.toByte,
      0xC4.toByte, 0x00.toByte, 0xB5.toByte, 0x10.toByte, 0x00.toByte, 0x02.toByte, 0x01.toByte, 0x03.toByte,
      0x03.toByte, 0x02.toByte, 0x04.toByte, 0x03.toByte, 0x05.toByte, 0x05.toByte, 0x04.toByte, 0x04.toByte,
      0x00.toByte, 0x00.toByte, 0x01.toByte, 0x7D.toByte, 0x01.toByte, 0x02.toByte, 0x03.toByte, 0x00.toByte,
      0x04.toByte, 0x11.toByte, 0x05.toByte, 0x12.toByte, 0x21.toByte, 0x31.toByte, 0x41.toByte, 0x06.toByte,
      0x13.toByte, 0x51.toByte, 0x61.toByte, 0x07.toByte, 0x22.toByte, 0x71.toByte, 0x14.toByte, 0x32.toByte,
      0x81.toByte, 0x91.toByte, 0xA1.toByte, 0x08.toByte, 0x23.toByte, 0x42.toByte, 0xB1.toByte, 0xC1.toByte,
      0x15.toByte, 0x52.toByte, 0xD1.toByte, 0xF0.toByte, 0x24.toByte, 0x33.toByte, 0x62.toByte, 0x72.toByte,
      0x82.toByte, 0x09.toByte, 0x0A.toByte, 0x16.toByte, 0x17.toByte, 0x18.toByte, 0x19.toByte, 0x1A.toByte,
      0x25.toByte, 0x26.toByte, 0x27.toByte, 0x28.toByte, 0x29.toByte, 0x2A.toByte, 0x34.toByte, 0x35.toByte,
      0x36.toByte, 0x37.toByte, 0x38.toByte, 0x39.toByte, 0x3A.toByte, 0x43.toByte, 0x44.toByte, 0x45.toByte,
      0x46.toByte, 0x47.toByte, 0x48.toByte, 0x49.toByte, 0x4A.toByte, 0x53.toByte, 0x54.toByte, 0x55.toByte,
      0x56.toByte, 0x57.toByte, 0x58.toByte, 0x59.toByte, 0x5A.toByte, 0x63.toByte, 0x64.toByte, 0x65.toByte,
      0x66.toByte, 0x67.toByte, 0x68.toByte, 0x69.toByte, 0x6A.toByte, 0x73.toByte, 0x74.toByte, 0x75.toByte,
      0x76.toByte, 0x77.toByte, 0x78.toByte, 0x79.toByte, 0x7A.toByte, 0x83.toByte, 0x84.toByte, 0x85.toByte,
      0x86.toByte, 0x87.toByte, 0x88.toByte, 0x89.toByte, 0x8A.toByte, 0x92.toByte, 0x93.toByte, 0x94.toByte,
      0x95.toByte, 0x96.toByte, 0x97.toByte, 0x98.toByte, 0x99.toByte, 0x9A.toByte, 0xA2.toByte, 0xA3.toByte,
      0xA4.toByte, 0xA5.toByte, 0xA6.toByte, 0xA7.toByte, 0xA8.toByte, 0xA9.toByte, 0xAA.toByte, 0xB2.toByte,
      0xB3.toByte, 0xB4.toByte, 0xB5.toByte, 0xB6.toByte, 0xB7.toByte, 0xB8.toByte, 0xB9.toByte, 0xBA.toByte,
      0xC2.toByte, 0xC3.toByte, 0xC4.toByte, 0xC5.toByte, 0xC6.toByte, 0xC7.toByte, 0xC8.toByte, 0xC9.toByte,
      0xCA.toByte, 0xD2.toByte, 0xD3.toByte, 0xD4.toByte, 0xD5.toByte, 0xD6.toByte, 0xD7.toByte, 0xD8.toByte,
      0xD9.toByte, 0xDA.toByte, 0xE1.toByte, 0xE2.toByte, 0xE3.toByte, 0xE4.toByte, 0xE5.toByte, 0xE6.toByte,
      0xE7.toByte, 0xE8.toByte, 0xE9.toByte, 0xEA.toByte, 0xF1.toByte, 0xF2.toByte, 0xF3.toByte, 0xF4.toByte,
      0xF5.toByte, 0xF6.toByte, 0xF7.toByte, 0xF8.toByte, 0xF9.toByte, 0xFA.toByte, 0xFF.toByte, 0xDA.toByte,
      0x00.toByte, 0x08.toByte, 0x01.toByte, 0x01.toByte, 0x00.toByte, 0x00.toByte, 0x3F.toByte, 0x00.toByte,
      0xFB.toByte, 0xD4.toByte, 0xFF.toByte, 0xD9.toByte
    )
    Files.write(Paths.get(path), jpgBytes)
  }

  private def createPngFile(path: String): Unit = {
    // Minimal PNG file (1x1 pixel)
    val pngBytes = Array[Byte](
      0x89.toByte, 0x50.toByte, 0x4E.toByte, 0x47.toByte, 0x0D.toByte, 0x0A.toByte, 0x1A.toByte, 0x0A.toByte,
      0x00.toByte, 0x00.toByte, 0x00.toByte, 0x0D.toByte, 0x49.toByte, 0x48.toByte, 0x44.toByte, 0x52.toByte,
      0x00.toByte, 0x00.toByte, 0x00.toByte, 0x01.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, 0x01.toByte,
      0x08.toByte, 0x06.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, 0x1F.toByte, 0x15.toByte, 0xC4.toByte,
      0x89.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, 0x0A.toByte, 0x49.toByte, 0x44.toByte, 0x41.toByte,
      0x54.toByte, 0x78.toByte, 0x9C.toByte, 0x63.toByte, 0xF8.toByte, 0x0F.toByte, 0x00.toByte, 0x00.toByte,
      0x01.toByte, 0x01.toByte, 0x01.toByte, 0x00.toByte, 0x18.toByte, 0xDD.toByte, 0x8D.toByte, 0xB4.toByte,
      0x00.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, 0x49.toByte, 0x45.toByte, 0x4E.toByte, 0x44.toByte,
      0xAE.toByte, 0x42.toByte, 0x60.toByte, 0x82.toByte
    )
    Files.write(Paths.get(path), pngBytes)
  }

  private def createLargeFile(path: String, sizeInMB: Int): Unit = {
    val content = "x" * (sizeInMB * 1024 * 1024)
    Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))
  }

  private def createExeFile(path: String): Unit = {
    // EXE file magic number: 4D 5A (MZ)
    val exeBytes = Array[Byte](
      0x4D.toByte, 0x5A.toByte, 0x90.toByte, 0x00.toByte, 0x03.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte,
      0x04.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, 0xFF.toByte, 0xFF.toByte, 0x00.toByte, 0x00.toByte
    )
    Files.write(Paths.get(path), exeBytes)
  }

  // ============ Test: findFile with valid PDF ============
  test("findFile: Accept valid PDF") {
    val pdfPath = "/tmp/test-valid.pdf"
    createPdfFile(pdfPath)

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$pdfPath".!!.trim
    assertNoDiff(result, "File accepted: test-valid.pdf (application/pdf)")
  }

  // ============ Test: findFile with valid JPG ============
  test("findFile: Accept valid JPG") {
    val jpgPath = "/tmp/test-valid.jpg"
    createJpgFile(jpgPath)

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$jpgPath".!!.trim
    assertNoDiff(result, "File accepted: test-valid.jpg (image/jpeg)")
  }

  // ============ Test: findFile with valid PNG ============
  test("findFile: Accept valid PNG") {
    val pngPath = "/tmp/test-valid.png"
    createPngFile(pngPath)

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$pngPath".!!.trim
    assertNoDiff(result, "File accepted: test-valid.png (image/png)")
  }

  // ============ Test: findFile rejects text file ============
  test("findFile: Reject text file with wrong MIME type") {
    val txtPath = "/tmp/test-document.txt"
    Files.write(Paths.get(txtPath), "Just plain text".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$txtPath".!!.trim
    assertNoDiff(result, "Invalid file format - File test-document.txt with content type text/plain is not allowed")
  }

  // ============ Test: findFile rejects EXE file ============
  test("findFile: Reject executable EXE file by magic number") {
    val exePath = "/tmp/test-malware.exe"
    createExeFile(exePath)

    val result = s"curl http://localhost:${server.port}/file/validate-magic-number -F resourceFile=@$exePath".!!.trim
    assertNoDiff(result, "Invalid file content detected - File test-malware.exe with content type application/octet-stream is not allowed")
  }

  // ============ Test: findFile validates file size ============
  test("findFile: Reject oversized file (DOS attack prevention)") {
    val largePath = "/tmp/test-large.pdf"
    createPdfFile(largePath)
    // Append large content to simulate a large file while keeping valid PDF header
    val writer = Files.newOutputStream(Paths.get(largePath), java.nio.file.StandardOpenOption.APPEND)
    val largeContent = "x" * (10 * 1024 * 1024) // 10MB beyond PDF
    writer.write(largeContent.getBytes(StandardCharsets.UTF_8))
    writer.close()

    val result = s"curl http://localhost:${server.port}/file/validate-format-and-size -F resourceFile=@$largePath".!!.trim
    println(s"result = ${result}")
    assertNoDiff(result,
      """Status: 413
        |Description: Request Entity Too Large""".stripMargin)
  }

  // ============ Test: findFile accepts file within size limit ============
  test("findFile: Accept file within size limit") {
    val pdfPath = "/tmp/test-sized.pdf"
    createPdfFile(pdfPath)

    val result = s"curl http://localhost:${server.port}/file/validate-format-and-size -F resourceFile=@$pdfPath".!!.trim
    assertEquals(result, "File accepted: test-sized.pdf (Size: 578 bytes)")
  }

  // ============ Test: findFile rejects file with dangerous extension ============
  test("findFile: Reject file with dangerous extension (.exe)") {
    val txtPath = "/tmp/test-masked.exe"
    Files.write(Paths.get(txtPath), "Just text pretending to be an exe".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/file/validate-filename -F resourceFile=@$txtPath".!!.trim
    assertNoDiff(result, "Invalid file name - File test-masked.exe with content type application/octet-stream is not allowed")
  }

  // ============ Test: findFile works with different field names ============
  test("findFile: Accept file with any field name") {
    val pdfPath = "/tmp/test-fieldname.pdf"
    createPdfFile(pdfPath)

    val result = s"curl http://localhost:${server.port}/file/always-accept -F anyFieldName=@$pdfPath".!!.trim
    assertNoDiff(result, "File accepted: test-fieldname.pdf")
  }

  // ============ Test: findFiles returns multiple files ============
  test("findFiles: Accept multiple valid files") {
    val pdfPath = "/tmp/test-multi-1.pdf"
    val jpgPath = "/tmp/test-multi-2.jpg"

    createPdfFile(pdfPath)
    createJpgFile(jpgPath)

    val result = s"curl http://localhost:${server.port}/files/validate-multiple -F file1=@$pdfPath -F file2=@$jpgPath".!!.trim
    assertNoDiff(result, "Files accepted: 2 files uploaded")
  }

  // ============ Test: findFiles all-or-nothing validation ============
  test("findFiles: Reject all if one file fails validation (all-or-nothing)") {
    val pdfPath = "/tmp/test-all-1.pdf"
    val txtPath = "/tmp/test-all-2.txt"

    createPdfFile(pdfPath)
    Files.write(Paths.get(txtPath), "Invalid file".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/files/all-or-nothing -F file1=@$pdfPath -F file2=@$txtPath".!!.trim
    assertNoDiff(result, "Invalid files detected - File test-all-2.txt with content type text/plain is not allowed")
  }

  // ============ Test: findFiles accepts all valid files ============
  test("findFiles: Accept all when all files pass validation") {
    val pdf1Path = "/tmp/test-ok-1.pdf"
    val pdf2Path = "/tmp/test-ok-2.pdf"
    val pngPath = "/tmp/test-ok-3.png"

    createPdfFile(pdf1Path)
    createPdfFile(pdf2Path)
    createPngFile(pngPath)

    val result = s"curl http://localhost:${server.port}/files/all-or-nothing -F f1=@$pdf1Path -F f2=@$pdf2Path -F f3=@$pngPath".!!.trim
    assertNoDiff(result, "All 3 files passed validation")
  }

  // ============ Test: Ease of use - Simple validator ============
  test("API Ease of Use: Simple inline validator") {
    val pdfPath = "/tmp/test-inline.pdf"
    createPdfFile(pdfPath)

    // This test demonstrates the ease of creating custom validators
    val result = s"curl http://localhost:${server.port}/file/always-accept -F document=@$pdfPath".!!.trim
    assertNoDiff(result, "File accepted: test-inline.pdf")
  }

  // ============ New Tests: Accept Microsoft Office binary formats ============
  test("findFile: Accept Microsoft Excel (XLS)") {
    val xlsPath = "/tmp/test-excel.xls"
    Files.write(Paths.get(xlsPath), "Excel content".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$xlsPath;type=application/vnd.ms-excel".!!.trim
    assertNoDiff(result, "File accepted: test-excel.xls (application/vnd.ms-excel)")
  }

  test("findFile: Accept Microsoft Word (DOC)") {
    val docPath = "/tmp/test-word.doc"
    Files.write(Paths.get(docPath), "Word content".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$docPath;type=application/msword".!!.trim
    assertNoDiff(result, "File accepted: test-word.doc (application/msword)")
  }

  test("findFile: Accept Microsoft PowerPoint (PPT)") {
    val pptPath = "/tmp/test-ppt.ppt"
    Files.write(Paths.get(pptPath), "PowerPoint content".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$pptPath;type=application/vnd.ms-powerpoint".!!.trim
    assertNoDiff(result, "File accepted: test-ppt.ppt (application/vnd.ms-powerpoint)")
  }

  // ============ Test: Reject ZIP files (archive attack) ============
  test("findFile: Reject ZIP files (archive bombs prevention)") {
    val zipPath = "/tmp/test-archive.zip"
    // ZIP file magic: 50 4B
    val zipBytes = Array[Byte](
      0x50.toByte, 0x4B.toByte, 0x03.toByte, 0x04.toByte, 0x14.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte
    )
    Files.write(Paths.get(zipPath), zipBytes)

    val result = s"curl http://localhost:${server.port}/file/validate-magic-number -F resourceFile=@$zipPath".!!.trim
    assertNoDiff(result, "Invalid file content detected - File test-archive.zip with content type application/octet-stream is not allowed")
  }

  // ============ Test: Error handling with Try ============
  test("API Error Handling: Try[MultipartFile] properly handles failures") {
    val txtPath = "/tmp/test-error.txt"
    Files.write(Paths.get(txtPath), "Wrong format".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$txtPath".!!.trim
    assertNoDiff(result, "Invalid file format - File test-error.txt with content type text/plain is not allowed")
  }

  // ============ Test: Content stream inspection ============
  test("API Feature: Validator can inspect content stream") {
    val pdfPath = "/tmp/test-content.pdf"
    createPdfFile(pdfPath)

    // The validator function receives the content InputStream, so it can inspect bytes
    val result = s"curl http://localhost:${server.port}/file/validate-magic-number -F doc=@$pdfPath".!!.trim
    assertNoDiff(result, "File accepted: test-content.pdf")
  }

  // ============ Test: findFile rejects when declared MIME type differs from actual content ============
  test("findFile: Reject when declared MIME type differs from actual content".fail) {
    val pdfPath = "/tmp/test-mismatch.pdf"
    createPdfFile(pdfPath)

    // upload while declaring the content type as image/jpeg (mismatch)
    val result = s"curl http://localhost:${server.port}/file/validate-format-only -F resourceFile=@$pdfPath;type=image/jpeg".!!.trim

    // server wraps the underlying IllegalArgumentException message with the endpoint's prefix
    val expected = "Invalid file format - File test-mismatch.pdf has content type image/jpeg but actual content type is application/pdf"
    assertNoDiff(result, expected)
  }

  // ============ Test: findFiles rejects zero-length file ============
  test("findFiles: Reject zero-length file") {
    val emptyPath = "/tmp/test-empty.pdf"
    Files.write(Paths.get(emptyPath), Array.emptyByteArray)

    val result = s"curl http://localhost:${server.port}/files/empty-content -F file1=@$emptyPath".!!.trim
    assertNoDiff(result, "Files uploaded 0")
  }

  // ============ Test: findFiles rejects file with empty or whitespace filename ============
  test("findFiles: Reject file with empty or whitespace filename") {
    val pdfPath = "/tmp/test-blank-name.pdf"
    createPdfFile(pdfPath)

    val result = s"""curl http://localhost:${server.port}/files/blank-file-name -F "file1=@$pdfPath;filename= " """.!!.trim
    assertNoDiff(result, "Invalid files detected - Empty or whitespace-only filename not allowed")
  }

}