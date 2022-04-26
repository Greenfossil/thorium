package com.greenfossil.data.mapping

import Formatter.*
import com.greenfossil.commons.json.{JsArray, JsObject, JsValue}

import java.time.*
import scala.deriving.Mirror
import scala.deriving.Mirror.ProductOf

object Mapping extends MappingInlines {

  inline def apply[A](name: String): Mapping[A] =
    mapTo[A].name(name)

  inline def apply[A](name: String, mapping: Mapping[A]): Mapping[A] =
    mapping.name(name)
}

trait Mapping[A] extends ConstraintVerifier[A]{
  val tpe: String

  val name: String

  val value: Option[A]

  val errors: Seq[MappingError]

  def name(name: String): Mapping[A]

  def safeValue: Any =  if value.isDefined then value.get else None

  def mappings(mappings: Mapping[?] *: Tuple, mirror: Mirror.ProductOf[A]): Mapping[A]

  def format: Option[(String, Seq[Any])]

  def binder(binder: Formatter[A]): Mapping[A]

  def bind(data: (String, String)*): Mapping[A] =
    bind(data.groupMap(_._1)(_._2))

  def bind(data:Map[String, Seq[String]]): Mapping[A]

  def fill(newValue: A):Mapping[A]

  protected def getPathName(prefix: String, name: String): String = {
    (prefix, name) match
      case (prefix, null) => prefix
      case ("", _) => name
      case (_, _) => s"$prefix.$name"
  }

  def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Mapping[A]

  def bind(jsValue: JsValue): Mapping[A]

  def bind(prefix: String, jsValue: JsValue): Mapping[A]

  def transform[B](mappingFn: A => B): Mapping[B] =
    DelegateMapping(tpe = "#", delegate = this, delegateMapping = mappingFn)

  def map[B](mappingFn: A => B): Mapping[B] =
    DelegateMapping(tpe = "#", delegate = this, delegateMapping = mappingFn)

  override def toString: String =
    s"name:$name type:$tpe value:$value"

  /*
   * Form APIs
   */

  /**
   * Alias for method field()
   * @param key
   * @tparam A
   * @return
   */
  def apply[A](key: String): Mapping[A]

  def fold[R](hasErrors: Mapping[A] => R, success: A => R): R = 
    value match
      case Some(v) if errors.isEmpty => success(v)
      case _ => hasErrors(this)

  /**
   * Returns `true` if there is an error related to this form.
   */
  def hasErrors: Boolean = errors.nonEmpty

  /**
   * Retrieve the first error for this key.
   *
   * @param key field name.
   */
  def error(key: String): Option[MappingError] = errors.find(_.key == key)

  /**
   * Retrieve all errors for this key.
   *
   * @param key field name.
   */
  def errors(key: String): Seq[MappingError] = errors.filter(_.key == key)

  /**
   * Retrieves all global errors, i.e. errors without a key.
   *
   * @return all global errors
   */
  def globalErrors: Seq[MappingError] = errors.filter(e => !Option(e.key).exists(_.nonEmpty))

  /**
   * Adds an error to this form
   * @param error FormError
   */
  def withError(error: MappingError): Mapping[A]

  def discardingErrors: Mapping[A]

  /**
   * Adds an error to this form
   * @param key Error key
   * @param message Error message
   * @param args Error message arguments
   */
  def withError(key: String, message: String, args: Any*): Mapping[A] =
    withError(MappingError(key, message, args))

  /**
   * Adds a global error to this form
   * @param message Error message
   * @param args Error message arguments
   */
  def withGlobalError(message: String, args: String*): Mapping[A] = withError("", message, args*)

}

case class ScalarMapping[A](tpe: String,
                            name: String = null,
                            value: Option[A] = None,
                            binder: Formatter[A],
                            constraints:Seq[Constraint[A]] = Nil,
                            format: Option[(String, Seq[Any])] = None,
                            errors: Seq[MappingError] = Nil) extends Mapping[A] {

  override def toString: String = 
    s"name:$name type:$tpe binder:${if binder != null then binder.tpe else null} value:$value"

  override def name(name: String): Mapping[A] = copy(name = name)

  override def binder(binder: Formatter[A]): Mapping[A] = copy(binder = binder)

  override def mappings(mappings: Mapping[?] *: Tuple, mirror: Mirror.ProductOf[A]): Mapping[A] =
    throw new UnsupportedOperationException("ScalarField does not support mappings")

  override def fill(newValue: A): Mapping[A] =
    copy(value = Some(newValue), errors = applyConstraints(newValue))

  override def bind(data:Map[String, Seq[String]]): Mapping[A] =
    bindUsingPrefix("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Mapping[A] = 
    val pathName = getPathName(prefix, name)
    binder.bind(pathName, data) match 
      case Left(errors) => copy(errors = errors)
      case Right(value) =>
        val errors = applyConstraints(value)
        copy(value = Option(value), errors = errors)

  override def bind(jsValue: JsValue): Mapping[A] =
    bind("", jsValue)

  override def bind(prefix: String, jsValue: JsValue): Mapping[A] =
    val pathName = getPathName(prefix, name)
    (jsValue \ pathName).asOpt[Any] match
      case Some(value) => bind(Map(pathName -> Seq(value.toString)))
      case None => bind(Map.empty)

  override def verifying(newConstraints: Constraint[A]*): Mapping[A] =
    copy(constraints = constraints ++ newConstraints)

  override def apply[A](key: String): Mapping[A] = this.asInstanceOf[Mapping[A]]

  /**
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: MappingError): Mapping[A] =
   this.copy(errors = this.errors :+ error)

  override def discardingErrors: Mapping[A] =
   this.copy(errors = Nil)

}

case class ProductMapping[A](tpe: String,
                             name: String = null,
                             value: Option[A] = None,
                             constraints:Seq[Constraint[A]] = Nil,
                             format: Option[(String, Seq[Any])] = None,
                             errors: Seq[MappingError] = Nil,
                             mappings: Mapping[?] *: Tuple = null,
                             mirrorOpt: Option[Mirror.ProductOf[A]] = None) extends Mapping[A] {

  override def name(name: String): Mapping[A] =
    copy(name = name)

  override def mappings(mappings: Mapping[?] *: Tuple, mirror: Mirror.ProductOf[A]): Mapping[A] =
    copy(mappings = mappings, mirrorOpt = Option(mirror))

  def mapping(name: String): Option[Mapping[?]] =
    mappings.toList.asInstanceOf[List[Mapping[?]]].find(_.name == name)

  override def binder(binder: Formatter[A]): Mapping[A] =
    throw new UnsupportedOperationException("Product field does not support setting of binder")

  override def bind(data: Map[String, Seq[String]]): Mapping[A] =
    bindToProduct("", data)

  private def bindToProduct(prefix: String, data: Map[String, Seq[String]]): Mapping[A] = {
    val pathName = getPathName(prefix, name)
    val newMappings =
      mappings.map[[X] =>> Mapping[?]]{
        [X] => (x: X) => x match
          case f: Mapping[t] =>
            f.bindUsingPrefix(pathName, data)
      }

    boundFieldsToProduct(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))
  }

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Mapping[A] =
    bindToProduct(prefix, data)

  override def fill(newValue: A): Mapping[A] =
    newValue match {
      case tuple: Tuple =>
        val iter = tuple.productIterator
        val newMappings = mappings.map[[X] =>> Mapping[?]]([X] => (t: X) => t match {
          case f: Mapping[a] => f.fill(iter.next().asInstanceOf[a])
        })
        boundFieldsToProduct(newMappings, mirrorOpt,
          (newData, newMappings, newValue, newErrors) =>
            copy(mappings = newMappings, value = Option(newValue), errors = newErrors))

      case product: Product =>
        val iter = Tuple.fromProduct(product).productIterator
        val newMappings = mappings.map[[X] =>> Mapping[?]]([X] => (t: X) => t match {
          case f: Mapping[a] => f.fill(iter.next().asInstanceOf[a])
        })
        boundFieldsToProduct(newMappings, mirrorOpt,
          (newData, newMappings, newValue, newErrors) =>
            copy(mappings = newMappings, value = Option(newValue), errors = newErrors))

      case _ => this
    }

  override def verifying(newConstraints: Constraint[A]*): Mapping[A] =
    copy(constraints = constraints ++ newConstraints)

  override def bind(jsValue: JsValue): Mapping[A] =
    bind("", jsValue)

  def bind(prefix: String, jsValue: JsValue): Mapping[A] =
    val value = if name == null || name == "" then jsValue else jsValue \ name
    val newMappings =
      mappings.map[[X] =>> Mapping[?]] {
        [X] => (x: X) => x match {
          case f: Mapping[t] =>
            f.bind(value)
        }
      }

    boundFieldsToProduct(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))

  override def apply[A](key: String): Mapping[A] =
    mappings
      .toList
      .collectFirst{case f : Mapping[A]  if f.name == key => f}
      .orNull

  /**
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: MappingError): Mapping[A] =
    this.copy(errors = this.errors :+ error)

  override def discardingErrors: Mapping[A] =
    this.copy(errors = Nil)
}

case class OptionalMapping[A](tpe: String,
                              name: String = null,
                              value: Option[A] = None,
                              constraints:Seq[Constraint[A]] = Nil,
                              errors: Seq[MappingError] = Nil,
                              elemField: Mapping[A]) extends Mapping[A] {

  override def name(name: String): OptionalMapping[A] =
    copy(name = name, elemField = elemField.name(name))

  override def mappings(mappings: Mapping[?] *: Tuple, mirror: Mirror.ProductOf[A]): Mapping[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def format: Option[(String, Seq[Any])] = elemField.format

  override def binder(binder: Formatter[A]): Mapping[A] =
    copy(elemField= elemField.binder(binder))

  override def bind(data: Map[String, Seq[String]]): Mapping[A] =
    bindUsingPrefix("", data)

  override def safeValue: Any =  value

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Mapping[A] =
    val boundField = elemField.bindUsingPrefix(prefix, data)
    val boundValue = boundField.value

    //ignore required field as this field is optional
    val boundFieldErrors = boundField.errors.filterNot(_.is(name, "error.required"))
    val errors = applyConstraints(boundValue.asInstanceOf[A])
    copy(value = boundValue, errors = boundFieldErrors ++ errors, elemField = boundField)

  override def fill(newValue: A): Mapping[A] =
    newValue match
      case Some(value) =>
        val filledField = elemField.fill(value.asInstanceOf[A])
        copy(value = filledField.value , elemField = filledField, errors = filledField.errors)
      case _ => this

  override def verifying(newConstraints: Constraint[A]*): Mapping[A] =
    copy(constraints = constraints ++ newConstraints)

  override def bind(jsValue: JsValue): Mapping[A] =
    bind("", jsValue)

  override def bind(prefix: String, jsValue: JsValue): Mapping[A] =
    val boundField = elemField.bind(prefix, jsValue)
    val boundValue = boundField.value

    //ignore required field as this field is optional
    val boundFieldErrors = boundField.errors.filterNot(_.is(name, "error.required"))
    val errors = applyConstraints(boundValue.asInstanceOf[A])
    copy(value = boundValue, errors = boundFieldErrors ++ errors, elemField = boundField)

  override def apply[A](key: String): Mapping[A] =
    elemField.apply(key)

  /**
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: MappingError): Mapping[A] =
    this.copy(errors = this.errors :+ error)

  override def discardingErrors: Mapping[A] =
    this.copy(errors = Nil)
}

case class SeqMapping[A](tpe: String,
                         name: String = null,
                         value: Option[A] = None,
                         constraints:Seq[Constraint[A]] = Nil,
                         errors: Seq[MappingError] = Nil,
                         elemField: Mapping[A],
                         boundFields: Seq[Mapping[A]] = Nil) extends Mapping[A] {

  override def name(name: String): Mapping[A] =
    copy(name = name, elemField = elemField.name(name))

  def indexes: Range =
    value match
      case None => 0 to 0
      case Some(xs: Seq[?]) => xs.indices

  override def mappings(mappings: Mapping[?] *: Tuple, mirror: Mirror.ProductOf[A]): Mapping[A] =
    copy(elemField = elemField.mappings(mappings = mappings, mirror))

  override def format: Option[(String, Seq[Any])] = elemField.format

  override def binder(binder: Formatter[A]): Mapping[A] =
    copy(elemField = elemField.binder(binder))

  override def bind(data: Map[String, Seq[String]]): Mapping[A] =
    bindToSeq("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Mapping[A] =
    bindToSeq(prefix, data)

  private def bindToSeq(prefix: String, dataMap: Map[String, Seq[String]]): Mapping[A] = {

    val pathName = getPathName(prefix, name)
    val keyMatchRegex = s"$pathName(\\[\\d*])?(\\..*)?"
    val keyReplaceRegex = s"($pathName(\\[(\\d*)])?)(\\.(.*))?"  // keyName is group 1, index is group 3

    /**
     * sorted according to their dataMap index if available.
     * if index is not available it will place in front
     */
    val sortedFieldNames =
      dataMap
        .toList
        .collect { case (key, _) if key.matches(keyMatchRegex) =>
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
    val boundFields: Seq[Mapping[A]] = sortedFieldNames.flatMap{ name =>
      dataMap.getOrElse(name, Nil) match
        case Nil => Seq(elemField.name(name).bind(dataMap))
        case values => values.map(value => elemField.name(name).bind(Map(name -> Seq(value))))
    }

    val values = boundFields.collect{case f: Mapping[?] if f.value.isDefined => f.value.get}
    val errors = boundFields.collect{case f: Mapping[?] if f.errors.nonEmpty => f.errors}.flatten ++
      applyConstraints(values.asInstanceOf[A])

    copy(value = Option(values).asInstanceOf[Option[A]], errors = errors, boundFields = boundFields)
  }

  override def verifying(newConstraints: Constraint[A]*): Mapping[A] =
    copy(constraints = constraints ++ newConstraints)

  override def fill(newValue: A): Mapping[A] =
    newValue match
      case xs: Seq[?] =>
        val filledFields = xs.map(x => elemField.fill(x.asInstanceOf[A]))
        val errors = filledFields.collect{case f: Mapping[?] if f.errors.nonEmpty => f.errors}.flatten ++
          applyConstraints(newValue)
        copy(value = Option(newValue), errors = errors, boundFields = filledFields)
      case _ =>
        val filledField = elemField.fill(newValue)
        val errors = applyConstraints(newValue)
        copy(value = Option(newValue), errors = errors, boundFields = Seq(filledField))

  override def bind(jsValue: JsValue): Mapping[A] =
    bind("", jsValue)

  override def bind(prefix: String, jsValue: JsValue): Mapping[A] =
    val pathName = getPathName(prefix, name)
    val data = (jsValue \ pathName).asOpt[Seq[Any]] match {
      case Some(xs) =>
        xs.zipWithIndex.map(x => s"$pathName[${x._2}]" -> Seq(x._1.toString)).toMap
      case None =>
        Map.empty
    }
    bindToSeq(prefix, data)

  override def apply[A](key: String): Mapping[A] =
    elemField.apply(key)

  def boundField(index: Int, elemName: String): Mapping[A] =
    if boundFields.indices.contains(index)
    then {
      boundFields(index) match 
        case s: ScalarMapping[A] => s
        case p: ProductMapping[A] =>
          p.mapping(elemName)
            .getOrElse(elemField)
            .asInstanceOf[Mapping[A]]
    }
    else elemField

  /**
   * Adds an error to this form
   * @param error FormError
   */
  override def withError(error: MappingError): Mapping[A] =
    this.copy(errors = this.errors :+ error, boundFields = boundFields.map(_.withError(error)))

  override def discardingErrors: Mapping[A] =
    this.copy(errors = Nil, boundFields = boundFields.map(_.discardingErrors))
}

case class DelegateMapping[A, B](tpe: String,
                                 name: String = null,
                                 value: Option[B] = None,
                                 errors: Seq[MappingError] = Nil,
                                 constraints: Seq[Constraint[B]] = Nil,
                                 delegate: Mapping[A],
                                 delegateMapping: A => B) extends Mapping[B] {

  override def name(name: String): Mapping[B] =
    copy(name = name, delegate = delegate.name(name))

  override def mappings(mappings: Mapping[?] *: Tuple, mirror: ProductOf[B]): Mapping[B] =
    throw new IllegalArgumentException("MappingField#mappings is not supported for MappingField")

  override def format: Option[(String, Seq[Any])] =
    throw new IllegalArgumentException("MappingField#format is not supported for MappingField")

  override def binder(binder: Formatter[B]): Mapping[B] =
    throw new IllegalArgumentException("MappingField#binder is not supported for MappingField")

  override def fill(newValue:B): Mapping[B] =
    val filledDelegate = if newValue != null then delegate.fill(newValue.asInstanceOf[A]) else delegate
    val _valueOpt = filledDelegate.value.map(delegateMapping).orElse(value)
    val _errors = _valueOpt.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _valueOpt, delegate = filledDelegate, errors =_errors)

  override def bind(data: Map[String, Seq[String]]): Mapping[B] =
    bindUsingPrefix("", data)

  override def bindUsingPrefix(prefix: String, data: Map[String, Seq[String]]): Mapping[B] =
    val boundDelegate = delegate.bindUsingPrefix(prefix, data)
    val _value = boundDelegate.value.map(v => delegateMapping(v)).orElse(value)
    val _errors = _value.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _value, delegate = boundDelegate , errors =_errors)

  override def bind(jsValue: JsValue): Mapping[B] =
    val boundDelegate = delegate.bind(jsValue)
    val _value = boundDelegate.value.map(v => delegateMapping(v)).orElse(value)
    val _errors = _value.map(v => applyConstraints(v)).getOrElse(Nil)
    copy(value =  _value , errors =_errors)

  override def bind(prefix: String, jsValue: JsValue): Mapping[B] =
    copy(delegate = delegate.bind(prefix, jsValue))

  override def verifying(newConstraints: Constraint[B]*): Mapping[B] =
    copy(constraints = newConstraints)

  override def apply[A](key: String): Mapping[A] =
    delegate.apply(key)
  
  override def safeValue: Any =
   delegate.value.map(v => delegateMapping(v)).getOrElse(value.orNull)

  /**
   * Adds an error to this form
   *
   * @param error FormError
   */
  override def withError(error: MappingError): Mapping[B] =
    this.copy(errors = this.errors :+ error, delegate = delegate.withError(error))

  override def discardingErrors: Mapping[B] =
    this.copy(errors = Nil, delegate = delegate.discardingErrors)
}