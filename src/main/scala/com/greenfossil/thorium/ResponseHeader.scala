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

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import scala.collection.immutable.TreeMap

object ResponseHeader:
  val basicDateFormatPattern = "EEE, dd MMM yyyy HH:mm:ss"
  val httpDateFormat: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern(basicDateFormatPattern + " 'GMT'")
      .withLocale(java.util.Locale.ENGLISH)
      .withZone(ZoneOffset.UTC)

  def apply(headers: Map[String, String]): ResponseHeader = apply(headers, null)

  private object CaseInsensitiveOrdered extends Ordering[String]:
    def compare(left: String, right: String): Int =
      left.compareToIgnoreCase(right)

  def apply(headers: Map[String, String], reasonPhrase: String): ResponseHeader =
    val ciHeaders = TreeMap[String, String]()(CaseInsensitiveOrdered) ++ headers
    new ResponseHeader(ciHeaders, Option(reasonPhrase))

case class ResponseHeader(headers: TreeMap[String, String], reasonPhrase:Option[String] = None)