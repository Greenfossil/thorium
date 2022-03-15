package com.greenfossil.webserver.data

import com.greenfossil.commons.json.JsValue

import java.time.*

object Field {
  import scala.compiletime.*

  inline def of[A]: Field[A] = Field(fieldType[A])

  inline def of[A](name: String): Field[A] = Field(fieldType[A], name = name)

  inline def fieldType[A]: String =
    inline erasedValue[A] match {
      case _: Int                => "Int"
      case _: String             => "String"
      case _: Long               => "Long"
      case _: Double             => "Double"
      case _: Float              => "Float"
      case _: Boolean            => "Boolean"
      case _: LocalDate          => "LocalDate"
      case _: LocalTime          => "LocalTime"
      case _: LocalDateTime      => "LocalDateTime"
      case _: java.sql.Date      => "SqlDate"
      case _: java.sql.Timestamp => "SqlTimestamp"
      case _: java.util.UUID     => "UUID"
      case _: Byte               => "Byte"
      case _: Short              => "Short"
      case _: BigDecimal         => "BigDecimal"
      case _: Char               => "Char"
      case _: Seq[a]             => "[" + fieldType[a]
      case _: Option[a]          => "?" + fieldType[a]
    }

  def toValueOf[A](tpe: String, value: Any): Option[A] = {
    if value == null then None
    else {
      val optValue = tpe match {
        case "Int" =>
          value match {
            case x: Int => Option(x)
            case s: String => s.toIntOption
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toIntOption)
            case xs: Option[_] => xs.flatMap(_.toString.toIntOption)
            case _ => None
          }

        case "Long" =>
          value match {
            case x: Long => Option(x)
            case s: String => s.toLongOption
            case s: BigDecimal /* This is to handle JsNumber type */ => Option(s.toLong)
            case xs: Option[_] => xs.flatMap(_.toString.toLongOption)
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toLongOption)
            case _ => None
          }

        case "String" =>
          value match {
            case s: String => Option(s)
            case xs: Option[_] => xs.map(_.toString)
            case xs: Seq[_] => xs.headOption.map(_.toString)
            case _ => None
          }

        case "Double" =>
          value match {
            case x: Double => Option(x)
            case s: String => s.toDoubleOption
            case xs: Option[_] => xs.flatMap(_.toString.toDoubleOption)
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toDoubleOption)
            case _ => None
          }

        case "Float" =>
          value match {
            case x: Float => Option(x)
            case s: String => s.toFloatOption
            case xs: Option[_] => xs.flatMap(_.toString.toFloatOption)
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toFloatOption)
            case _ => None
          }

        case "Boolean" =>
          value match {
            case x: Boolean => Option(x)
            case s: String => s.toBooleanOption
            case xs: Option[_] => xs.flatMap(_.toString.toBooleanOption)
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toBooleanOption)
            case _ => None
          }

        case "LocalDate" =>
          value match {
            case x: LocalDate => Option(x)
            case s: String => Option(LocalDate.parse(s))
            case xs: Option[_] => xs.flatMap(x => Option(LocalDate.parse(x.toString)))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(LocalDate.parse(x.toString)))
            case _ => None
          }

        case "LocalTime" =>
          value match {
            case x: LocalTime => Option(x)
            case s: String => Option(LocalTime.parse(s))
            case xs: Option[_] => xs.flatMap(x => Option(LocalTime.parse(x.toString)))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(LocalTime.parse(x.toString)))
            case _ => None
          }

        case "SqlDate" =>
          value match {
            case x: java.sql.Date => Option(x)
            case s: String => Option(java.sql.Date.valueOf(s))
            case xs: Option[_] => xs.flatMap(x => Option(java.sql.Date.valueOf(x.toString)))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(java.sql.Date.valueOf(x.toString)))
            case _ => None
          }

        case "SqlTimestamp" =>
          value match {
            case x: java.sql.Timestamp => Option(x)
            case s: String => Option(java.sql.Timestamp.valueOf(s))
            case xs: Option[_] => xs.flatMap(x => Option(java.sql.Timestamp.valueOf(x.toString)))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(java.sql.Timestamp.valueOf(x.toString)))
            case _ => None
          }

        case "LocalDateTime" =>
          value match {
            case x: LocalDateTime => Option(x)
            case s: String => Option(LocalDateTime.parse(s))
            case xs: Option[_] => xs.flatMap(x => Option(LocalDateTime.parse(x.toString)))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(LocalDateTime.parse(x.toString)))
            case _ => None
          }

        case "UUID" =>
          value match {
            case x: java.util.UUID => Option(x)
            case s: String => Option(java.util.UUID.fromString(s))
            case xs: Option[_] => xs.flatMap(x => Option(java.util.UUID.fromString(x.toString)))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(java.util.UUID.fromString(x.toString)))
            case _ => None
          }

        case "Byte" =>
          value match {
            case x: Byte => Option(x)
            case s: String => Option(s.toByte)
            case xs: Option[_] => xs.flatMap(x => Option(x.toString.toByte))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(x.toString.toByte))
            case _ => None
          }

        case "Short" =>
          value match {
            case x: Short => Option(x)
            case s: String => Option(s.toShort)
            case xs: Option[_] => xs.flatMap(x => Option(x.toString.toShort))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(x.toString.toShort))
            case _ => None
          }

        case "BigDecimal" =>
          value match {
            case x: BigDecimal => Option(x)
            case s: String => Option(BigDecimal(s))
            case xs: Option[_] => xs.flatMap(x => Option(BigDecimal(x.toString)))
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(BigDecimal(x.toString)))
            case _ => None
          }

        case "Char" =>
          value match {
            case x: Char => Option(x)
            case s: String => s.headOption
            case xs: Option[_] => xs.flatMap(_.toString.headOption)
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.headOption)
            case _ => None
          }

        case seq if seq.startsWith("[") =>
          value match {
            case xs: Seq[_] =>
              Option(xs.flatMap(x => toValueOf(seq.tail,x)))
            case xs: Option[_] =>
              xs.flatMap(x => toValueOf(seq, x))
            case s: String =>
              Option(Seq(toValueOf(seq.tail, s)))
          }
        case opt if opt.startsWith("?") =>
          value match {
            case opt: Option[_] => opt
            case xs: Seq[_] => Option(xs.flatMap(x => toValueOf(opt.tail,x)))
            case s: String => Option(Seq(toValueOf(opt.tail, s)))
          }
      }
      optValue.asInstanceOf[Option[A]]
    }
  }

}

case class Field[A](tpe: String, 
                    form: Form[_] = null,
                    name: String = null,
                    constraints:Seq[Constraint[A]] = Nil,
                    format: Option[(String, Seq[Any])] = None,
                    errors: Seq[FormError] = Nil,
                    value: Option[A] = None) extends ConstraintVerifier[Field, A](name, constraints) {

  def fill(newValue: A):Field[A] = copy(value = Option(newValue))

  def bind(value: Any): Field[A] = {
    val newValueOpt = value match {
      case js: JsValue => Field.toValueOf[A](tpe, js.asOpt[Any])
      case any => Field.toValueOf[A](tpe, any)
    }
    //Check constraints here
    newValueOpt match {
      case Some(value: A) =>
        val formErrors = applyConstraints(value)
        copy(value = newValueOpt, errors = formErrors)

      case None =>
        copy(value = None)
    }
  }

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

  //If same type, retain all settings, if, if not same all constraints will be dropped
  //Transform should start before the verifying
  inline def transform[B](fn: A => B, fn2: B => A): Field[B] =
    Field.of[B](name).copy(form = this.form)


}

/*
 * https://www.playframework.com/documentation/2.8.x/api/scala/play/api/data/Forms$.html
 */
//Numeric
inline def boolean = Field.of[Boolean]
inline def byteNumber = Field.of[Byte]
inline def byteNumber(min: Byte = Byte.MinValue, max: Byte = Byte.MaxValue, strict: Boolean = false) =
  Field.of[Byte].verifying(Constraints.min(min, strict), Constraints.max(max, strict))

inline def shortNumber = Field.of[Short]
inline def shortNumber(min: Short = Short.MinValue, max: Short = Short.MinValue, strict: Boolean = false) =
  Field.of[Short].verifying(Constraints.min[Short](min, strict), Constraints.max[Short](max, strict))
inline def number = Field.of[Int]
inline def number(min:Int, max:Int) = Field.of[Int].verifying(Constraints.min(min), Constraints.max(max))
inline def longNumber = Field.of[Long]
inline def longNumber(min: Long = Long.MinValue, max: Long = Long.MaxValue, strict: Boolean = false) =
  Field.of[Long].verifying(Constraints.min[Long](min, strict), Constraints.max[Long](max, strict))
inline def double = Field.of[Double]
inline def float = Field.of[Float]
inline def bigDecimal = Field.of[BigDecimal]
inline def bigDecimal(precision: Int, scale: Int) =
  Field.of[BigDecimal].verifying(Constraints.precision(precision, scale))

//Text
inline def char = Field.of[Char]

inline def text:Field[String] = Field.of[String]

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

inline def date(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) = Field.of[java.util.Date]
inline def localDate = Field.of[java.time.LocalDate]
inline def localDate(pattern: String) = Field.of[java.time.LocalDate]
inline def localDateTime = Field.of[java.time.LocalDateTime]
inline def localDateTime(pattern: String) = Field.of[java.time.LocalDateTime]
inline def localTime = Field.of[java.time.LocalTime]
inline def localTime(pattern: String) = Field.of[java.time.LocalTime]
inline def sqlDate = Field.of[java.sql.Date]
inline def sqlDate(pattern: String) = Field.of[java.sql.Date]
inline def sqlTimestamp = Field.of[java.sql.Timestamp]
inline def sqlTimestamp(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =  Field.of[java.sql.Timestamp]

//Collection
inline def indexedSeq[A] = Field.of[IndexedSeq[A]]
inline def list[A] = Field.of[List[A]]
inline def list[A](a: Field[A]): Field[List[A]] = ???
inline def seq[A] = Field.of[Seq[A]]
inline def seq[A](a: Field[A]): Field[Seq[A]] = ???
inline def set[A] = Field.of[Set[A]]
inline def vector[A] = Field.of[Vector[A]]
inline def optional[A] = Field.of[Option[A]]
inline def optional[A](a: Field[A]): Field[Option[A]] = ??? // Field.of[Option[A]]

inline def uuid = Field.of[java.util.UUID]
inline def checked(msg: String) = Field.of[Boolean].verifying(msg, _ == true)
inline def default[A](mapping: Field[A], value: A): Field[A] = Field.of[A].fill(value) //FIXME OptionalMapping(mapping).transform(_.getOrElse(value), Some(_))
inline def ignored[A](value: A): Field[A] = ??? // of(ignoredFormat(value))
