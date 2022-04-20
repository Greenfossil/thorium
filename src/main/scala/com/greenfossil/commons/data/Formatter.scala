package com.greenfossil.commons.data

import java.sql.Timestamp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

trait Formatter[T] {

  val tpe: String

  /**
   * The expected format of `Any`.
   */
  val format: Option[(String, Seq[Any])] = None

  /**
   * Binds this field, i.e. constructs a concrete value from submitted data.
   *
   * @param key the field key
   * @param data the submitted data
   * @return Either a concrete value of type T or a set of error if the binding failed.
   */
  def bind(key: String, data: Map[String, Seq[String]]): Either[Seq[FormError], T]

  override def toString: String = s"Binder: ${tpe}"
}

/** This object defines several default formatters. */
object Formatter {

  /**
   * Default formatter for the `String` type.
   */
  def checkRequiredFormat: Formatter[String] = new Formatter[String] {

    override val tpe: String = "String"

    override def bind(key: String, data: Map[String, Seq[String]]) =
      data
        .get(key)
        .flatMap(_.headOption)
        .toRight(Seq(FormError(key, "error.required", Nil)))
  }

  /**
   * Default formatter for the `Char` type.
   */
  def charFormat: Formatter[Char] = new Formatter[Char] {

    override val tpe: String = "Char"

    override def bind(key: String, data: Map[String, Seq[String]]) =
      data
        .get(key)
        .flatMap(_.headOption)
        .filter(s => s.length == 1 && s != " ")
        .map(s => Right(s.charAt(0)))
        .getOrElse(Left(Seq(FormError(key, "error.required", Nil))))
  }

  /**
   * Helper for formatters binders
   * @param parse Function parsing a String value into a T value, throwing an exception in case of failure
   * @param errArgs Error to set in case of parsing failure
   * @param key Key name of the field to parse
   * @param data Field data
   */
  def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, Seq[String]]): Either[Seq[FormError], T] = {
    checkRequiredFormat
      .bind(key, data)
      .flatMap { s =>
        scala.util.control.Exception
          .allCatch[T]
          .either(parse(s))
          .left
          .map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }

  private def numberFormatter[T](convert: String => T, real: Boolean = false): Formatter[T] = {

    val (formatString, errorString) = if real then ("format.real", "error.real") else ("format.numeric", "error.number")

    new Formatter[T] {
      override val tpe: String = "Number"

      override val format = Some(formatString -> Nil)

      override def bind(key: String, data: Map[String, Seq[String]]) =
        parsing(convert, errorString, Nil)(key, data)

    }
  }

  /**
   * Default formatter for the `Long` type.
   */
  def longFormat: Formatter[Long] = numberFormatter(_.toLong)

  /**
   * Default formatter for the `Int` type.
   */
  def intFormat: Formatter[Int] = numberFormatter(_.toInt)

  /**
   * Default formatter for the `Short` type.
   */
  def shortFormat: Formatter[Short] = numberFormatter(_.toShort)

  /**
   * Default formatter for the `Byte` type.
   */
  def byteFormat: Formatter[Byte] = numberFormatter(_.toByte)

  /**
   * Default formatter for the `Float` type.
   */
  def floatFormat: Formatter[Float] = numberFormatter(_.toFloat, real = true)

  /**
   * Default formatter for the `Double` type.
   */
  def doubleFormat: Formatter[Double] = numberFormatter(_.toDouble, real = true)

  /**
   * Default formatter for the `BigDecimal` type.
   */
  def bigDecimalFormat(precision: Option[(Int, Int)]): Formatter[BigDecimal] = new Formatter[BigDecimal] {

    override val tpe: String = "BigDecimal"

    override val format = Some(("format.real", Nil))

    override def bind(key: String, data: Map[String, Seq[String]]) = {
      checkRequiredFormat
        .bind(key, data)
        .flatMap { s =>
          scala.util.control.Exception
            .allCatch[BigDecimal]
            .either {
              val bd = BigDecimal(s)
              precision
                .map{
                  case (p, s) =>
                    if (bd.precision - bd.scale > p - s) {
                      throw new java.lang.ArithmeticException("Invalid precision")
                    }
                    bd.setScale(s)
                }
                .getOrElse(bd)
            }
            .left
            .map { e =>
              Seq(
                precision match {
                  case Some((p, s)) => FormError(key, "error.real.precision", Seq(p, s))
                  case None         => FormError(key, "error.real", Nil)
                }
              )
            }
        }
    }

  }

  /**
   * Default formatter for the `BigDecimal` type with no precision
   */
  val bigDecimalFormat: Formatter[BigDecimal] = bigDecimalFormat(None)

  /**
   * Default formatter for the `Boolean` type.
   */
  def booleanFormat: Formatter[Boolean] = new Formatter[Boolean] {

    override val tpe: String = "Boolean"

    override val format = Some(("format.boolean", Nil))

    override def bind(key: String, data: Map[String, Seq[String]]) =
      Right(data.getOrElse(key, Seq("false")).head).flatMap {
        case "true"  => Right(true)
        case "false" => Right(false)
        case _       => Left(Seq(FormError(key, "error.boolean", Nil)))
      }

  }

  import java.util.{Date, TimeZone}

  /**
   * Formatter for the `java.util.Date` type.
   *
   * @param pattern a date pattern, as specified in `format.DateTimeFormatter`.
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def dateFormat(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Formatter[Date] = new Formatter[Date] {

    override val tpe: String = "java.util.Date"

    val javaTimeZone = timeZone.toZoneId
    val formatter    = DateTimeFormatter.ofPattern(pattern)

    def dateParse(data: String) = { //FIXME
      val instant: Instant = ??? // PlayDate.parse(data, formatter).toZonedDateTime(ZoneOffset.UTC)
      Date.from(instant)
    }

    override val format = Some(("format.date", Seq(pattern)))

    override def bind(key: String, data: Map[String, Seq[String]]) =
      parsing(dateParse, "error.date", Nil)(key, data)

  }

  /**
   * Default formatter for the `java.util.Date` type with pattern `yyyy-MM-dd`.
   */
  val dateFormat: Formatter[Date] = dateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `java.sql.Date` type.
   *
   * @param pattern a date pattern as specified in `DateTimeFormatter`.
   */
  def sqlDateFormat(pattern: String): Formatter[java.sql.Date] = new Formatter[java.sql.Date] {

    override val tpe: String = "java.sql.Date"

    private val dateFormatter: Formatter[LocalDate] = localDateFormat(pattern)

    override val format = Some(("format.date", Seq(pattern)))

    override def bind(key: String, data: Map[String, Seq[String]]) =
      dateFormatter.bind(key, data).map(d => java.sql.Date.valueOf(d))

  }

  /**
   * Default formatter for `java.sql.Date` type with pattern `yyyy-MM-dd`.
   */
  val sqlDateFormat: Formatter[java.sql.Date] = sqlDateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `java.sql.Timestamp` type.
   *
   * @param pattern a date pattern as specified in `DateTimeFormatter`.
   * @param timeZone the `java.util.TimeZone` to use for parsing and formatting
   */
  def sqlTimestampFormat(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Formatter[java.sql.Timestamp] =
    new Formatter[java.sql.Timestamp] {

      override val tpe: String = "java.sql.Timestamp"

      private val formatter = DateTimeFormatter.ofPattern(pattern).withZone(timeZone.toZoneId)

      private def timestampParse(data: String) =
        if pattern.isEmpty
        then java.sql.Timestamp.valueOf(data)
        else java.sql.Timestamp.valueOf(LocalDateTime.parse(data, formatter))

      override val format = Some(("format.timestamp", Seq(pattern)))

      override def bind(key: String, data: Map[String, Seq[String]]): Either[Seq[FormError], Timestamp] =
        parsing(timestampParse, "error.timestamp", Nil)(key, data)

    }

  /**
   * Default formatter for `java.sql.Timestamp` type with pattern `yyyy-MM-dd HH:mm:ss`.
   */
  val sqlTimestampFormat: Formatter[java.sql.Timestamp] = sqlTimestampFormat("")

  /**
   * Formatter for the `LocalDate` type.
   *
   * @param pattern a date pattern as specified in `format.DateTimeFormatter`.
   */
  def localDateFormat(pattern: String): Formatter[LocalDate] = new Formatter[LocalDate] {

    override val tpe: String = "LocalDate"

    val formatter = DateTimeFormatter.ofPattern(pattern)

    def localDateParse(data: String) = LocalDate.parse(data, formatter)

    override val format = Some(("format.date", Seq(pattern)))

    override def bind(key: String, data: Map[String, Seq[String]]) =
      parsing(localDateParse, "error.date", Nil)(key, data)

  }

  /**
   * Default formatter for `LocalDate` type with pattern `yyyy-MM-dd`.
   */
  val localDateFormat: Formatter[LocalDate] = localDateFormat("yyyy-MM-dd")

  /**
   * Formatter for the `LocalDateTime` type.
   *
   * @param pattern a date pattern as specified in `format.DateTimeFormatter`.
   * @param zoneId the `ZoneId` to use for parsing and formatting
   */
  def localDateTimeFormat(pattern: String, zoneId: ZoneId = ZoneId.systemDefault()): Formatter[LocalDateTime] =

    new Formatter[LocalDateTime] {

      override val tpe: String = "LocalDateTime"

      val formatter =
        if pattern.isEmpty
        then DateTimeFormatter.ISO_LOCAL_DATE_TIME
        else DateTimeFormatter.ofPattern(pattern).withZone(zoneId)

      def localDateTimeParse(data: String) = LocalDateTime.parse(data, formatter)

      override val format = Some(("format.localDateTime", Seq(pattern)))

      override def bind(key: String, data: Map[String, Seq[String]]) =
        parsing(localDateTimeParse, "error.localDateTime", Nil)(key, data)

    }

  /**
   * Default formatter for `LocalDateTime` type with pattern `yyyy-MM-dd`.
   */
  val localDateTimeFormat: Formatter[LocalDateTime] =
    localDateTimeFormat("")

  /**
   * Formatter for the `LocalTime` type.
   *
   * @param pattern a date pattern as specified in `format.DateTimeFormatter`.
   */
  def localTimeFormat(pattern: String): Formatter[LocalTime] = new Formatter[LocalTime] {

    override val tpe: String = "LocalTime"

    val formatter: DateTimeFormatter =
      if pattern.isEmpty
      then DateTimeFormatter.ISO_LOCAL_TIME
      else DateTimeFormatter.ofPattern(pattern)

    def localTimeParse(data: String): LocalTime =  LocalTime.parse(data, formatter)

    override val format = Some(("format.localTime", Seq(pattern)))

    override def bind(key: String, data: Map[String, Seq[String]]) =
      parsing(localTimeParse, "error.localTime", Nil)(key, data)

  }

  /**
   * Default formatter for `LocalTime` type with pattern `HH:mm:ss`.
   */
  val localTimeFormat: Formatter[LocalTime] = localTimeFormat("")

  def yearMonthFormat(pattern: String): Formatter[YearMonth] = new Formatter[YearMonth] {
    override val tpe: String = "YearMonth"

    val formatter = DateTimeFormatter.ofPattern(pattern)

    def yearMonthParse(data: String): YearMonth =
      if pattern.isEmpty then YearMonth.parse(data) else YearMonth.parse(data, formatter)

    override def bind(key: String, data: Map[String, Seq[String]]) =
      parsing(yearMonthParse, "error.yearMonth", Nil)(key, data)
  }

  val yearMonthFormat: Formatter[YearMonth] = yearMonthFormat("")

  /**
   * Default formatter for the `java.util.UUID` type.
   */
  def uuidFormat: Formatter[UUID] = new Formatter[UUID] {

    override val tpe: String = "java.util.UUID"

    override val format = Some(("format.uuid", Nil))

    override def bind(key: String, data: Map[String, Seq[String]]) =
      parsing(UUID.fromString, "error.uuid", Nil)(key, data)

  }
  
}