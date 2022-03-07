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

  def fields[A <: Field[_] *: Tuple](fields: A): Form[FormMappings[A]] =
    Form[FormMappings[A]](fields)

  def asTuple[A  <: (String, Field[_]) *: Tuple](tuple: A): Form[FormMappings[A]] =
    val fs = tuple.map[[X] =>> Field[_]]([X] => (x: X) => x match {
      case (name: String, f: Field[_]) => f.copy(name = name)
    })
    Form[FormMappings[A]](fs)

  import scala.compiletime.{summonFrom, error, constValue, erasedValue, summonInline}

  import scala.deriving.*
  def asCaseClass[A](using m: Mirror.ProductOf[A]) =
    CaseClassMapper[A, Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]]()

  inline def toMappingFields(mappings: Tuple): Tuple =
    mappings match {
      case EmptyTuple => EmptyTuple
      case t *: ts =>
        val f = t match {case (name, field) => field }
        println(s"f = ${f}")
        f *: toMappingFields(ts)
    }

  inline private def getLabelNames[A <: Tuple]: Seq[String] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => constValue[t].toString +: getLabelNames[ts]

  inline private def getFieldTypes[A]: Tuple =
    inline erasedValue[A] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts) => toField[t] *: getFieldTypes[ts]

  inline private def toField[A]: Field[_] =
    inline erasedValue[A] match {
      case _: Long => longNumber
      case _: String => text
    }
}

case class CaseClassMapper[A, B](mappings: Field[_] *: Tuple = null, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[A] = None) {
  def fill(values: A):CaseClassMapper[A, B] = ???
  def apply(nameFieldmappings:B): CaseClassMapper[A,B] = ???
}

case class Form[A](mappings: Field[_] *: Tuple, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[A] = None) {

  import scala.deriving.Mirror

  def fill(values: A): Form[A] =
    val filledFields  = values match {
      case _values: Tuple =>
       tupleToData(_values)

      case caseclass: Product =>
        val tuple = Tuple.fromProduct(caseclass)
        tupleToData(tuple)
    }
    copy(mappings = filledFields)

  def bindFromRequest()(using request: com.greenfossil.webserver.Request): Form[A] =
    val querydata: Map[String, Seq[String]] =
      request.method() match {
        case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Map.empty
        case _ => Map.empty //FIXME - request.queryString
      }
    request match {
      case req if req.asFormUrlEncoded.nonEmpty =>
        bind(req.asFormUrlEncoded ++ querydata)

      case req if req.asMultipartFormData.bodyPart.nonEmpty =>
        bind(req.asMultipartFormData.asFormUrlEncoded ++ querydata)

      case req if req.asJson.asOpt.isDefined =>
        bind(req.asJson, querydata)
    }

  def bind(data: Map[String, Seq[String]]): Form[A] = {
    val newMappings = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.copy(value = Field.toValueOf(f.tpe, data.get(f.name).orNull))
    }
    this.copy(mappings = newMappings, data = data)
  }

  def bind(js: JsValue, query: Map[String, Seq[String]]): Form[A] = {
    //WIP
    val newMappings = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.copy(value = Field.toValueOf(f.tpe, (js \ f.name).asOpt[String]))
    }
    this.copy(mappings = newMappings, data = null)
  }

  private def tupleToData(values: Product): Field[_] *: Tuple = {
    val valuesIter = values.productIterator
    val filledFields = mappings.map[[F] =>> Field[_]](
      [F] => (f: F) => f match {
        case f: Field[_] => f.copy(value = valuesIter.nextOption())
      })
    filledFields
  }

  def fold[R](hasErrors: Form[A] => R, success: A => R): R = value match {
    case Some(v) if errors.isEmpty => success(v)
    case _ => hasErrors(this)
  }

  inline def apply[T](key: String): Field[T] =
    mappings
      .productIterator
      .find(_.asInstanceOf[Field[T]].name == key)
      .map(_.asInstanceOf[Field[T]])
      .orNull

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

case class Field[A](tpe: String, form: Form[_] = null , name: String = null, errors: Seq[FormError] = Nil, value: Option[A] = None) {
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