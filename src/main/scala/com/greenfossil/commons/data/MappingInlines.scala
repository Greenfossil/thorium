package com.greenfossil.commons.data

import com.greenfossil.commons.data.Mapping.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}

import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}
import scala.deriving.Mirror

/*
 * https://www.playframework.com/documentation/2.8.x/api/scala/play/api/data/Forms$.html
 */
//Numeric
inline def boolean =
  Mapping.of[Boolean]

inline def byteNumber =
  Mapping.of[Byte]

inline def byteNumber(min: Byte = Byte.MinValue, max: Byte = Byte.MaxValue, strict: Boolean = false) =
  Mapping.of[Byte].verifying(Constraints.min(min, strict), Constraints.max(max, strict))

inline def shortNumber =
  Mapping.of[Short]

inline def shortNumber(min: Short = Short.MinValue, max: Short = Short.MinValue, strict: Boolean = false) =
  Mapping.of[Short].verifying(Constraints.min[Short](min, strict), Constraints.max[Short](max, strict))

inline def number =
  Mapping.of[Int]

inline def number(min:Int, max:Int) =
  Mapping.of[Int].verifying(Constraints.min(min), Constraints.max(max))

inline def longNumber =
  Mapping.of[Long]

inline def longNumber(min: Long = Long.MinValue, max: Long = Long.MaxValue, strict: Boolean = false) =
  Mapping.of[Long].verifying(Constraints.min[Long](min, strict), Constraints.max[Long](max, strict))

inline def double =
  Mapping.of[Double]

inline def float =
  Mapping.of[Float]

inline def bigDecimal =
  Mapping.of[BigDecimal]

inline def bigDecimal(precision: Int, scale: Int) =
  Mapping.of[BigDecimal].verifying(Constraints.precision(precision, scale))

//Text
inline def char =
  Mapping.of[Char]

inline def text:Mapping[String] =
  Mapping.of[String]

inline def text(minLength: Int, maxLength: Int, trim: Boolean): Mapping[String] =
  val _text = if trim then text.transform[String](_.trim) else text
  (minLength, maxLength)  match {
    case (min, Int.MaxValue) => _text.verifying(Constraints.minLength(min))
    case (0, max)            => _text.verifying(Constraints.maxLength(max))
    case (min, max)          => _text.verifying(Constraints.minLength(min), Constraints.maxLength(max))
  }

inline def nonEmptyText =
  Mapping.of[String].verifying(Constraints.nonEmpty)

inline def nonEmptyText(minLength: Int = 0, maxLength: Int = Int.MaxValue) =
  Mapping.of[String].verifying(Constraints.minLength(minLength), Constraints.maxLength(maxLength))

inline def email =
  Mapping.of[String].verifying(Constraints.emailAddress)

//Temporal
inline def date =
  Mapping.of[java.util.Date]

inline def dateUsing(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =
  Mapping.of[java.util.Date](Formatter.dateFormat(pattern, timeZone))

inline def localDate =
  Mapping.of[LocalDate]

inline def localDateUsing(pattern: String) =
  Mapping.of[LocalDate](Formatter.localDateFormat(pattern))

inline def localDateTime =
  Mapping.of[LocalDateTime]

inline def localDateTimeUsing(pattern: String) =
  Mapping.of[LocalDateTime](Formatter.localDateTimeFormat(pattern))

inline def localTime =
  Mapping.of[LocalTime]

inline def localTimeUsing(pattern: String) =
  Mapping.of[LocalTime](Formatter.localTimeFormat(pattern))

inline def yearMonth =
  Mapping.of[YearMonth]

inline def yearMonthUsing(pattern: String) =
  Mapping.of[YearMonth](Formatter.yearMonthFormat(pattern))

inline def sqlDate =
  Mapping.of[java.sql.Date]

inline def sqlDateUsing(pattern: String) =
  Mapping.of[java.sql.Date]

inline def sqlTimestamp =
  Mapping.of[java.sql.Timestamp]

inline def sqlTimestampUsing(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =
  Mapping.of[java.sql.Timestamp]

inline def uuid =
  Mapping.of[java.util.UUID]

inline def checked(msg: String) =
  Mapping.of[Boolean].verifying(msg, _ == true)

inline def default[A](mapping: Mapping[A], defaultValue: A): Mapping[A] =
  DelegateMapping[A, A](tpe = "#", value = Option(defaultValue), delegate =  mapping, a =>
    Option(a).getOrElse(defaultValue)
  )

inline def ignored[A](value: A): Mapping[A] =
  Mapping.of[A].transform[A](_ => value)

inline def tuple[A <: Tuple](nameValueTuple: A): Mapping[FieldTypeExtractor[A]] =
  Mapping.of[FieldTypeExtractor[A]].mappings(toNamedFieldTuple(nameValueTuple), null)

inline def mapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Mapping[A] =
  Mapping.of[A].mappings(toNamedFieldTuple(nameValueTuple), m)

inline def optional[A]:Mapping[Option[A]] =
  Mapping.of[Option[A]]

inline def optional[A](field: Mapping[A]): Mapping[Option[A]] =
  OptionalMapping[A]("?", elemField = field).asInstanceOf[Mapping[Option[A]]]

inline def optionalTuple[A <: Tuple](nameValueTuple: A): Mapping[Option[FieldTypeExtractor[A]]] =
  Mapping.of[Option[FieldTypeExtractor[A]]]
    .mappings(toNamedFieldTuple(nameValueTuple), null)
    .asInstanceOf[Mapping[Option[FieldTypeExtractor[A]]]]

inline def optionalMapping[A](using m: Mirror.ProductOf[A])
                             (nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Mapping[Option[A]] =
  val elemField = mapping[A](nameValueTuple)
  OptionalMapping(tpe = "?", elemField = elemField).asInstanceOf[Mapping[Option[A]]]

inline def seq[A] =
  Mapping.of[Seq[A]]

inline def repeatedTuple[A <: Tuple](nameValueTuple: A) =
  val elemField = tuple[A](nameValueTuple)
  SeqMapping(tpe = "[Seq", elemField = elemField).asInstanceOf[Mapping[Seq[FieldTypeExtractor[A]]]]

inline def repeatedMapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]) =
  new SeqMapping[A](tpe="[Seq", elemField = mapping[A](nameValueTuple)).asInstanceOf[Mapping[Seq[A]]]

//Collection
inline def indexedSeq[A] =
  Mapping.of[IndexedSeq[A]]

inline def list[A] =
  Mapping.of[List[A]]

inline def list[A](a: Mapping[A]): Mapping[List[A]] =
  ???

inline def set[A] =
  Mapping.of[Set[A]]

inline def vector[A] = Mapping.of[Vector[A]]