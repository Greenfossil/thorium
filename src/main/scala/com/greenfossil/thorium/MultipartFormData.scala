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
import java.util.Locale
import scala.util.Try

/**
 * Strategy for generating the on-disk storage filename of an uploaded file.
 *
 * The original client-supplied filename is always preserved as metadata in the
 * returned `MultipartFile` (via `filename()`). The `StorageNameMode` only
 * controls the basename used on disk under the upload directory.
 *
 * - [[StorageNameMode.PureToken]]: a cryptographically random alphanumeric token
 *   (via `HMACUtil.randomAlphaNumericString`) + the validated extension.
 *   Example: `aB3xK9mP2nQ7x.scala`
 *
 * - [[StorageNameMode.PrefixedToken]]: a slugified prefix derived from the
 *   original basename (sanitized to `[A-Za-z0-9_-]`, truncated to 40 chars)
 *   followed by `-` + the token + extension.
 *   Example: `Sql-aB3xK9mP2nQ7x.scala`
 *   This gives `ls`-level traceability without a DB lookup while still
 *   guaranteeing uniqueness and unpredictability via the token.
 */
enum StorageNameMode:
  case PureToken
  case PrefixedToken

/**
 * Context passed to the application's upload validator.
 *
 * - `declaredType` is the HTTP `Content-Type` supplied by the client. It is
 *   UNTRUSTED metadata (OWASP File Upload Cheat Sheet) — useful as a hint but
 *   never proof of the actual file content.
 * - `detectedType` is the server-side MIME type detected from the file bytes
 *   (via `Files.probeContentType` / `MimeTypeDetector`). It is a detection
 *   hint, not absolute proof.
 * - `content` is a freshly opened `InputStream` over the uploaded bytes.
 *   Unlike the legacy 4-arg validator, this stream has not been consumed by
 *   MIME detection and may be read by the validator for magic-byte / parser
 *   checks. The framework closes it after the validator returns.
 *
 * The validator returns `true` to accept, `false` to reject (which causes an
 * `IllegalArgumentException` to be thrown by the framework).
 */
final case class UploadContext(
    fieldName: String,
    fileName: String,
    declaredType: MediaType,
    detectedType: MediaType,
    content: InputStream
)

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
   * Extract the lowercase extension (without leading dot) from a filename.
   * Returns `None` if there is no extension.
   */
  private def fileExtension(filename: String): Option[String] =
    val idx = filename.lastIndexOf('.')
    if idx <= 0 || idx == filename.length - 1 then None
    else Some(filename.substring(idx + 1).toLowerCase(Locale.ROOT))

  /**
   * Sanitize the original basename into a filesystem-safe slug for
   * [[StorageNameMode.PrefixedToken]].
   *
   * Rules:
   *   - strip the extension (the token will carry it)
   *   - replace any character outside `[A-Za-z0-9_-]` with `_`
   *   - truncate to 40 characters
   *   - return `""` if the result is empty (caller falls back to PureToken)
   */
  private def slugify(filename: String): String =
    val base = fileExtension(filename) match
      case Some(ext) => filename.substring(0, filename.length - ext.length - 1)
      case None      => filename
    val slug = base.toCharArray.map(c =>
      if (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
         (c >= '0' && c <= '9') || c == '_' || c == '-' then c else '_'
    ).mkString
    val truncated = if slug.length > 40 then slug.substring(0, 40) else slug
    truncated

  /**
   * Generate a server-side storage filename using
   * `HMACUtil.randomAlphaNumericString(16)` (cryptographically random,
   * filesystem-safe alphanumeric).
   *
   * - [[StorageNameMode.PureToken]]: `aB3xK9mP2nQ7x.scala`
   * - [[StorageNameMode.PrefixedToken]]: `Sql-aB3xK9mP2nQ7x.scala`
   *   Falls back to `PureToken` form when the slug is empty (e.g. the
   *   original basename is all non-ASCII).
   */
  private def generateStorageName(filename: String, mode: StorageNameMode): String =
    val ext  = fileExtension(filename)
    val token = HMACUtil.randomAlphaNumericString(16)
    mode match
      case StorageNameMode.PureToken =>
        ext.fold(token)(e => s"$token.$e")
      case StorageNameMode.PrefixedToken =>
        val slug = slugify(filename)
        if slug.isEmpty then
          ext.fold(token)(e => s"$token.$e")
        else
          ext.fold(s"$slug-$token")(e => s"$slug-$token.$e")

  /**
   * Core validation-and-save implementation. All public `findFile` /
   * `findFiles` overloads delegate here.
   *
   * Security pipeline (OWASP File Upload Cheat Sheet):
   *   1. Filename sanitization + path-traversal containment
   *   2. Extension allowlist (if `allowedExtensions` is non-empty)
   *   3. Server-side MIME detection (hint, not proof)
   *   4. Mismatch logged as warning — does NOT throw
   *   5. `validatorFn` is the authority; receives `UploadContext` with
   *      declared + detected types and a FRESH `InputStream`
   *   6. File written to final storage (client name or server-generated name)
   *
   * @param allowedExtensions empty set = skip the extension gate (preserves
   *                          the legacy "validatorFn is the only gate" contract)
   * @param storageNameMode   `None` = use the client filename verbatim (today's
   *                          behavior); `Some(mode)` = server-generated name
   * @param validatorFn       application-level authority
   */
  private def saveFileToValidated(
      fieldName: String,
      part: AggregatedBodyPart,
      allowedExtensions: Set[String],
      storageNameMode: Option[StorageNameMode],
      validatorFn: UploadContext => Boolean
  ): Try[MultipartFile] =
    Try:
      // --- 1. Filename sanitization ---
      val trimmedFilename = Option(part.filename()).map(_.trim).getOrElse("")
      if trimmedFilename.isBlank then
        throw new IllegalArgumentException("Empty or whitespace-only filename not allowed")
      if trimmedFilename == "." || trimmedFilename == ".." ||
         trimmedFilename.contains(java.io.File.separator) ||
         trimmedFilename.contains("/") || trimmedFilename.contains("\\") then
        throw new IllegalArgumentException(s"Invalid filename: $trimmedFilename")

      // Ensure upload dir exists
      if !Files.exists(multipartUploadLocation) then multipartUploadLocation.toFile.mkdirs()

      // --- 2. Extension allowlist (pre-filter; validatorFn is still the authority) ---
      val ext = fileExtension(trimmedFilename)
      if allowedExtensions.nonEmpty then
        ext match
          case Some(e) if !allowedExtensions.contains(e) =>
            throw new IllegalArgumentException(s"File extension '.$e' is not allowed")
          case None =>
            throw new IllegalArgumentException("File extension is required but none was provided")
          case _ => ()

      // --- 3. Server-side MIME detection (on a fresh stream) ---
      val detectionIs = part.content().toInputStream
      val realMimeType =
        try mimeTypeDetector.detectMimeType(trimmedFilename, detectionIs)
        finally detectionIs.close()
      val detectedType = MediaType.parse(realMimeType)
      val declaredType = part.contentType()
      if detectedType != declaredType then
        actionLogger.warn(
          s"File ${part.filename()} has declared content type $declaredType " +
          s"but detected content type is $detectedType — deferring to validator"
        )

      // --- 4. Validator (fresh stream; the InputStream fix) ---
      val validatorIs = part.content().toInputStream
      val ctx = UploadContext(fieldName, trimmedFilename, declaredType, detectedType, validatorIs)
      val accepted =
        try validatorFn(ctx)
        finally validatorIs.close()
      if !accepted then
        throw new IllegalArgumentException(
          s"File $trimmedFilename with content type $declaredType is not allowed"
        )

      // --- 5. Resolve storage path ---
      val (storageName, filePath) =
        storageNameMode match
          case Some(mode) =>
            val sName = generateStorageName(trimmedFilename, mode)
            val fPath = multipartUploadLocation.resolve(sName).normalize()
            if !fPath.startsWith(multipartUploadLocation.normalize()) then
              throw new IllegalArgumentException(s"Generated filename resolves outside upload directory: $sName")
            if Files.exists(fPath) && Files.isDirectory(fPath) then
              throw new IllegalArgumentException(s"Generated filename resolves to directory: $sName")
            (sName, fPath)
          case None =>
            val fPath = multipartUploadLocation.resolve(trimmedFilename).normalize()
            if !fPath.startsWith(multipartUploadLocation.normalize()) then
              throw new IllegalArgumentException(s"Invalid filename resolves outside upload directory: $trimmedFilename")
            if Files.exists(fPath) && Files.isDirectory(fPath) then
              throw new IllegalArgumentException(s"Invalid filename resolves to directory: $trimmedFilename")
            (trimmedFilename, fPath)

      // --- 6. Write to disk ---
      Files.copy(part.content().toInputStream, filePath, StandardCopyOption.REPLACE_EXISTING)

      // --- 7. Traceability log (only when the on-disk name differs from the original) ---
      if storageNameMode.isDefined then
        actionLogger.info(
          s"Uploaded '$trimmedFilename' stored as '$storageName' " +
          s"(field=$fieldName, declared=$declaredType, detected=$detectedType)"
        )

      // Original filename preserved as metadata; file() points to the on-disk path
      MultipartFile.of(fieldName, part.filename(), filePath.toFile)

  // ---------------------------------------------------------------------------
  // Legacy 4-arg API — signatures preserved exactly; delegates to the new
  // path so existing callers automatically get the InputStream fix.
  // ---------------------------------------------------------------------------

  /**
   * Find the uploaded files with validation. All files must pass the
   * validation or else an exception is returned.
   *
   * '''Behavior note:''' The `content` `InputStream` passed to `validatorFn`
   * is freshly opened — it has NOT been consumed by MIME detection.
   * Previously the stream was exhausted; validators that read `content`
   * now see the actual bytes.
   *
   * @param validatorFn Validation for security. Do not use
   *                    `(_, _, _, _) => true` as this bypasses the security check
   * @return If validatorFn returns false, this will return a Failure
   */
  def findFiles(validatorFn: (fieldName: String, fileName: String, contentType: MediaType, content: InputStream) => Boolean): Try[List[MultipartFile]] =
    findFiles(
      allowedExtensions = Set.empty,
      storageNameMode   = None,
      validatorFn       = (ctx: UploadContext) =>
        validatorFn(ctx.fieldName, ctx.fileName, ctx.declaredType, ctx.content)
    )

  /**
   * Find the uploaded file with validation. All files must pass the
   * validation or else an exception is returned.
   *
   * @param validatorFn Validation for security. Do not use
   *                    `(_, _, _, _) => true` as this bypasses the security check
   * @return If validatorFn returns false, this will return a Failure
   */
  def findFile(validatorFn: (fieldName: String, fileName: String, contentType: MediaType, content: InputStream) => Boolean): Try[MultipartFile] =
    findFiles(validatorFn).map(_.head)

  // ---------------------------------------------------------------------------
  // New API — UploadContext validator + optional extension allowlist
  // ---------------------------------------------------------------------------

  /**
   * Find the uploaded files with validation. All files must pass the
   * validation or else an exception is returned.
   *
   * The `UploadContext` exposes both the client-declared `Content-Type`
   * (untrusted) and the server-detected MIME type, plus a fresh
   * `InputStream` for content-level checks.
   *
   * @param allowedExtensions if non-empty, files whose extension is not in
   *                          this set are rejected before `validatorFn` runs.
   *                          Empty set = skip (delegate entirely to `validatorFn`)
   * @param validatorFn       application-level authority. Returns `true` to accept
   * @return `Failure` if any file fails validation
   */
  def findFiles(allowedExtensions: Set[String], validatorFn: UploadContext => Boolean): Try[List[MultipartFile]] =
    findFiles(allowedExtensions, None, validatorFn)

  /**
   * Find the uploaded file with validation. See
   * [[findFiles(allowedExtensions:Set,validatorFn*]] for semantics.
   */
  def findFile(allowedExtensions: Set[String], validatorFn: UploadContext => Boolean): Try[MultipartFile] =
    findFiles(allowedExtensions, validatorFn).map(_.head)

  // ---------------------------------------------------------------------------
  // New API — UploadContext validator + extension allowlist + server-generated filename
  // ---------------------------------------------------------------------------

  /**
   * Find the uploaded files with validation and server-generated storage
   * filenames.
   *
   * When `storageNameMode` is provided, the on-disk filename is replaced with
   * a cryptographically random token (via `HMACUtil.randomAlphaNumericString`)
   * while the original client filename is preserved as
   * `MultipartFile.filename()` metadata. An info-level log line records the
   * original→storage name mapping for traceability.
   *
   * @param allowedExtensions extension allowlist (empty = skip)
   * @param storageNameMode   `PureToken` or `PrefixedToken`
   * @param validatorFn       application-level authority
   */
  def findFiles(allowedExtensions: Set[String], storageNameMode: StorageNameMode, validatorFn: UploadContext => Boolean): Try[List[MultipartFile]] =
    findFiles(allowedExtensions, Some(storageNameMode), validatorFn)

  /**
   * Find the uploaded file with validation and server-generated storage
   * filename. See
   * [[findFiles(allowedExtensions:Set,storageNameMode:com.greenfossil.thorium.StorageNameMode,validatorFn*]]
   * for semantics.
   */
  def findFile(allowedExtensions: Set[String], storageNameMode: StorageNameMode, validatorFn: UploadContext => Boolean): Try[MultipartFile] =
    findFiles(allowedExtensions, storageNameMode, validatorFn).map(_.head)

  // ---------------------------------------------------------------------------
  // Internal: single implementation used by all public overloads
  // ---------------------------------------------------------------------------

  private def findFiles(
      allowedExtensions: Set[String],
      storageNameMode: Option[StorageNameMode],
      validatorFn: UploadContext => Boolean
  ): Try[List[MultipartFile]] =
    Try:
      val fileTries: Seq[Try[MultipartFile]] =
        for
          name <- names
          part <- aggMultipart.fields(name).asScala
          if part.filename() != null && !part.content().isEmpty
        yield saveFileToValidated(name, part, allowedExtensions, storageNameMode, validatorFn)
      fileTries.map(_.get).toList

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