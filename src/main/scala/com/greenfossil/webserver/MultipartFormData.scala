package com.greenfossil.webserver

import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.common.multipart.{AggregatedBodyPart, AggregatedMultipart}

import java.io.{File, InputStream}
import java.nio.charset.Charset
import scala.util.Try

case class MultipartFormData(aggMultipart: AggregatedMultipart):
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
  case class TemporaryFile(name: String, filename: String, contentType: MediaType, part: AggregatedBodyPart) {
    import java.nio.file.*

    private def copyInputStreamToPath(is: InputStream, path: Path): File =
      Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING)
      is.close()
      path.toFile

    def inputStream: InputStream = part.content.toInputStream

    def saveFileTo(pathStr: String): Try[File] = saveFileTo(pathStr, filename, false)

    def saveFileTo(dirPathStr: String, targetFilename: String, createDirectoryIfNotExist: Boolean): Try[File] = Try {
      val path = Paths.get(dirPathStr)
      val filePath = Paths.get(s"$dirPathStr/$targetFilename")
      if Files.exists(path)
      then copyInputStreamToPath(inputStream, filePath)
      else if createDirectoryIfNotExist then
        Files.createDirectory(path)
        copyInputStreamToPath(inputStream, filePath)
      else throw new FileSystemNotFoundException
    }

    @deprecated("to use input stream instead")
    def photoUrlOpt: Option[java.net.URL] = saveFileTo("/tmp").map(_.toURL).toOption

    def photoFileSizeGB: Double = part.content().length().toDouble / 1000 / 1000 / 1000
    def photoFileSizeMB: Double = part.content().length().toDouble / 1000 / 1000
    def photoFileSizeByte: Int = part.content().length()
  }

  def files: List[TemporaryFile] =
    val xs = for {
      name <- names()
      part <- aggMultipart.fields(name).asScala
      //      if part.headers().get(HttpHeaderNames.CONTENT_TRANSFER_ENCODING, "").equals("binary")
      if part.filename() != null
    } yield  TemporaryFile(name, part.filename(), part.contentType(), part)
    xs.toList

  /**
   *
   * @param fileNameRegex
   * @return
   */
  def findFile(fileNameRegex: String): Option[TemporaryFile] =
    files.find(file => file.name.matches(fileNameRegex))