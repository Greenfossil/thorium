package com.greenfossil.webserver.data

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

object Form {

  type FormMappings[Xs <: Tuple] <: Tuple = Xs match {
    case EmptyTuple => Xs
    case Field[t] *: ts => t *: FormMappings[ts]
    case (String, Field[t]) *: ts => t *: FormMappings[ts]
  }

  type NameFieldMappings[X <:Tuple] <: Tuple = X match {
    case EmptyTuple => X
    case t *: ts => Field[t] *: NameFieldMappings[ts]
  }

//  def fields[A <: Field[_] *: Tuple](fields: A): Form[FormMappings[A]] =
//    Form[FormMappings[A]](fields)

  def asTuple[A  <: (String, Field[_]) *: Tuple](tuple: A): TupleMapper[FormMappings[A]] =
    val fs = tuple.map[[X] =>> Field[_]]([X] => (x: X) => x match {
      case (name: String, f: Field[_]) => f.copy(name = name)
    })
    TupleMapper[FormMappings[A]](fs)


  import scala.deriving.*

  def asClass[A](using m: Mirror.ProductOf[A])(tuple: Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]) =
    val xs = tuple.map[[X] =>> Field[_]]([X] => (x: X) =>
      x match {
        case (name: String, f: Field[_]) => f.copy(name = name)
      }
    )
    CaseClassMapper[A, Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]](xs.asInstanceOf[Field[_] *: Tuple])

}

case class CaseClassMapper[T, B](mappings: Field[_] *: Tuple = null, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[T] = None) extends FormMapping[CaseClassMapper[T, B], T]{
  override def setMappings(mapping: Field[_] *: Tuple): CaseClassMapper[T, B] = copy(mappings = mapping)

  override def setData(data: Map[String, Any]): CaseClassMapper[T, B] = copy(data = data)

  override def setValue(value: T): CaseClassMapper[T, B] = copy(value = Option(value))

  override def setErrors(errors: Seq[FormError]): CaseClassMapper[T, B] = copy(errors = errors)
}

case class TupleMapper[T <: Tuple](mappings: Field[_] *: Tuple, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[T] = None) extends FormMapping[TupleMapper[T], T] {
  override def setMappings(mapping: Field[_] *: Tuple): TupleMapper[T] = copy(mappings = mapping)

  override def setData(data: Map[String, Any]): TupleMapper[T] = copy(data = data)

  override def setValue(value: T): TupleMapper[T] = copy(value = Option(value))

  override def setErrors(errors: Seq[FormError]): TupleMapper[T] = copy(errors = errors)
}

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

case class Field[A](tpe: String, form: TupleMapper[_] = null, name: String = null, errors: Seq[FormError] = Nil, value: Option[A] = None) {
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


case class FormError(key: String, messages: Seq[String], args: Seq[Any] = Nil)