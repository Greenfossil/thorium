package com.greenfossil.commons.data

import com.greenfossil.commons.data.Formatter.*
import com.greenfossil.commons.json.JsValue
import com.greenfossil.webserver.data.Field.fieldType
import com.greenfossil.webserver.data.Form.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}

import java.time.*
import scala.deriving.Mirror

object Field {

  inline def of[A]: Field[A] =
    fieldOf[A]

  inline def of[A](binder: Formatter[A]) : Field[A] =
    fieldOf[A].binder(binder) //new Field(fieldType[A], binder)

  inline def of[A](name: String): Field[A] =
    fieldOf[A].name(name)  //new Field(fieldType[A], binder= binderOf[A], name = name)

  inline def of[A](name: String, binder: Formatter[A]): Field[A] =
    fieldOf[A].name(name).binder(binder) //new Field(fieldType[A], binder= binder, name = name)

  import scala.compiletime.*

  inline def fieldOf[A]: Field[A] =
    inline erasedValue[A] match {
      case _: String             => ScalarField("String", binder = binderOf[A])
      case _: Int                => ScalarField("Int", binder = binderOf[A])
      case _: Long               => ScalarField("Long", binder = binderOf[A])
      case _: Double             => ScalarField("Double", binder = binderOf[A])
      case _: Float              => ScalarField("Float", binder = binderOf[A])
      case _: Boolean            => ScalarField("Boolean", binder = binderOf[A])
      case _: LocalDateTime      => ScalarField("LocalDateTime", binder = binderOf[A])
      case _: LocalDate          => ScalarField("LocalDate", binder = binderOf[A])
      case _: LocalTime          => ScalarField("LocalTime", binder = binderOf[A])
      case _: YearMonth          => ScalarField("YearMonth", binder = binderOf[A])
      case _: java.sql.Timestamp => ScalarField("SqlTimestamp", binder = binderOf[A])
      case _: java.sql.Date      => ScalarField("SqlDate", binder = binderOf[A])
      case _: java.util.Date     => ScalarField("Date", binder = binderOf[A])
      case _: java.util.UUID     => ScalarField("UUID", binder = binderOf[A])
      case _: Byte               => ScalarField("Byte", binder = binderOf[A])
      case _: Short              => ScalarField("Short", binder = binderOf[A])
      case _: BigDecimal         => ScalarField("BigDecimal", binder = binderOf[A])
      case _: Char               => ScalarField("Char", binder = binderOf[A])
      case _: Option[a]          => OptionField[a]("?", elemField = fieldOf[a]).asInstanceOf[Field[A]] // fieldType[a]
      case _: Seq[a]             => SeqField[a]("[", elemField = fieldOf[a]).asInstanceOf[Field[A]]  //"[" + fieldType[a]
      case _: Tuple              => ProductField("P-") // "P-"
      case _: Product            => ProductField("P+") //"P+" //Product must be tested last
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

trait Field[A] extends ConstraintVerifier[Field, A]{
  val tpe: String
  val name: String
  val form: Form[_]
  val value: Option[A]
  val errors: Seq[FormError]

  def name(name: String): Field[A]
  def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A]
  def binder(binder: Formatter[A]): Field[A]
  def bind(data:Map[String, String]): Field[A]

  protected def getPathName(prefix: String, name: String): String = {
    (prefix, name) match
      case (prefix, null) => prefix
      case ("", _) => name
      case (_, _) => s"$prefix.$name"
  }

  def bindUsingPrefix(prefix: String, data: Map[String, String]): Field[A]

  def bindJsValue(jsValue: JsValue): Field[A] = ???

  def fill(newValue: A):Field[A] = ??? //copy(value = Option(newValue))

  def fill(newValueOpt: Option[?]): Field[A] = ???

  override def toString: String = s"name:$name type:$tpe value:$value"

}

case class ScalarField[A](tpe: String,
                       name: String = null,
                       form: Form[_] = null,
                       value: Option[A] = None,
                       binder: Formatter[A],
                       constraints:Seq[Constraint[A]] = Nil,
                       format: Option[(String, Seq[Any])] = None,
                       errors: Seq[FormError] = Nil) extends Field[A] {

  override def toString: String = s"name:$name type:$tpe binder:${if binder != null then binder.tpe else null} value:$value"

  override def name(name: String): Field[A] = copy(name = name)

  override def binder(binder: Formatter[A]): Field[A] = copy(binder = binder)

  override def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    throw new UnsupportedOperationException("ScalarField does not support mappings")

  override def fill(newValue: A):Field[A] = copy(value = Option(newValue))

  override def fill(newValueOpt: Option[?]): Field[A] = copy(value = newValueOpt.asInstanceOf[Option[A]])

  override def bind(data:Map[String, String]): Field[A] =
    bindUsingPrefix("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, String]): Field[A] = {
    val pathName = getPathName(prefix, name)
    binder.bind(pathName, data) match {
      case Left(errors) => copy(errors = errors)
      case Right(value) =>
        val errors = applyConstraints(value)
        copy(value = Option(value), errors = errors)
    }
  }

  override def bindJsValue(jsValue: JsValue): Field[A] =
  //    val newValueOpt =  Field.toValueOf[A](tpe, jsValue.asOpt[Any])
    ???

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

}


case class ProductField[A](tpe: String,
                           name: String = null,
                           form: Form[_] = null,
                           value: Option[A] = null,
                           constraints:Seq[Constraint[A]] = Nil,
                           format: Option[(String, Seq[Any])] = None,
                           errors: Seq[FormError] = Nil,
                           mappings: Field[_] *: Tuple = null,
                           mirrorOpt: Option[Mirror.ProductOf[A]] = None) extends Field[A] {
  override def name(name: String): Field[A] =
    copy(name = name)

  override def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(mappings = mappings, mirrorOpt = Option(mirror))

  override def binder(binder: Formatter[A]): Field[A] =
    throw new UnsupportedOperationException("Product field does not support setting of binder")

  override def bind(data: Map[String, String]): Field[A] =
    bindToProduct("", data)

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

  override def bindUsingPrefix(prefix: String, data: Map[String, String]): Field[A] =
    throw new UnsupportedOperationException("Product Field does not support this action")

  override def verifying(newConstraints: Constraint[A]*): ProductField[A] =
    copy(constraints = constraints ++ newConstraints)
}

case class OptionField[A](tpe: String,
                          name: String = null,
                          form: Form[_] = null,
                          value: Option[A] = null,
                          errors: Seq[FormError] = Nil,
                          elemField: Field[A]) extends Field[A] {

  override def name(name: String): Field[A] =
    copy(name = name, elemField = elemField.name(name))

  override def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def binder(binder: Formatter[A]): Field[A] =
    copy(elemField= elemField.binder(binder))

  override def bind(data: Map[String, String]): Field[A] =
    val bindedField = elemField.bind(data)
    copy(value = bindedField.value, errors = bindedField.errors, elemField = bindedField)

  override def bindUsingPrefix(prefix: String, data: Map[String, String]): Field[A] =
    elemField.bindUsingPrefix(prefix, data)

  override def verifying(error: String, constraintPredicate: A => Boolean): Field[A] =
    val verifiedField = elemField.verifying(error, constraintPredicate)
    copy(errors = verifiedField.errors, elemField = verifiedField)

  override val constraints: Seq[Constraint[A]] =
    elemField.constraints

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    elemField.verifying(newConstraints*)
}

case class SeqField[A](tpe: String,
                       name: String = null,
                       form: Form[_] = null,
                       value: Option[A] = null,
                       errors: Seq[FormError] = Nil,
                       elemField: Field[A]) extends Field[A] {

  override def name(name: String): Field[A] =
    copy(name = name, elemField = elemField.name(name))

  override def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def binder(binder: Formatter[A]): Field[A] =
    copy(elemField = elemField.binder(binder))

  override def bind(data: Map[String, String]): Field[A] =
    bindToSeq("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, String]): Field[A] =
    elemField.bindUsingPrefix(prefix, data)

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
      //set elemField as Indexed name []
      elemField.name(key).bind(data)
    }

    val values = bindedFields.collect{case f: Field[_] if f.value.isDefined => f.value.get}
    val errors = bindedFields.collect{case f: Field[_] if f.errors.nonEmpty => f.errors}.flatten
    copy(value = Option(values).asInstanceOf[Option[A]], errors = errors)
  }

  override val constraints: Seq[Constraint[A]] =
    elemField.constraints

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    elemField.verifying(newConstraints*)
}