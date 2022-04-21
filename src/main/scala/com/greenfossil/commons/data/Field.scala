package com.greenfossil.commons.data

import com.greenfossil.commons.data.Formatter.*
import com.greenfossil.commons.json.{JsArray, JsObject, JsValue}

import java.time.*
import scala.deriving.Mirror
import scala.deriving.Mirror.ProductOf

object Field {

  /*
 * Extracts Type  't' from Field[t]
 */
  type FieldTypeExtractor[Xs <: Tuple] <: Tuple = Xs match {
    case EmptyTuple => Xs
    case Field[t] *: ts => t *: FieldTypeExtractor[ts]
    case (String, Field[t]) *: ts => t *: FieldTypeExtractor[ts]
  }

  /*
   * Constructs Field[t] from a given type 't'
   */
  type FieldConstructor[X <:Tuple] <: Tuple = X match {
    case EmptyTuple => X
    case t *: ts => Field[t] *: FieldConstructor[ts]
  }

  def toNamedFieldTuple(tuple: Tuple): Field[?] *: Tuple =
    tuple.map[[X] =>> Field[?]]([X] => (x: X) =>
      x match
        case (name: String, f: Field[?]) => f.name(name)
    ).asInstanceOf[Field[?] *: Tuple]

  inline def of[A]: Field[A] =
    fieldOf[A]

  inline def of[A](inline binder: Formatter[A]) : Field[A] =
    fieldOf[A].binder(binder)

  inline def of[A](name: String): Field[A] =
    fieldOf[A].name(name)

  inline def of[A](name: String, binder: Formatter[A]): Field[A] =
    fieldOf[A].name(name).binder(binder)

  import scala.compiletime.*

  inline def fieldOf[A]: Field[A] =
    inline erasedValue[A] match {
      case _: String             => ScalarField("String", binder = checkRequiredFormat.asInstanceOf[Formatter[A]])
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

  val value: Option[A]

  val errors: Seq[FormError]

  def name(name: String): Field[A]

  def safeValue: Any =  if value.isDefined then value.get else None

  def mappings(mappings: Field[?] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A]

  def format: Option[(String, Seq[Any])]

  def binder(binder: Formatter[A]): Field[A]

  def bind(data: (String, String)*): Field[A] =
    bind(data.groupMap(_._1)(_._2))

  def bind(data:Map[String, Seq[String]]): Field[A]

  def fill(newValue: A):Field[A]

  protected def getPathName(prefix: String, name: String): String = {
    (prefix, name) match
      case (prefix, null) => prefix
      case ("", _) => name
      case (_, _) => s"$prefix.$name"
  }

  def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A]

  def bind(jsValue: JsValue): Field[A]

  def bind(prefix: String, jsValue: JsValue): Field[A]

  def transform[B](mappingFn: A => B): Field[B] =
    MappingField(tpe = "#", delegate = this, delegateMapping = mappingFn)

  def map[B](mappingFn: A => B): Field[B] =
    MappingField(tpe = "#", delegate = this, delegateMapping = mappingFn)

  override def toString: String = s"name:$name type:$tpe value:$value"

  /*
   * Form APIs
   */
  def field[A](key: String): Field[A]

  def fold[R](hasErrors: Field[A] => R, success: A => R): R = value match {
    case Some(v) if errors.isEmpty => success(v)
    case _ => hasErrors(this)
  }

  /**
   * Returns `true` if there is an error related to this form.
   */
  def hasErrors: Boolean = errors.nonEmpty

  /**
   * Retrieve the first error for this key.
   *
   * @param key field name.
   */
  def error(key: String): Option[FormError] = errors.find(_.key == key)

  /**
   * Retrieve all errors for this key.
   *
   * @param key field name.
   */
  def errors(key: String): Seq[FormError] = errors.filter(_.key == key)

  /**
   * Retrieves the first global error, if it exists, i.e. an error without any key.
   *
   * @return an error
   */
  @deprecated("Should use globalErrors", "")
  def globalError: Option[FormError] = globalErrors.headOption

  /**
   * Retrieves all global errors, i.e. errors without a key.
   *
   * @return all global errors
   */
  def globalErrors: Seq[FormError] =
    errors.filter(_.key.isEmpty)

  /**
   * Adds an error to this form
   * @param error FormError
   */
  def withError(error: FormError): Field[A]

  def discardingErrors: Field[A]

  /**
   * Adds an error to this form
   * @param key Error key
   * @param message Error message
   * @param args Error message arguments
   */
  def withError(key: String, message: String, args: Any*): Field[A] =
    withError(FormError(key, message, args))

  /**
   * Adds a global error to this form
   * @param message Error message
   * @param args Error message arguments
   */
  def withGlobalError(message: String, args: String*): Field[A] = withError("", message, args*)

}

case class ScalarField[A](tpe: String,
                       name: String = null,
                       value: Option[A] = None,
                       binder: Formatter[A],
                       constraints:Seq[Constraint[A]] = Nil,
                       format: Option[(String, Seq[Any])] = None,
                       errors: Seq[FormError] = Nil) extends Field[A] {

  override def toString: String = s"name:$name type:$tpe binder:${if binder != null then binder.tpe else null} value:$value"

  override def name(name: String): Field[A] = copy(name = name)

  override def binder(binder: Formatter[A]): Field[A] = copy(binder = binder)

  override def mappings(mappings: Field[?] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    throw new UnsupportedOperationException("ScalarField does not support mappings")

  override def fill(newValue: A): Field[A] =
    copy(value = Some(newValue), errors = applyConstraints(newValue))

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

  override def bind(jsValue: JsValue): Field[A] =
    bind("", jsValue)

  override def bind(prefix: String, jsValue: JsValue): Field[A] =
    val pathName = getPathName(prefix, name)
    (jsValue \ pathName).asOpt[Any] match
      case Some(value) => bind(Map(pathName -> Seq(value.toString)))
      case None => bind(Map.empty)

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

  override def field[A](key: String): Field[A] = this.asInstanceOf[Field[A]]

  /**
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: FormError): Field[A] =
   this.copy(errors = this.errors :+ error)

  override def discardingErrors: Field[A] =
   this.copy(errors = Nil)

}

case class ProductField[A](tpe: String,
                           name: String = null,
                           value: Option[A] = None,
                           constraints:Seq[Constraint[A]] = Nil,
                           format: Option[(String, Seq[Any])] = None,
                           errors: Seq[FormError] = Nil,
                           mappings: Field[?] *: Tuple = null,
                           mirrorOpt: Option[Mirror.ProductOf[A]] = None) extends Field[A] {

  override def name(name: String): Field[A] =
    copy(name = name)

  override def mappings(mappings: Field[?] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(mappings = mappings, mirrorOpt = Option(mirror))

  def mapping(name: String): Option[Field[?]] =
    mappings.toList.asInstanceOf[List[Field[?]]].find(_.name == name)

  override def binder(binder: Formatter[A]): Field[A] =
    throw new UnsupportedOperationException("Product field does not support setting of binder")

  override def bind(data: Map[String, Seq[String]]): Field[A] =
    bindToProduct("", data)

  private def bindToProduct(prefix: String, data: Map[String, Seq[String]]): Field[A] = {
    val pathName = getPathName(prefix, name)
    val newMappings =
      mappings.map[[X] =>> Field[?]]{
        [X] => (x: X) => x match
          case f: Field[t] =>
            f.bindUsingPrefix(pathName, data)
      }

    boundFieldsToProduct(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))
  }

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A] =
    bindToProduct(prefix, data)

  override def fill(newValue: A): Field[A] =
    newValue match {
      case tuple: Tuple =>
        val iter = tuple.productIterator
        val newMappings = mappings.map[[X] =>> Field[?]]([X] => (t: X) => t match {
          case f: Field[a] => f.fill(iter.next().asInstanceOf[a])
        })
        boundFieldsToProduct(newMappings, mirrorOpt,
          (newData, newMappings, newValue, newErrors) =>
            copy(mappings = newMappings, value = Option(newValue), errors = newErrors))

      case product: Product =>
        val iter = Tuple.fromProduct(product).productIterator
        val newMappings = mappings.map[[X] =>> Field[?]]([X] => (t: X) => t match {
          case f: Field[a] => f.fill(iter.next().asInstanceOf[a])
        })
        boundFieldsToProduct(newMappings, mirrorOpt,
          (newData, newMappings, newValue, newErrors) =>
            copy(mappings = newMappings, value = Option(newValue), errors = newErrors))

      case _ => this
    }

  override def verifying(newConstraints: Constraint[A]*): ProductField[A] =
    copy(constraints = constraints ++ newConstraints)

  override def bind(jsValue: JsValue): Field[A] =
    bind("", jsValue)

  def bind(prefix: String, jsValue: JsValue): Field[A] =
    val value = if name == null || name == "" then jsValue else jsValue \ name
    val newMappings =
      mappings.map[[X] =>> Field[?]] {
        [X] => (x: X) => x match {
          case f: Field[t] =>
            f.bind(value)
        }
      }

    boundFieldsToProduct(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))

  override def field[A](key: String): Field[A] =
    mappings
      .toList
      .collectFirst{case f : Field[A]  if f.name == key => f}
      .orNull

  /** FIXME
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: FormError): Field[A] =
    this.copy(errors = this.errors :+ error)

  override def discardingErrors: Field[A] =
    this.copy(errors = Nil)
}

case class OptionalField[A](tpe: String,
                            name: String = null,
                            value: Option[A] = None,
                            constraints:Seq[Constraint[A]] = Nil,
                            errors: Seq[FormError] = Nil,
                            elemField: Field[A]) extends Field[A] {

  override def name(name: String): OptionalField[A] =
    copy(name = name, elemField = elemField.name(name))

  override def mappings(mappings: Field[?] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def format: Option[(String, Seq[Any])] = elemField.format

  override def binder(binder: Formatter[A]): Field[A] =
    copy(elemField= elemField.binder(binder))

  override def bind(data: Map[String, Seq[String]]): Field[A] =
    bindUsingPrefix("", data)

  override def safeValue: Any =  value

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[A] =
    val boundField = elemField.bindUsingPrefix(prefix, data)
    val boundValue = boundField.value

    //ignore required field as this field is optional
    val boundFieldErrors = boundField.errors.filterNot(_.is(name, "error.required"))
    val errors = applyConstraints(boundValue.asInstanceOf[A])
    copy(value = boundValue, errors = boundFieldErrors ++ errors, elemField = boundField)

  override def fill(newValue: A): Field[A] =
    newValue match {
      case Some(value) =>
        val filledField = elemField.fill(value.asInstanceOf[A])
        copy(value = filledField.value , elemField = filledField, errors = filledField.errors)
      case _ => this
    }

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

  override def bind(jsValue: JsValue): Field[A] =
    bind("", jsValue)

  override def bind(prefix: String, jsValue: JsValue): Field[A] =
    val boundField = elemField.bind(prefix, jsValue)
    val boundValue = boundField.value

    //ignore required field as this field is optional
    val boundFieldErrors = boundField.errors.filterNot(_.is(name, "error.required"))
    val errors = applyConstraints(boundValue.asInstanceOf[A])
    copy(value = boundValue, errors = boundFieldErrors ++ errors, elemField = boundField)

  override def field[A](key: String): Field[A] =
    elemField.field(key)

  /** FIXME
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: FormError): Field[A] =
    this.copy(errors = this.errors :+ error)

  override def discardingErrors: Field[A] =
    this.copy(errors = Nil)
}

case class SeqField[A](tpe: String,
                       name: String = null,
                       value: Option[A] = None,
                       constraints:Seq[Constraint[A]] = Nil,
                       errors: Seq[FormError] = Nil,
                       elemField: Field[A],
                       boundFields: Seq[Field[A]] = Nil) extends Field[A] {

  override def name(name: String): Field[A] =
    copy(name = name, elemField = elemField.name(name))

  def indexes: Range =
    value match
      case None => 0 to 0
      case Some(xs: Seq[?]) => xs.indices

  override def mappings(mappings: Field[?] *: Tuple, mirror: Mirror.ProductOf[A]): Field[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def format: Option[(String, Seq[Any])] = elemField.format

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
    val boundFields: Seq[Field[A]] = sortedFieldNames.flatMap{name =>
      dataMap.getOrElse(name, Nil) match {
        case Nil => Seq(elemField.name(name).bind(dataMap))
        case values => values.map(value => elemField.name(name).bind(Map(name -> Seq(value))))
      }
    }

    val values = boundFields.collect{case f: Field[?] if f.value.isDefined => f.value.get}
    val errors = boundFields.collect{case f: Field[?] if f.errors.nonEmpty => f.errors}.flatten ++
      applyConstraints(values.asInstanceOf[A])

    copy(value = Option(values).asInstanceOf[Option[A]], errors = errors, boundFields = boundFields)
  }

  override def verifying(newConstraints: Constraint[A]*): Field[A] =
    copy(constraints = constraints ++ newConstraints)

  override def fill(newValue: A): Field[A] =
    val filledField = elemField.fill(newValue)
    copy(value = filledField.value, errors = filledField.errors)

  override def bind(jsValue: JsValue): Field[A] =
    bind("", jsValue)

  override def bind(prefix: String, jsValue: JsValue): Field[A] =
    val pathName = getPathName(prefix, name)
    val data = (jsValue \ pathName).asOpt[Seq[Any]] match {
      case Some(xs) =>
        xs.zipWithIndex.map(x => s"$pathName[${x._2}]" -> Seq(x._1.toString)).toMap
      case None =>
        Map.empty
    }
    bindToSeq(prefix, data)

  override def field[A](key: String): Field[A] =
    elemField.field(key)

  /** FIXME
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: FormError): Field[A] =
    this.copy(errors = this.errors :+ error)

  override def discardingErrors: Field[A] =
    this.copy(errors = Nil)
}

case class MappingField[A, B](tpe: String,
                              name: String = null,
                              value: Option[B] = None,
                              errors: Seq[FormError] = Nil,
                              constraints: Seq[Constraint[B]] = Nil,
                              delegate: Field[A],
                              delegateMapping: A => B) extends Field[B] {

  override def name(name: String): Field[B] =
    copy(name = name, delegate = delegate.name(name))

  override def mappings(mappings: Field[?] *: Tuple, mirror: ProductOf[B]): Field[B] =
    throw new IllegalArgumentException("MappingField#mappings is not supported for MappingField")

  override def format: Option[(String, Seq[Any])] =
    throw new IllegalArgumentException("MappingField#format is not supported for MappingField")

  override def binder(binder: Formatter[B]): Field[B] =
    throw new IllegalArgumentException("MappingField#binder is not supported for MappingField")

  override def fill(newValue:B): Field[B] =
    val filledDelegate = delegate.fill(newValue.asInstanceOf[A])
    val _valueOpt = filledDelegate.value.map(delegateMapping).orElse(value)
    val _errors = _valueOpt.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _valueOpt, delegate = filledDelegate, errors =_errors)

  override def bind(data: Map[String, Seq[String]]): Field[B] =
    bindUsingPrefix("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Field[B] =
    val boundDelegate = delegate.bindUsingPrefix(prefix, data)
    val _value = boundDelegate.value.map(v => delegateMapping(v)).orElse(value)
    val _errors = _value.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _value, delegate = boundDelegate , errors =_errors)

  override def bind(jsValue: JsValue): Field[B] =
    val boundDelegate = delegate.bind(jsValue)
    val _value = boundDelegate.value.map(v => delegateMapping(v)).orElse(value)
    val _errors = _value.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _value , errors =_errors)

  override def bind(prefix: String, jsValue: JsValue): Field[B] =
    copy(delegate = delegate.bind(prefix, jsValue))

  override def verifying(newConstraints: Constraint[B]*): Field[B] =
    copy(constraints = newConstraints)

  override def field[A](key: String): Field[A] =
    delegate.field(key)

  /** FIXME
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: FormError): Field[B] =
    this.copy(errors = this.errors :+ error)

  override def discardingErrors: Field[B] =
    this.copy(errors = Nil)
}