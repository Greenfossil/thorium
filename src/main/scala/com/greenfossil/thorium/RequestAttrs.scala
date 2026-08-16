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

import org.slf4j.LoggerFactory

import java.time.{Duration, ZoneId}

object RequestAttrs:
  import io.netty.util.AttributeKey
  val TZ = AttributeKey.valueOf[ZoneId]("tz")
  val Session = AttributeKey.valueOf[Session]("session")
  val Flash = AttributeKey.valueOf[Flash]("flash")
  val Config = AttributeKey.valueOf[Configuration]("config")
  val Request = AttributeKey.valueOf[Request]("request")
  val CSRFToken = AttributeKey.valueOf[String]("csrf-token")
  val RecaptchaResponse  = AttributeKey.valueOf[Recaptcha]("recaptcha-response")

  private[thorium] val resourceLogger = LoggerFactory.getLogger("com.greenfossil.thorium.resource")

  /**
   * A managed resource entry — holds the AutoCloseable resource with metadata
   * for logging and lease monitoring. Closed in LIFO order after the HTTP
   * response is fully written.
   *
   * @param resource the AutoCloseable to close
   * @param label human-readable identity for logging (e.g. "db-conn")
   * @param registeredAtNanos System.nanoTime() at registration
   * @param maxLease if non-null, a WARN is logged when the resource is held
   *                 longer than this duration
   */
  final class ManagedResource(
      val resource: AutoCloseable,
      val label: String,
      val registeredAtNanos: Long,
      val maxLease: Duration
  ):
    def close(): Unit =
      val duration = Duration.ofNanos(System.nanoTime() - registeredAtNanos)
      if maxLease != null && duration.compareTo(maxLease) > 0 then
        resourceLogger.warn(s"Resource [$label] held for ${duration.humanize} (maxLease=${maxLease.humanize})")
      else
        resourceLogger.debug(s"Resource [$label] closed after ${duration.humanize}")
      try resource.close()
      catch case _: Throwable => ()

  val ManagedResources = AttributeKey.valueOf[java.util.List[ManagedResource]]("managed-resources")