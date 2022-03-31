package com.greenfossil.webserver.data

import com.greenfossil.commons.json.JsValue
import com.greenfossil.webserver.data.Field.fieldType
import com.greenfossil.webserver.data.Form.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}

import java.time.*
import scala.deriving.Mirror

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
      case _: YearMonth          => "YearMonth"
      case _: java.sql.Date      => "SqlDate"
      case _: java.util.Date     => "Date"
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
    if value == null || value == Some(null) || value == "" then None
    else {
      val optValue = tpe match {

        case "String" =>
          value match {
            case s: String => Option(s)
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Int" =>
          value match {
            case x: Int => Option(x)
            case x: Long => Option(x.toInt)
            case x: BigDecimal => Option(x.toInt)
            case s: String => s.toIntOption
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Long" =>
          value match {
            case x: Int => Option(x.toLong)
            case x: Long  => Option(x)
            case s: BigDecimal /* This is to handle JsNumber type */ => Option(s.toLong)
            case s: String => s.toLongOption
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Double" =>
          value match {
            case x: Double => Option(x)
            case x: Float => Option(x.toDouble)
            case x: BigDecimal => Option(x.toDouble)
            case s: String => s.toDoubleOption
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Float" =>
          value match {
            case x: Float => Option(x)
            case x: Double => Option(x.toFloat)
            case x: BigDecimal => Option(x.toFloat)
            case s: String => s.toFloatOption
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Boolean" =>
          value match {
            case x: Boolean => Option(x)
            case s: String => s.toBooleanOption
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "LocalDate" =>
          value match {
            case x: LocalDate => Option(x)
            case s: String => Option(LocalDate.parse(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "LocalTime" =>
          value match {
            case x: LocalTime => Option(x)
            case s: String => Option(LocalTime.parse(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "YearMonth" =>
          value match {
            case x: YearMonth => Option(x)
            case s: String => Option(YearMonth.parse(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "SqlDate" =>
          value match {
            case x: java.sql.Date => Option(x)
            case s: String => Option(java.sql.Date.valueOf(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Date" =>
          value match {
            case x: java.util.Date => Option(x)
            case s: String => Option(java.util.Date.parse(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "SqlTimestamp" =>
          value match {
            case x: java.sql.Timestamp => Option(x)
            case s: String => Option(java.sql.Timestamp.valueOf(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "LocalDateTime" =>
          value match {
            case x: LocalDateTime => Option(x)
            case s: String => Option(LocalDateTime.parse(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "UUID" =>
          value match {
            case x: java.util.UUID => Option(x)
            case s: String => Option(java.util.UUID.fromString(s))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Byte" =>
          value match {
            case x: Byte => Option(x)
            case s: String => Option(s.toByte)
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Short" =>
          value match {
            case x: Short => Option(x)
            case s: String => Option(s.toShort)
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "BigDecimal" =>
          value match {
            case x: BigDecimal => Option(x)
            case s: String => Option(s).filter(_.nonEmpty).map(x => BigDecimal(x))
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case "Char" =>
          value match {
            case x: Char => Option(x)
            case s: String => s.headOption
            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
            case _ => None
          }

        case seqType if tpe.startsWith("[") =>
          value match {
            case opt: Option[_] =>
              //Seq is wrapped in Opt, unwrap it first and bind as Seq
              opt.flatMap(toValueOf(tpe, _))
            case xs: Seq[_] =>
              Option(xs.flatMap(toValueOf(tpe.tail,_)))
            case s: String =>
              Option(Seq(toValueOf(tpe.tail, s)))
          }

        case optType if tpe.startsWith("?") =>
          value match {
            case opt: Option[_] => opt.map{toValueOf(tpe.tail, _)}
            case xs: Seq[_] => xs.headOption.map{toValueOf(tpe.tail, _)}
            case s: String => toValueOf(tpe.tail, s)
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
                    value: Option[A] = None,

                    /*
                     * these params are meant for use in embedded class use
                     */
                    mappings: Field[_] *: Tuple = null,
                    mirrorOpt: Option[scala.deriving.Mirror.ProductOf[A]] = None) extends ConstraintVerifier[Field, A](name, constraints) {

  def isOptional: Boolean = tpe.startsWith("?")
  def isSeq: Boolean = tpe.startsWith("[") && !isSeqProduct
  def isProduct: Boolean = tpe.startsWith("C-")
  def isSeqProduct: Boolean = tpe.startsWith("[C-")

  def rawValue: Any = if isOptional then value else value.orNull

  def fill(newValue: A):Field[A] = copy(value = Option(newValue))

  def fill(newValueOpt: Option[?]): Field[A] = copy(value = newValueOpt.asInstanceOf[Option[A]])

  def bind(any: Any): Field[A] =
   any match {
      case data: Seq[(String, Any)] =>
        //Seq - name-value pair where name can have duplicates
        bindDataMapObj(data.groupMap(_._1)(_._2))
      case data: Map[String, Any] =>
        bindDataMapObj(data)
      case value: Any =>
        bindValueToField(Field.toValueOf[A](tpe, value))
    }

  def bindDataMapObj(data:Map[String, Any]): Field[A] = {
    if isSeqProduct then
      bindSeqClass(data)
    else if isProduct then
      bindClass(name, data)
    else
      bindDataMapValue(data)
  }

  private def bindDataMapValue(data: Map[String, Any]): Field[A] =
    val value =  if tpe.startsWith("[")
    then
      /*
       * Attempt to get keys that matches f.name + '['
       * if fail then use f.name as key to retrieve value from data
       */
      data.toList.filter((key: String, value: Any) => key.startsWith(s"${name}[")).map(_._2) match {
        case Nil =>
          data.getOrElse(name, None)
        case values =>
          //flatten the values
          values.foldLeft(Seq.empty){(res, v) =>
            v match {
              case xs: Seq[_] => res ++ xs
              case x => res :+ x
            }
          }
      }
    else data.getOrElse(name, None)

    val newValueOpt =  Field.toValueOf[A](tpe, value)
    bindValueToField(newValueOpt)

  private def bindSeqClass(data: Map[String, Any]): Field[A] =
    /*
      * Filter all name-value list that matches 'field.name' + '.'
      */
    val keyMatchRegex = s"$name\\[\\d+]\\..+"
    val keyReplaceRegex = s"$name\\[(\\d+)]"
    //Group name-value pairs by index
    val valueList: Seq[(Int, (String, Any))] = data.toList.collect{ case (key, x) if key.matches(keyMatchRegex) =>
      key.replaceAll(keyReplaceRegex, "$1").split("\\.",2) match {
        case Array(index, fieldKey) =>
          index.toInt -> (fieldKey, x)
      }
    }
    val nvPairsByIndex = valueList.groupMap(_._1)(_._2)
    val sortedIndices = nvPairsByIndex.keys.toList.sorted
    val value = sortedIndices.flatMap{index =>
      val map = nvPairsByIndex(index).map((n, v) => name+"." + n -> v).toMap
      bindClass(name, map).value
    }
    copy(value = Some(value.asInstanceOf[A]))

  private def bindClass(classname: String, data: Map[String, Any]): Field[A] = {
    /*
     * Filter all name-value list that matches 'field.name' + '.'
     */
    val xs: Map[String, Any] = data.collect { case (key, value) if key.startsWith(classname + ".") =>
      key.replace(s"${classname}.", "") -> value
    }
    println(s"xs = ${xs}")
    val newMappings = bindDataToMappings(mappings, xs)
    bindedFieldsToValue(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))
  }

  def bindJsValue(jsValue: JsValue): Field[A] =
    val newValueOpt =  Field.toValueOf[A](tpe, jsValue.asOpt[Any])
    bindValueToField(newValueOpt)

  def bindValueToField(newValueOpt: Option[A]) : Field[A] =
    newValueOpt match {
      case Some(value: A) =>
        val formErrors = applyConstraints(value)
        copy(value = newValueOpt, errors = formErrors)

      case None =>
        copy(value = None)
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
inline def yearMonth = Field.of[java.time.YearMonth]
inline def yearMonth(pattern: String) = Field.of[java.time.YearMonth]
inline def sqlDate = Field.of[java.sql.Date]
inline def sqlDate(pattern: String) = Field.of[java.sql.Date]
inline def sqlTimestamp = Field.of[java.sql.Timestamp]
inline def sqlTimestamp(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =  Field.of[java.sql.Timestamp]

//Collection
inline def indexedSeq[A] = Field.of[IndexedSeq[A]]
inline def list[A] = Field.of[List[A]]
@deprecated("Use list[A]", "")
inline def list[A](a: Field[A]): Field[List[A]] = Field.of[List[A]]
inline def seq[A]: Field[Seq[A]] = Field.of[Seq[A]]

inline def mapping[A](using m: Mirror.ProductOf[A])(
  nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]
): Field[A] =
  new Field(tpe = s"C-${m}", mappings = Form.toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))

inline def mappingRepeat[A](using m: Mirror.ProductOf[A])(
  nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]
): Field[Seq[A]] =
  val aField =  new Field[A](tpe = s"C-$m", mappings = Form.toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))
  new Field[Seq[A]](tpe = s"[C-$m", mappings = aField *: EmptyTuple)

@deprecated("Use seq[A]", "")
inline def seq[A](a: Field[A]): Field[Seq[A]] = Field.of[Seq[A]]
inline def set[A] = Field.of[Set[A]]
//inline def vector[A] = Field.of[Vector[A]]
inline def optional[A] = Field.of[Option[A]]
@deprecated("Use optional[A]", "")
inline def optional[A](a: Field[A]): Field[Option[A]] = Field.of[Option[A]]

inline def uuid = Field.of[java.util.UUID]
inline def checked(msg: String) = Field.of[Boolean].verifying(msg, _ == true)
inline def default[A](mapping: Field[A], value: A): Field[A] = Field.of[A].fill(value) //FIXME OptionalMapping(mapping).transform(_.getOrElse(value), Some(_))
inline def ignored[A](value: A): Field[A] = ??? // of(ignoredFormat(value))
