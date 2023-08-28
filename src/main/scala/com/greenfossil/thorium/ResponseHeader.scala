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