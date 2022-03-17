package com.greenfossil.webserver

import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.common.multipart.{AggregatedBodyPart, AggregatedMultipart}

import java.nio.charset.Charset

case class MultipartFormData(aggMultipart: AggregatedMultipart) {
  import scala.jdk.CollectionConverters.*

  def bodyPart: Seq[AggregatedBodyPart] = aggMultipart.bodyParts().asScala.toSeq

  def names() = aggMultipart.names().asScala

  //TODO - need to be deterministic about the content type - form or file/bindary/octet stream etc.
  def asFormUrlEncoded: Map[String, Seq[String]] =
    val xs = for {
      name <- names()
      part <- aggMultipart.fields(name).asScala
      if part.contentType().is(MediaType.PLAIN_TEXT)
    } yield (name, part.content(Option(part.contentType().charset()).getOrElse(Charset.forName("UTF-8"))))
    xs.toList.groupMap(_._1)(_._2)

  /**
   * Seq(name, filename, content-type, content)
   * @return
   */
  case class TemporaryFile(name: String, filename: String, contentType: MediaType, part: AggregatedBodyPart)

  def files: List[TemporaryFile] =
    val xs = for {
      name <- names()
      part <- aggMultipart.fields(name).asScala
      //      if part.headers().get(HttpHeaderNames.CONTENT_TRANSFER_ENCODING, "").equals("binary")
      if part.filename() != null
    } yield  TemporaryFile(name, part.filename(), part.contentType(), part)
    xs.toList

}