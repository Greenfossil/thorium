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

inline def text(minLength: Int, maxLength: Int, trim: Boolean): Field[String] = 
  val _text = if trim then text.transform[String](_.trim) else text
  (minLength, maxLength)  match {
    case (min, Int.MaxValue) => _text.verifying(Constraints.minLength(min))
    case (0, max)            => _text.verifying(Constraints.maxLength(max))
    case (min, max)          => _text.verifying(Constraints.minLength(min), Constraints.maxLength(max))
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

inline def uuid =
  Field.of[java.util.UUID]

inline def checked(msg: String) =
  Field.of[Boolean].verifying(msg, _ == true)

inline def default[A](mapping: Field[A], defaultValue: A): Field[A] =
//Need to initialize value to defaultValue
  MappingField[A, A](tpe = "#", value = Option(defaultValue), delegate =  mapping, a => 
    println("a " + a)
    Option(a).getOrElse(defaultValue)
  )

inline def ignored[A](value: A): Field[A] =
  Field.of[A].transform[A](_ => value)

inline def tuple[A <: Tuple](nameValueTuple: A): Field[FieldTypeExtractor[A]] =
  Field.of[FieldTypeExtractor[A]].mappings(toNamedFieldTuple(nameValueTuple), null)

inline def mapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Field[A] =
  Field.of[A].mappings(toNamedFieldTuple(nameValueTuple), m)

inline def optional[A]:Field[Option[A]] =
  Field.of[Option[A]]

inline def optional[A](field: Field[A]): Field[Option[A]] =
  OptionalField[A]("?", elemField = field).asInstanceOf[Field[Option[A]]]

inline def optionalTuple[A <: Tuple](nameValueTuple: A): OptionalField[FieldTypeExtractor[A]] =
  Field.of[Option[FieldTypeExtractor[A]]]
    .mappings(toNamedFieldTuple(nameValueTuple), null)
    .asInstanceOf[OptionalField[FieldTypeExtractor[A]]]

inline def optionalMapping[A](using m: Mirror.ProductOf[A])
                             (nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): OptionalField[A] =
  val elemField = mapping[A](nameValueTuple)
  OptionalField(tpe = "?", elemField = elemField).asInstanceOf[OptionalField[A]]

inline def seq[A] =
  Field.of[Seq[A]]

inline def repeatedTuple[A <: Tuple](nameValueTuple: A) =
  val elemField = tuple[A](nameValueTuple)
  SeqField(tpe = "[Seq", elemField = elemField).asInstanceOf[Field[Seq[FieldTypeExtractor[A]]]]

inline def repeatedMapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]) =
  new SeqField[A](tpe="[Seq", elemField = mapping[A](nameValueTuple)).asInstanceOf[Field[Seq[A]]]

//Collection
inline def indexedSeq[A] =
  Field.of[IndexedSeq[A]]

inline def list[A] =
  Field.of[List[A]]

inline def list[A](a: Field[A]): Field[List[A]] =
  ???

inline def set[A] =
  Field.of[Set[A]]

inline def vector[A] = Field.of[Vector[A]]