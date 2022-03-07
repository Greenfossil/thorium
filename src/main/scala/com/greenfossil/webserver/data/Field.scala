package com.greenfossil.webserver.data

import java.time.LocalDate

object Field {
  import scala.compiletime.*

  inline def of[A]: Field[A] = Field(fieldType[A])

  inline def of[A](name: String): Field[A] = Field(fieldType[A], name = name)

  inline def fieldType[A]: String =
    inline erasedValue[A] match {
      case _: Int       => "Int"
      case _: String    => "String"
      case _: Long      => "Long"
      case _: Double    => "Double"
      case _: Float     => "Float"
      case _: Boolean   => "Boolean"
      case _: LocalDate => "LocalDate"
      case _: Seq[a]    => "[" + fieldType[a]
      case _: Option[a] => "?" + fieldType[a]
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
            case _ => None
          }

        case "Long" =>
          value match {
            case x: Long => Option(x)
            case s: String => s.toLongOption
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toLongOption)
            case _ => None
          }

        case "String" =>
          value match {
            case s: String => Option(s)
            case xs: Seq[_] => xs.headOption.map(_.toString)
            case _ => None
          }

        case "Double" =>
          value match {
            case x: Double => Option(x)
            case s: String => s.toDoubleOption
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toDoubleOption)
            case _ => None
          }

        case "Float" =>
          value match {
            case x: Float => Option(x)
            case s: String => s.toFloatOption
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toFloatOption)
            case _ => None
          }

        case "Boolean" =>
          value match {
            case x: Boolean => Option(x)
            case s: String => s.toBooleanOption
            case xs: Seq[_] => xs.headOption.flatMap(_.toString.toBooleanOption)
            case _ => None
          }

        case "LocalDate" =>
          value match {
            case x: LocalDate => Option(x)
            case s: String => LocalDate.parse(s)
            case xs: Seq[_] => xs.headOption.flatMap(x => Option(LocalDate.parse(x.toString)))
            case _ => None
          }

        case seq if seq.startsWith("[") =>
          value match {
            case xs: Seq[_] =>
              Option(xs.flatMap(x => toValueOf(seq.tail,x)))
            case s: String =>
              Option(Seq(toValueOf(seq.tail, s)))
          }
        case opt if opt.startsWith("?") =>
          value match {
            case xs: Seq[_] => Option(xs.flatMap(x => toValueOf(opt.tail,x)))
            case s: String => Option(Seq(toValueOf(opt.tail, s)))
          }
      }
      optValue.asInstanceOf[Option[A]]
    }
  }

}

case class Field[A](tpe: String, form: Form[_] = null, name: String = null, errors: Seq[FormError] = Nil, value: Option[A] = None) {
  def setValue(a: A): Field[A] = copy(value = Option(a))
}


inline def char = Field.of[Char]
inline def short = Field.of[Short]
inline def number = Field.of[Int]
inline def longNumber = Field.of[Long]
inline def byteNumber = Field.of[Byte]
inline def text = Field.of[String]
inline def nonEmptytext = Field.of[String]
inline def email = Field.of[String]
inline def double = Field.of[Double]
inline def float = Field.of[Float]
inline def bigDecimal = Field.of[BigDecimal]
inline def bigDecimal(precision: Int, scale: Int) = Field.of[BigDecimal]
inline def boolean = Field.of[Boolean]
inline def date = Field.of[java.util.Date]
inline def date(patter: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) = Field.of[java.util.Date]
inline def localDate = Field.of[java.time.LocalDate]
inline def localDate(pattern: String) = Field.of[java.time.LocalDate]
inline def localDateTime = Field.of[java.time.LocalDateTime]
inline def localDateTime(pattern: String) = Field.of[java.time.LocalDateTime]
inline def localTime = Field.of[java.time.LocalTime]
inline def localTime(pattern: String) = Field.of[java.time.LocalTime]
inline def sqlDate = Field.of[java.sql.Date]
inline def sqlTimestamp = Field.of[java.sql.Timestamp]
inline def sqlTimestamp(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =  Field.of[java.sql.Timestamp]
inline def list[A] = Field.of[List[A]]
inline def seq[A] = Field.of[Seq[A]]
inline def set[A] = Field.of[Set[A]]
inline def vector[A] = Field.of[Vector[A]]
inline def optional[A] = Field.of[Option[A]]
inline def checked(msg: String): Boolean = ???
inline def default[A](mapping: Field[A], value: A): Field[A] = ???
inline def ignored[A](value: A): Field[A] = ???
