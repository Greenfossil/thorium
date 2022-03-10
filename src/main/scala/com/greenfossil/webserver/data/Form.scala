package com.greenfossil.webserver.data

import java.time.LocalDate

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

object Form {

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

  /**
   *
   * @param nameValueTuple - a name-value pair tuple
   * @tparam A
   * @return
   */
  def tuple[A <: Tuple](nameValueTuple: A): Form[FieldTypeExtractor[A]] =
    Form[FieldTypeExtractor[A]](toNamedFieldTuple(nameValueTuple))

  import scala.deriving.*
  /**
   *
   * @param m - Mirror.of[A]
   * @param nameValueTuple - a name-value pair tuple using  case class [A] members i.e. (fieldLabel -> value)
   * @tparam A - case class type A
   * @return - Form[A]
   */
  def mapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Form[A] =
    Form[A](toNamedFieldTuple(nameValueTuple), mirrorrOpt = Some(m))

  private def toNamedFieldTuple(tuple: Tuple): Field[_] *: Tuple =
    tuple.map[[X] =>> Field[_]]([X] => (x: X) =>
      x match
        case (name: String, f: Field[_]) => f.copy(name = name)
    ).asInstanceOf[Field[_] *: Tuple]

}

/**
 *
 * @param mappings
 * @param data
 * @param errors
 * @param value
 * @tparam T
 */
case class Form[T](mappings: Field[_] *: Tuple,
                   data: Map[String, Any] = Map.empty,
                   errors: Seq[FormError] = Nil,
                   value: Option[T] = None,
                   constraints: Seq[Constraint[T]] = Nil,
                   mirrorrOpt: Option[scala.deriving.Mirror.ProductOf[T]] = None) extends ConstraintVerifier[Form, T]("", constraints) {

  def fill(values: T): Form[T] =
    val filledFields  = values match {
      case _values: Tuple =>
        tupleToData(_values)

      case caseclass: Product =>
        val tuple = Tuple.fromProduct(caseclass)
        tupleToData(tuple)
    }
    val dataMap = filledFields.toList.map{
      case f: Field[_] => f.name -> f.value.orNull
    }.toMap
    copy(mappings = filledFields, value = Option(values), data = dataMap)

  def bindFromRequest()(using request: com.greenfossil.webserver.Request): Form[T] =
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

  def bind(data: Map[String, Any]): Form[T] = {
    val bindedFields = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        /*
         * For Seq[_] field type, the type param can have a square bracket "[" after the key.
         * e.g. "foo[]", "foo[0]", "foo"
         */
        case f: Field[t] if f.tpe.startsWith("[") =>
          val values = data.getOrElse(f.name,
            /*
             * If the data is a array form param, concatenate all values that matches the key.
             */
            data.filter(_._1.startsWith(f.name + "[")).values.flatMap{
              case s: Seq[_] => s
              case s => Seq(s)
            }
          )
          f.bind(values)

        case f: Field[t] => f.bind(data.get(f.name).orNull)
    }

    updateBindedFields(bindedFields)
  }

  def bind(js: JsValue, query: Map[String, Any]): Form[T] = {
    val bindedFields = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.bind(js \ f.name)
    }
    updateBindedFields(bindedFields)
  }

  private def updateBindedFields(newMappings: Field[_] *: Tuple): Form[T] = {

    val newData = newMappings.toList.map{ case f: Field[_] => f.name -> f.value.orNull }.toMap

    val fieldsErrors =  newMappings.toList.flatMap{ case f: Field[t] => f.errors }

    val bindedFieldValues = newMappings.map[[A] =>> Any]{
      [X] => (x: X) => x match
        case f: Field[t] => f.value.orNull
    }

    val bindedValue: T = mirrorrOpt.map(m => m.fromProduct(bindedFieldValues)).getOrElse(bindedFieldValues.asInstanceOf[T])

    val formConstraintsErrors = applyConstraints(bindedValue)

    copy(data= newData, mappings = newMappings, value = Option(bindedValue), errors = formConstraintsErrors ++ fieldsErrors)
  }

  private def tupleToData(values: Product): Field[_] *: Tuple = {
    val valuesIter = values.productIterator
    val filledFields = mappings.map[[F] =>> Field[_]](
      [F] => (f: F) => f match {
        case f: Field[_] => f.bind(valuesIter.nextOption())
      })
    filledFields
  }

  def fold[R](hasErrors: Form[T] => R, success: T => R): R = value match {
    case Some(v) if errors.isEmpty => success(v)
    case _ => hasErrors(this)
  }

  def apply[A](key: String): Field[A] =
    mappings
      .productIterator
      .find(_.asInstanceOf[Field[A]].name == key)
      .map(_.asInstanceOf[Field[A]])
      .getOrElse(Field.of[Nothing].copy(name = key))
      .asInstanceOf[Field[A]]

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
  def globalError: Option[FormError] = globalErrors.headOption

  /**
   * Retrieves all global errors, i.e. errors without a key.
   *
   * @return all global errors
   */
  def globalErrors: Seq[FormError] = errors.filter(_.key.isEmpty)

  override def verifying(addConstraints: Constraint[T]*): Form[T] =
    copy(constraints = constraints ++ addConstraints)

}