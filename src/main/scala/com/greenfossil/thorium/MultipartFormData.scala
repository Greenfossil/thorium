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
import com.linecorp.armeria.common.multipart.{AggregatedBodyPart, AggregatedMultipart, MultipartFile}

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.file.*
import scala.util.Try

case class MultipartFormData(aggMultipart: AggregatedMultipart, multipartUploadLocation: Path):
  import scala.jdk.CollectionConverters.*

  lazy val bodyPart: Seq[AggregatedBodyPart] = aggMultipart.bodyParts().asScala.toSeq

  lazy val names: List[String] = aggMultipart.names().asScala.toList

  lazy val asFormUrlEncoded: FormUrlEndcoded =
    val xs = for {
      name <- names
      part <- aggMultipart.fields(name).asScala
      content =
        if part.contentType().is(MediaType.PLAIN_TEXT) then
          part.content(Option(part.contentType().charset()).getOrElse(Charset.forName("UTF-8")))
        else if part.filename() != null then
          part.filename()
        else null
      if content != null
    } yield
      (name, content)
    FormUrlEndcoded(xs.groupMap(_._1)(_._2))

  @deprecated("Use getFiles instead")
  private def saveFileTo( part: AggregatedBodyPart): Option[File] =
    Try {
      if !Files.exists(multipartUploadLocation) then multipartUploadLocation.toFile.mkdirs()
      val filePath = multipartUploadLocation.resolve(part.filename())
      val is: InputStream = part.content().toInputStream
      Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING)
      is.close()
      filePath.toFile
    }.toOption

  @deprecated("Use findFiles instead")
  lazy val files: List[MultipartFile] =
    for {
      name <- names
      part <- aggMultipart.fields(name).asScala
      if part.filename() != null && !part.content().isEmpty
      file <- saveFileTo(part)
    } yield  MultipartFile.of(name, part.filename(), file)

  /**
   * Save the uploaded file to disk with validation
   * if real mime type is different from the part.contentType(), an exception is thrown
   * if validatorFn returns false, an exception is thrown
   * @param fieldName
   * @param part
   * @param validatorFn
   * @return
   */
  private def saveFileTo(fieldName: String, part: AggregatedBodyPart, validatorFn: (fieldName:String, fileName:String, contentType:MediaType, content: InputStream) => Boolean): Try[File] =
    Try:
      val is = part.content().toInputStream
      try
        if !validatorFn(fieldName, part.filename(), part.contentType(), is) then
          throw new IllegalArgumentException(s"File ${part.filename()} with content type ${part.contentType()} is not allowed")

        //Check if  realMimeType is same as part.contentType()
        val realMimeType = mimeTypeDetector.detectMimeType(part.filename(), is) //Read the stream to detect mime type
        MediaType.parse(realMimeType) match
          case mt if mt != part.contentType() =>
            actionLogger.error(s"File ${part.filename()} has content type ${part.contentType()} but actual content type is $mt")
            //This should be uncommented to enforce content type checking
//            throw new IllegalArgumentException(s"File ${part.filename()} has content type ${part.contentType()} but actual content type is $mt")
          case _ => //All good

        if !Files.exists(multipartUploadLocation) then multipartUploadLocation.toFile.mkdirs()
        val filePath = multipartUploadLocation.resolve(part.filename())
        Files.copy(part.content().toInputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
        filePath.toFile
      finally {
        is.close()
      }

  /**
   * Find the uploaded files with validation. All files must pass the validation or else an exception is returned
   * @param validatorFn
   * @return
   */
  def findFiles(validatorFn: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean): Try[List[MultipartFile]] =
    Try:
      val fileTries: Seq[Try[MultipartFile]] =
        for {
          name <- names
          part <- aggMultipart.fields(name).asScala
          if part.filename() != null && !part.content().isEmpty
        } yield saveFileTo(name, part, validatorFn).map(file => MultipartFile.of(name, part.filename(), file))
      fileTries.map(_.get).toList

  /**
   * Find a file using a predicate function
   * @param predicate
   * @return
   */
  def findFile(predicate: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean ): Try[MultipartFile] =
    findFiles(predicate).map(_.head)

  /**
   * Find a file using the form name
   * @param formNameRegex - this is the alias of findFileOfFormName
   * @return
   */
  @deprecated("Use findFile with a predicate function instead")
  def findFile(formNameRegex: String): Option[MultipartFile] =
    findFileOfFormName(formNameRegex)

  /**
   * Find a file using the form name
   * @param formNameRegex
   * @return
   */
  @deprecated("Use findFile with a predicate function instead")
  def findFileOfFormName(formNameRegex: String): Option[MultipartFile] =
    files.find(file => file.name.matches(formNameRegex) && file.file().length() > 0)

  /**
   * Find a file using the actual loaded filename
   * @param fileNameRegex
   * @return
   */
  @deprecated("Use findFile with a predicate function instead")
  def findFileOfFileName(fileNameRegex: String): Option[MultipartFile] =
    files.find(file => file.filename().matches(fileNameRegex) && file.file().length() > 0)