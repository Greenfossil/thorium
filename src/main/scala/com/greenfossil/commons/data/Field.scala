package com.greenfossil.commons.data

import com.greenfossil.commons.data.Formatter.*
import com.greenfossil.commons.json.{JsObject, JsValue}
import com.greenfossil.commons.data.Form.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}

import java.time.*
import scala.deriving.Mirror
import scala.deriving.Mirror.ProductOf

object Field {

  inline def of[A]: Field[A] =
    fieldOf[A]

  inline def of[A](binder: Formatter[A]) : Field[A] =
    fieldOf[A].binder(binder)

  inline def of[A](name: String): Field[A] =
    fieldOf[A].name(name)

  inline def of[A](name: String, binder: Formatter[A]): Field[A] =
    fieldOf[A].name(name).binder(binder)

  import scala.compiletime.*

  inline def fieldOf[A]: Field[A] =
    inline erasedValue[A] match {
      case _: String             => ScalarField("String", binder = stringFormat.asInstanceOf[Formatter[A]])
      case _: Int                => ScalarField("Int", binder = intFormat.asInstanceOf[Formatter[A]])
      case _: Long               => ScalarField("Long", binder = longFormat.asInstanceOf[Formatter[A]])
      case _: Double             => ScalarField("Double", binder = doubleFormat.asInstanceOf[Formatter[A]])
      case _: Float              => ScalarField("Float", binder = floatFormat.asInstanceOf[Formatter[A]])
      case _: Boolean            => ScalarField("Boolean", binder = booleanFormat.asInstanceOf[Formatter[A]])
      case _: LocalDateTime      => ScalarField("LocalDateTime", binder = localDateTimeFormat.asInstanceOf[Formatter[A]])
      case _: LocalDate          => ScalarField("LocalDate", binder = localDateFormat.asInstanceOf[Formatter[A]])
      case _: LocalTime          => ScalarField("LocalTime", binder = localTimeFormat.asInstanceOf[Formatter[A]])
      case _: YearMonth          => ScalarField("YearMonth", binder = yearMonthFormat.asInstanceOf[Formatter[A]])
      case _: java.sql.Timestamp => ScalarField("SqlTimestamp", binder = sqlTimestampFormat.asInstanceOf[Formatter[A]])
      case _: java.sql.Date      => ScalarField("SqlDate", binder = sqlDateFormat.asInstanceOf[Formatter[A]])
      case _: java.util.Date     => ScalarField("Date", binder = dateFormat.asInstanceOf[Formatter[A]])
      case _: java.util.UUID     => ScalarField("UUID", binder = uuidFormat.asInstanceOf[Formatter[A]])
      case _: Byte               => ScalarField("Byte", binder = byteFormat.asInstanceOf[Formatter[A]])
      case _: Short              => ScalarField("Short", binder =shortFormat.asInstanceOf[Formatter[A]])
      case _: BigDecimal         => ScalarField("BigDecimal", binder = bigDecimalFormat.asInstanceOf[Formatter[A]])
      case _: Char               => ScalarField("Char", binder = charFormat.asInstanceOf[Formatter[A]])
      case _: Option[a]          => OptionalField[a]("?", elemField = fieldOf[a]).asInstanceOf[Field[A]]
      case _: Set[a]             => SeqField[a]("[Set", elemField = fieldOf[a]).asInstanceOf[Field[A]]
      case _: IndexedSeq[a]      => SeqField[a]("[IndexSeq", elemField = fieldOf[a]).asInstanceOf[Field[A]]
      case _: Vector[a]          => SeqField[a]("[Vector", elemField = fieldOf[a]).asInstanceOf[Field[A]]
      case _: List[a]            => SeqField[a]("[List", elemField = fieldOf[a]).asInstanceOf[Field[A]]
      case _: Seq[a]             => SeqField[a]("[Seq", elemField = fieldOf[a]).asInstanceOf[Field[A]]
      case _: Tuple              => ProductField("P-") // "P-"
      case _: Product            => ProductField("P+") //"P+" //Product must be tested last
      case _: Any                => ScalarField("Any", binder = null)
    }

}

trait Field[A] extends ConstraintVerifier[Field, A]{
  val tpe: String

  val name: String

  val form: Form[_]

  val value: Option[A]

  val errors: Seq[FormError]

  def name(name: String): Field[A]

  def safeValue: Any =  if value.isDefined then value.get else None

  def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A]

  def binder(binder: Formatter[A]): Field[A]

  def bind(data: (String, String)*): Field[A] =
    bind(data.groupMap(_._1)(_._2))

  def bind(data:Map[String, Seq[String]]): Field[A]

  def fill(newValue: A):Field[A] = fill(Option(newValue))

  def fill(newValueOpt: Option[A]): Field[A]

  protected def getPathName(prefix: String, name: String): String = {
    (prefix, name) match
      case (prefix, null) => prefix
      case ("", _) => name
      case (_, _) => s"$prefix.$name"
  }

  def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A]

  def bindJsValue(jsValue: JsValue): Field[A]

  def bindJsValue(prefix: String, jsValue: JsValue): Field[A]

  def transform[B](mappingFn: A => B): Field[B] =
    MappingField(tpe = "#", delegate = this, delegateMapping = mappingFn)

  def map[B](mappingFn: A => B): Field[B] =
    MappingField(tpe = "#", delegate = this, delegateMapping = mappingFn)

  override def toString: String = s"name:$name type:$tpe value:$value"

}

case class ScalarField[A](tpe: String,
                       name: String = null,
                       form: Form[_] = Form.empty,
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

  override def fill(newValueOpt: Option[A]): Field[A] =
    require(newValueOpt != null, "value cannot be null")
    newValueOpt.fold(this){newValue =>
      copy(value = newValueOpt, errors = applyConstraints(newValue))
    }

  override def bind(data:Map[String, Seq[String]]): Field[A] =
    bindUsingPrefix("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A] = {
    val pathName = getPathName(prefix, name)
    binder.bind(pathName, data) match {
      case Left(errors) => copy(errors = errors)
      case Right(value) =>
        val errors = applyConstraints(value)
        copy(value = Option(value), errors = errors)
    }
  }

  override def bindJsValue(jsValue: JsValue): Field[A] =
    bindJsValue("", jsValue)

  override def bindJsValue(prefix: String, jsValue: JsValue): Field[A] =
    val pathName = getPathName(prefix, name)
    (jsValue \ pathName).asOpt[Any] match
      case Some(value) => bind(Map(pathName -> Seq(value.toString)))
      case None => bind(Map.empty)

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

}

case class ProductField[A](tpe: String,
                           name: String = null,
                           form: Form[_] = Form.empty,
                           value: Option[A] = None,
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

  override def bind(data: Map[String, Seq[String]]): Field[A] =
    bindToProduct("", data)

  private def bindToProduct(prefix: String, data: Map[String, Seq[String]]): Field[A] = {
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

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A] =
    bindToProduct(prefix, data)

  override def fill(newValueOpt: Option[A]): Field[A] =
    newValueOpt.fold(this)(newValue =>
      copy(value = newValueOpt, errors = applyConstraints(newValue))
    )

  override def verifying(newConstraints: Constraint[A]*): ProductField[A] =
    copy(constraints = constraints ++ newConstraints)

  override def bindJsValue(jsValue: JsValue): Field[A] =
    bindJsValue("", jsValue)

  def bindJsValue(prefix: String, jsValue: JsValue): Field[A] =
    val pathName = getPathName(prefix, name)
    (jsValue \ pathName).asOpt[JsObject] match {
      case Some(jsObj) =>
        val newMappings = mappings.map[[X] =>> Field[_]]{[X] => (x: X) =>
          x match {
            case f: Field[t] => f.bindJsValue(prefix, jsObj)
          }
        }
        bindedFieldsToValue(newMappings, mirrorOpt,
          (newData, newMappings, newValue, newErrors) =>
            copy(mappings = newMappings, value = Option(newValue), errors = newErrors))

      case None => bindToProduct("", Map.empty)
    }
}

case class OptionalField[A](tpe: String,
                            name: String = null,
                            form: Form[_] = Form.empty,
                            value: Option[A] = None,
                            constraints:Seq[Constraint[A]] = Nil,
                            errors: Seq[FormError] = Nil,
                            elemField: Field[A]) extends Field[A] {

  override def name(name: String): OptionalField[A] =
    copy(name = name, elemField = elemField.name(name))

  override def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def binder(binder: Formatter[A]): Field[A] =
    copy(elemField= elemField.binder(binder))

  override def bind(data: Map[String, Seq[String]]): Field[A] =
    bindUsingPrefix("", data)

  override def safeValue: Any =  value 

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A] =
    val bindedField = elemField.bind(data)
    val bindedValue = bindedField.value

    //ignore required field as this field is optional
    val bindedFieldErrors = bindedField.errors.filterNot(_.is(name, "error.required"))
    val errors = applyConstraints(bindedValue.asInstanceOf[A])
    copy(value = bindedValue, errors = bindedFieldErrors ++ errors, elemField = bindedField)

  override def fill(newValueOpt: Option[A]): Field[A] =
    val filledField = elemField.fill(newValueOpt)
    copy(value = elemField.value , elemField = filledField, errors = filledField.errors)

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

  override def bindJsValue(jsValue: JsValue): Field[A] =
    (jsValue \ name).asOpt[Any] match
      case Some(value) => bind(Map(name -> Seq(value.toString)))
      case None => bind(Map.empty)

  override def bindJsValue(prefix: String, jsValue: JsValue): Field[A] = ???
}

case class SeqField[A](tpe: String,
                       name: String = null,
                       form: Form[_] = Form.empty,
                       value: Option[A] = None,
                       constraints:Seq[Constraint[A]] = Nil,
                       errors: Seq[FormError] = Nil,
                       elemField: Field[A]) extends Field[A] {

  override def name(name: String): Field[A] =
    copy(name = name, elemField = elemField.name(name))
    
  def indexes: Seq[Int] = ???

  override def mappings(mappings: Field[_] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def binder(binder: Formatter[A]): Field[A] =
    copy(elemField = elemField.binder(binder))

  override def bind(data: Map[String, Seq[String]]): Field[A] =
    bindToSeq("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A] =
    bindToSeq(prefix, data)

  private def bindToSeq(prefix: String, dataMap: Map[String, Seq[String]]): Field[A] = {

    val pathName = getPathName(prefix, name)
    val keyMatchRegex = s"$pathName(\\[\\d*])?(\\..*)?"
    val keyReplaceRegex = s"($pathName(\\[(\\d*)])?)(\\.(.*))?"  // keyName is group 1, index is group 3

    /**
     * sorted according to their dataMap index if available.
     * if index is not available it will place in front
     */
    val sortedFieldNames =
      dataMap.toList.collect { case (key, _) if key.matches(keyMatchRegex) =>
        val indexOpt: Option[Int] = Option(key.replaceFirst(keyReplaceRegex, "$3")).filter(_.nonEmpty).flatMap(_.toIntOption)
        val name = key.replaceFirst(keyReplaceRegex, "$1") // drop the inner field names
        (indexOpt, name)
      }
        .sortBy(_._1)
        .map(_._2).distinct

    /**
     * if name is not the same key in dataMap, bind using the entire dataMap (applicable for repeatedTuples)
     * if name exists in dataMap, bind each value separately - handles multiple value to one key
     */
    val bindedFields: Seq[Field[_]] = sortedFieldNames.flatMap{name =>
      dataMap.getOrElse(name, Nil) match {
        case Nil => Seq(elemField.name(name).bind(dataMap)) 
        case values => values.map(value => elemField.name(name).bind(Map(name -> Seq(value))))
      }
    }

    val values = bindedFields.collect{case f: Field[_] if f.value.isDefined => f.value.get}
    val errors = bindedFields.collect{case f: Field[_] if f.errors.nonEmpty => f.errors}.flatten ++
      applyConstraints(values.asInstanceOf[A])
    
    copy(value = Option(values).asInstanceOf[Option[A]], errors = errors)
  }

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

  override def fill(newValueOpt: Option[A]): Field[A] =
    val filledField = elemField.fill(newValueOpt)
    copy(value = filledField.value, errors = filledField.errors)

  override def bindJsValue(jsValue: JsValue): Field[A] =
    bindJsValue("", jsValue)

  override def bindJsValue(prefix: String, jsValue: JsValue): Field[A] =
    val pathName = getPathName(prefix, name)
    val data = (jsValue \ pathName).asOpt[Seq[Any]] match {
      case Some(xs) =>
        xs.zipWithIndex.map(x => s"$pathName[${x._2}]" -> Seq(x._1.toString)).toMap
      case None =>
        Map.empty
    }
    bindToSeq(prefix, data)
}

case class MappingField[A, B](tpe: String,
                              name: String = null,
                              form: Form[_] = Form.empty,
                              value: Option[B] = None,
                              errors: Seq[FormError] = Nil,
                              constraints: Seq[Constraint[B]] = Nil,
                              delegate: Field[A],
                              delegateMapping: A => B) extends Field[B] {

  override def name(name: String): Field[B] =
    copy(name = name, delegate = delegate.name(name))

  override def mappings(mappings: Field[_] *: Tuple, mirror: ProductOf[B]): Field[B] =
    throw new IllegalArgumentException("MappingField#mappings is not supported for MappingField")

  override def binder(binder: Formatter[B]): Field[B] =
    throw new IllegalArgumentException("MappingField#binder is not supported for MappingField")

  override def fill(newValueOpt: Option[B]): Field[B] =
    val filledDelegate = newValueOpt.map(v => delegate.fill(v.asInstanceOf[A]))
    val _value = filledDelegate.flatMap(f => f.value.map(delegateMapping)).orElse(value)
    val _errors = _value.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _value , errors =_errors)

  override def bind(data: Map[String, Seq[String]]): Field[B] =
    val bindedDelegate = delegate.bind(data)
    val _value = bindedDelegate.value.map(v => delegateMapping(v)).orElse(value)
    val _errors = _value.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _value , errors =_errors)

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[B] =
    throw new IllegalArgumentException("MappingField#bindUsingPrefix is not supported for MappingField")

  override def bindJsValue(jsValue: JsValue): Field[B] =
    val bindedDelegate = delegate.bindJsValue(jsValue)
    val _value = bindedDelegate.value.map(v => delegateMapping(v)).orElse(value)
    val _errors = _value.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _value , errors =_errors)

  override def bindJsValue(prefix: String, jsValue: JsValue): Field[B] =
    throw new IllegalArgumentException("MappingField#bindUsingPrefix is not supported for MappingField")

  override def verifying(newConstraints: Constraint[B]*): Field[B] =
    copy(constraints = newConstraints)
}