package com.greenfossil.commons.data

import com.greenfossil.commons.data.Formatter.*
import com.greenfossil.commons.json.JsValue
import com.greenfossil.webserver.data.Field.fieldType
import com.greenfossil.webserver.data.Form.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}

import java.time.*
import scala.deriving.Mirror

object Field {

  inline def of[A]: Field[A] = new Field(fieldType[A], binder = binderOf[A])

  inline def of[A](binder: Formatter[A]) : Field[A] = new Field(fieldType[A], binder)

  inline def of[A](name: String): Field[A] = new Field(fieldType[A], binder= binderOf[A], name = name)

  inline def of[A](name: String, binder: Formatter[A]): Field[A] = new Field(fieldType[A], binder= binder, name = name)

  import scala.compiletime.*

  inline def fieldType[A]: String =
    inline erasedValue[A] match {
      case _: String             => "String"
      case _: Int                => "Int"
      case _: Long               => "Long"
      case _: Double             => "Double"
      case _: Float              => "Float"
      case _: Boolean            => "Boolean"
      case _: LocalDateTime      => "LocalDateTime"
      case _: LocalDate          => "LocalDate"
      case _: LocalTime          => "LocalTime"
      case _: YearMonth          => "YearMonth"
      case _: java.sql.Timestamp => "SqlTimestamp"
      case _: java.sql.Date      => "SqlDate"
      case _: java.util.Date     => "Date"
      case _: java.util.UUID     => "UUID"
      case _: Byte               => "Byte"
      case _: Short              => "Short"
      case _: BigDecimal         => "BigDecimal"
      case _: Char               => "Char"
      case _: Option[a]          => "?" + fieldType[a]
      case _: Seq[a]             => "[" + fieldType[a]
      case _: Tuple              => "P-"
      case _: Product            => "P+" //Product must be tested last
    }

  inline def binderOf[A]: Formatter[A] =
    inline erasedValue[A] match
      case _: String             => stringFormat.asInstanceOf[Formatter[A]]
      case _: Int                => intFormat.asInstanceOf[Formatter[A]]
      case _: Long               => longFormat.asInstanceOf[Formatter[A]]
      case _: Double             => doubleFormat.asInstanceOf[Formatter[A]]
      case _: Float              => floatFormat.asInstanceOf[Formatter[A]]
      case _: Boolean            => booleanFormat.asInstanceOf[Formatter[A]]
      case _: LocalDateTime      => localDateTimeFormat.asInstanceOf[Formatter[A]]
      case _: LocalDate          => localDateFormat.asInstanceOf[Formatter[A]]
      case _: LocalTime          => localTimeFormat.asInstanceOf[Formatter[A]]
      case _: YearMonth          => yearMonthFormat.asInstanceOf[Formatter[A]]
      case _: java.sql.Timestamp => sqlTimestampFormat.asInstanceOf[Formatter[A]]
      case _: java.sql.Date      => sqlDateFormat.asInstanceOf[Formatter[A]]
      case _: java.util.Date     => dateFormat.asInstanceOf[Formatter[A]]
      case _: java.util.UUID     => uuidFormat.asInstanceOf[Formatter[A]]
      case _: Byte               => byteFormat.asInstanceOf[Formatter[A]]
      case _: Short              => shortFormat.asInstanceOf[Formatter[A]]
      case _: BigDecimal         => bigDecimalFormat.asInstanceOf[Formatter[A]]
      case _: Char               => charFormat.asInstanceOf[Formatter[A]]
      case _                     => null

}

case class Field[A](tpe: String,
                    binder: Formatter[A],
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


  override def toString: String = s"name:$name type:$tpe binder:${if binder != null then binder.tpe else null} value:$value"

  def isOptional: Boolean = tpe.startsWith("?")
  def isSeq: Boolean = tpe.startsWith("[")
  def isProduct: Boolean = tpe.startsWith("P")

  def name(name: String): Field[A] = copy(name = name)

  def rawValue: Any = if isOptional then value else value.orNull

  def fill(newValue: A):Field[A] = copy(value = Option(newValue))

  def fill(newValueOpt: Option[?]): Field[A] = copy(value = newValueOpt.asInstanceOf[Option[A]])

  def bind(data:Map[String, String]): Field[A] = {
    if isProduct then bindToProduct("", data)
    else if isOptional then bindToOptional("", data)
    else if isSeq then bindToSeq("", data)
    else bindUsingPrefix("", data)
  }

  private def getPathName(prefix: String, name: String): String = {
    (prefix, name) match
      case (prefix, null) => prefix
      case ("", _) => name
      case (_, _) => s"$prefix.$name"
  }


  def bindUsingPrefix(prefix: String, data: Map[String, String]): Field[A] = {
    if binder != null
    then
      val pathName = getPathName(prefix, name)
      binder.bind(pathName, data) match {
        case Left(errors) => copy(errors = errors)
        case Right(value) =>
          val errors = applyConstraints(value)
          copy(value = Option(value), errors = errors)
      }
    else
      bindToProduct(prefix, data)
  }

  private def bindToProduct(prefix: String, data: Map[String, String]): Field[A] = {
    val pathName = getPathName(prefix, name)
    val newMappings =
      mappings.map[[A] =>> Field[_]]{
        [X] => (x: X) => x match
          case f: Field[t] => f.bindUsingPrefix(pathName, data)
      }

    bindedFieldsToValue(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))
  }

  private def bindToIndexedProduct(pathName: String, data: Map[String, String]): Field[A] = {
    val newMappings =
      mappings.map[[A] =>> Field[_]]{
        [X] => (x: X) => x match
          case f: Field[t] => f.bindUsingPrefix(pathName, data)
      }

    bindedFieldsToValue(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))
  }

  private def bindToOptional(prefix: String, data: Map[String, String]): Field[A] = {
    val boundField = bindToProduct(prefix, data)
    copy(value = boundField.value.map(Option(_)).asInstanceOf[Option[A]], errors = boundField.errors)
  }

  private def bindToSeq(prefix: String, data: Map[String, String]): Field[A] = {
    /*
     * Filter all name-value list that matches 'field.name' + '.'
     */
    val pathName = getPathName(prefix, name)
    val keyMatchRegex = s"$pathName\\[\\d+].*"
    val keyReplaceRegex = s"$pathName\\[(\\d+)]"

    //Group name-value pairs by index
    val sortedIndexKeyTupList: Seq[(Int, String)] =
      data.toList.collect { case (key, x) if key.matches(keyMatchRegex) =>
        key.replaceAll(keyReplaceRegex, "$1").split("\\.", 2) match {
          case Array(index, fieldKey) =>
            index.toInt -> key.take( key.lastIndexOf("."+fieldKey)) //drop the "dot"fieldKey part from the 'key'
          case Array(index) =>
            index.toInt -> key
        }
      }.sortBy(_._1).distinct

    val bindedFields: Seq[Field[_]] = sortedIndexKeyTupList.map { (index, key) =>
      if binder != null
      then bindUsingPrefix(key, data)
      else bindToIndexedProduct(key, data)
    }

    val values = bindedFields.collect{case f: Field[_] if f.value.isDefined => f.value.get}
    val errors = bindedFields.collect{case f: Field[_] if f.errors.nonEmpty => f.errors}.flatten
    copy(value = Option(values).asInstanceOf[Option[A]], errors = errors)
  }

  def bindJsValue(jsValue: JsValue): Field[A] =
//    val newValueOpt =  Field.toValueOf[A](tpe, jsValue.asOpt[Any])
    ???
  

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

  //If same type, retain all settings, if, if not same all constraints will be dropped
  //Transform should start before the verifying
  inline def transform[B](fn: A => B, fn2: B => A): Field[B] =
    Field.of[B](name).copy(form = this.form)

}
