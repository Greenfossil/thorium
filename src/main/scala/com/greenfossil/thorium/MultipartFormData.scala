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

  def bodyPart: Seq[AggregatedBodyPart] = aggMultipart.bodyParts().asScala.toSeq

  def names() = aggMultipart.names().asScala

  lazy val asFormUrlEncoded: FormUrlEndcoded =
    val xs = for {
      name <- names()
      part <- aggMultipart.fields(name).asScala
      if part.contentType().is(MediaType.PLAIN_TEXT)
    } yield (name, part.content(Option(part.contentType().charset()).getOrElse(Charset.forName("UTF-8"))))
    FormUrlEndcoded(xs.toList.groupMap(_._1)(_._2))

  private def saveFileTo( part: AggregatedBodyPart): Option[File] =
    Try {
      if !Files.exists(multipartUploadLocation) then multipartUploadLocation.toFile.mkdirs()
      val filePath = multipartUploadLocation.resolve(part.filename())
      val is: InputStream = part.content().toInputStream
      Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING)
      is.close()
      filePath.toFile
    }.toOption

  lazy val files: List[MultipartFile] =
    for {
      name <- names().toList
      part <- aggMultipart.fields(name).asScala
      if part.filename() != null && !part.content().isEmpty
      file <- saveFileTo(part)
    } yield  MultipartFile.of(name, part.filename(), file)

  /**
   *
   * @param fileNameRegex
   * @return
   */
  def findFile(fileNameRegex: String): Option[MultipartFile] =
    files.find(file => file.name.matches(fileNameRegex) && file.file().length() > 0)