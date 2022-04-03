package com.greenfossil.commons.data

import Form.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}

import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}
import scala.deriving.Mirror

/*
 * https://www.playframework.com/documentation/2.8.x/api/scala/play/api/data/Forms$.html
 */
//Numeric
inline def boolean =
  Field.of[Boolean]

inline def byteNumber =
  Field.of[Byte]

inline def byteNumber(min: Byte = Byte.MinValue, max: Byte = Byte.MaxValue, strict: Boolean = false) =
  Field.of[Byte].verifying(Constraints.min(min, strict), Constraints.max(max, strict))

inline def shortNumber =
  Field.of[Short]

inline def shortNumber(min: Short = Short.MinValue, max: Short = Short.MinValue, strict: Boolean = false) =
  Field.of[Short].verifying(Constraints.min[Short](min, strict), Constraints.max[Short](max, strict))

inline def number =
  Field.of[Int]

inline def number(min:Int, max:Int) =
  Field.of[Int].verifying(Constraints.min(min), Constraints.max(max))

inline def longNumber =
  Field.of[Long]

inline def longNumber(min: Long = Long.MinValue, max: Long = Long.MaxValue, strict: Boolean = false) =
  Field.of[Long].verifying(Constraints.min[Long](min, strict), Constraints.max[Long](max, strict))

inline def double =
  Field.of[Double]

inline def float =
  Field.of[Float]

inline def bigDecimal =
  Field.of[BigDecimal]

inline def bigDecimal(precision: Int, scale: Int) =
  Field.of[BigDecimal].verifying(Constraints.precision(precision, scale))

//Text
inline def char =
  Field.of[Char]

inline def text:Field[String] =
  Field.of[String]

inline def text(minLength: Int, maxLength: Int, trim: Boolean): Field[String] = (minLength, maxLength)  match {
  case (min, Int.MaxValue) => text.verifying(Constraints.minLength(min))
  case (0, max)            => text.verifying(Constraints.maxLength(max))
  case (min, max)          => text.verifying(Constraints.minLength(min), Constraints.maxLength(max))
}

inline def nonEmptyText =
  Field.of[String].verifying(Constraints.nonEmpty)

inline def nonEmptyText(minLength: Int = 0, maxLength: Int = Int.MaxValue) =
  Field.of[String].verifying(Constraints.minLength(minLength), Constraints.maxLength(maxLength))

inline def email =
  Field.of[String].verifying(Constraints.emailAddress)

//Temporal
inline def date =
  Field.of[java.util.Date]

inline def date(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =
  Field.of[java.util.Date](Formatter.dateFormat(pattern, timeZone))

inline def localDate =
  Field.of[LocalDate]

inline def localDate(pattern: String) =
  Field.of[LocalDate](Formatter.localDateFormat(pattern))

inline def localDateTime =
  Field.of[LocalDateTime]

inline def localDateTime(pattern: String) =
  Field.of[LocalDateTime](Formatter.localDateTimeFormat(pattern))

inline def localTime =
  Field.of[LocalTime]

inline def localTime(pattern: String) =
  Field.of[LocalTime](Formatter.localTimeFormat(pattern))

inline def yearMonth =
  Field.of[YearMonth]

inline def yearMonth(pattern: String) =
  Field.of[YearMonth](Formatter.yearMonthFormat(pattern))

inline def sqlDate =
  Field.of[java.sql.Date]

inline def sqlDate(pattern: String) =
  Field.of[java.sql.Date]

inline def sqlTimestamp =
  Field.of[java.sql.Timestamp]

inline def sqlTimestamp(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =
  Field.of[java.sql.Timestamp]

inline def tuple[A <: Tuple](nameValueTuple: A): Field[FieldTypeExtractor[A]] =
  new Field(tpe = Field.fieldType[A], binder = null, mappings = toNamedFieldTuple(nameValueTuple))

inline def mapping[A](using m: Mirror.ProductOf[A])(
  nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]
): Field[A] =
  new Field(tpe = Field.fieldType[A] + s"$m", binder = null,  mappings = Form.toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))

inline def optional[A] =
  val elemField = Field.of[A]
  type B = Option[A]
  new Field[B](tpe = Field.fieldType[B], null, mappings = elemField *: EmptyTuple)

inline def optionalTuple[A <: Tuple](nameValueTuple: A) =
  val elemField = new Field[A](tpe = Field.fieldType[A], binder = null , mappings = toNamedFieldTuple(nameValueTuple))
  type B = Option[FieldTypeExtractor[A]]
  new Field[B](tpe = Field.fieldType[B], binder = null, mappings = elemField *: EmptyTuple)

inline def optionalMapping[A](using m: Mirror.ProductOf[A])(
  nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]) =
  val elemField =  new Field[A](tpe = Field.fieldType[A] + s"$m", binder = null, mappings = Form.toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))
  type B = Option[A]
  new Field[B](tpe = Field.fieldType[B], binder = null, mappings = elemField *: EmptyTuple)

inline def seq[A] =
  val elemField = Field.of[A]
  type B = Seq[A]
  new Field[B](tpe = Field.fieldType[B], null, mappings = elemField *: EmptyTuple)

inline def repeatedMapping[A](using m: Mirror.ProductOf[A])(
  nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]) =
  val elemField =  new Field[A](tpe = Field.fieldType[A] + s"$m", binder = null, mappings = Form.toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))
  type B = Seq[A]
  new Field[B](tpe = Field.fieldType[B], binder = null, mappings = elemField *: EmptyTuple)

inline def repeatedTuple[A <: Tuple](nameValueTuple: A) =
  val elemField = new Field[A](tpe = Field.fieldType[A], binder = null, mappings = toNamedFieldTuple(nameValueTuple))
  type B = Seq[FieldTypeExtractor[A]]
  new Field[B](Field.fieldType[B], binder = null, mappings = elemField *: EmptyTuple)

inline def uuid =
  Field.of[java.util.UUID]

inline def checked(msg: String) =
  Field.of[Boolean].verifying(msg, _ == true)

inline def default[A](mapping: Field[A], value: A): Field[A] =
  ???

inline def ignored[A](value: A): Field[A] =
  Field.of[A](binder = Formatter.ignoredFormat(value))


//Collection
//inline def indexedSeq[A] =
//  Field.of[IndexedSeq[A]]
//
//inline def list[A] =
//  Field.of[List[A]]
//
//@deprecated("Use list[A]", "")
//inline def list[A](a: Field[A]): Field[List[A]] =
//  Field.of[List[A]]
//
//inline def set[A] =
//  Field.of[Set[A]]
//
//inline def vector[A] = Field.of[Vector[A]]