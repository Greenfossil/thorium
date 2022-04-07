package com.greenfossil.commons.data

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

import java.time.LocalDate
import scala.util.Try

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

  def apply[A](name: String,  f: Field[A]): Form[A] =
    new Form[A](mappings = f.name(name) *: EmptyTuple)

  /**
   *
   * @param nameValueTuple - a name-value pair tuple
   * @tparam A
   * @return
   */
  def tuple[A <: Tuple](nameValueTuple: A): Form[FieldTypeExtractor[A]] =
    new Form[FieldTypeExtractor[A]](toNamedFieldTuple(nameValueTuple))

  import scala.deriving.*
  /**
   *
   * @param m - Mirror.of[A]
   * @param nameValueTuple - a name-value pair tuple using  case class [A] members i.e. (fieldLabel -> value)
   * @tparam A - case class type A
   * @return - Form[A]
   */
  def mapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Form[A] =
    new Form[A](toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))

  def toNamedFieldTuple(tuple: Tuple): Field[_] *: Tuple =
    tuple.map[[X] =>> Field[_]]([X] => (x: X) =>
      x match
        case (name: String, f: Field[_]) => f.name(name)
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
                   mirrorOpt: Option[scala.deriving.Mirror.ProductOf[T]] = None) extends ConstraintVerifier[Form, T] {

  val name = ""

  def fill(values: T): Form[T] =
    val bindedFields  = values match {
      case _values: Tuple =>
        fillValuesToFields(_values)

      case caseclass: Product =>
        val tuple = Tuple.fromProduct(caseclass)
        fillValuesToFields(tuple)

      case value => fillValuesToFields(Tuple1(value))
    }
    val dataMap = bindedFields.toList.collect{
      case f: Field[_] => f.name -> f.value.orNull
    }.toMap

    copy(mappings = bindedFields, value = Option(values), data = dataMap)

  def bindFromRequest()(using request: com.greenfossil.webserver.Request): Form[T] =
    val querydata: List[(String, String)] =
      request.method() match {
        case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Nil
        case _ => request.queryParamsList
      }
    // FIXME improve the validation
    request match {
      case req if req.asFormUrlEncoded.nonEmpty =>
        bind(req.asFormUrlEncoded, querydata)

//      case req if req.asMultipartFormData.bodyPart.nonEmpty =>
//        bind(req.asMultipartFormData.asFormUrlEncoded ++ querydata)

      case req if Try(req.asJson).isSuccess =>
        bind(req.asJson, querydata)

      case req =>
        ???
    }

  def bind(data: (String, String)*): Form[T] =
    bind(Map.empty,  data)

  def bind(data: Map[String, Seq[String]], queryData: Seq[(String, String)]): Form[T] =
    bind(data ++ queryData.groupMap(_._1)(_._2))

  def bind(data: Map[String, Seq[String]]): Form[T] =
    val bindedFields = 
        mappings.map[[A] =>> Field[_]]{
          [X] => (x: X) => x match
            case f: Field[t] => f.bind(data)
        }
    updateBindedFields(bindedFields)

  def bind(js: JsValue, query: List[(String, String)] = Nil): Form[T] = {
    val bindedFields = bindJsValueToMappings(mappings, js, query)
    updateBindedFields(bindedFields)
  }

  private def updateBindedFields(newMappings: Field[_] *: Tuple): Form[T] = {
    bindedFieldsToValue(newMappings, mirrorOpt,
      (newData, newMappings, newValue, newErrors) =>
        copy(data= newData, mappings = newMappings, value = Option(newValue), errors = newErrors)
    )
  }

  private def fillValuesToFields(values: Product): Field[_] *: Tuple = {
    val valuesIter = values.productIterator
    val bindedFields = mappings.map[[F] =>> Field[_]](
      [F] => (f: F) => f match {
        case f: Field[a] =>
          f.fill(valuesIter.nextOption().asInstanceOf[Option[a]])
      })
    bindedFields
  }

  def fold[R](hasErrors: Form[T] => R, success: T => R): R = value match {
    case Some(v) if errors.isEmpty => success(v)
    case _ => hasErrors(this)
  }

  def apply[A](key: String): Field[A] =
    mappings
      .toList
      .collectFirst{case f : Field[A]  if f.name == key => f}
      .orNull

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
  def globalErrors: Seq[FormError] = errors.filter(_.key.isEmpty)

  /**
    * Adds an error to this form
    * @param error FormError
    */
  def withError(error: FormError): Form[T] = this.copy(errors = this.errors :+ error)

  /**
    * Adds an error to this form
    * @param key Error key
    * @param message Error message
    * @param args Error message arguments
    */
  def withError(key: String, message: String, args: Any*): Form[T] = withError(FormError(key, message, args))

  /**
    * Adds a global error to this form
    * @param message Error message
    * @param args Error message arguments
    */
  def withGlobalError(message: String, args: String*): Form[T] = withError("", message, args*)

  override def verifying(addConstraints: Constraint[T]*): Form[T] =
    copy(constraints = constraints ++ addConstraints)

  def discardingErrors: Form[T] = this.copy(errors = Nil)

}