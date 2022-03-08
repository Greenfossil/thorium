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

  def tuple[A <: Tuple](tuple: A): Form[FieldTypeExtractor[A]] =
    Form[FieldTypeExtractor[A]](toNamedFieldTuple(tuple).asInstanceOf[Field[_] *: Tuple])

  import scala.deriving.*
  def mapping[A](using m: Mirror.ProductOf[A])(tuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Form[A] =
    Form[A](toNamedFieldTuple(tuple).asInstanceOf[Field[_] *: Tuple])

  private def toNamedFieldTuple(tuple: Tuple): Tuple =
    tuple.map[[X] =>> Field[_]]([X] => (x: X) =>
      x match {
        case (name: String, f: Field[_]) => f.copy(name = name)
      }
    )

}

/**
 *
 * @param mappings
 * @param data
 * @param errors
 * @param value
 * @tparam T
 */
case class Form[T](mappings: Field[_] *: Tuple, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[T] = None){

  def setMappings(mappings: Field[_] *: Tuple): Form[T] = copy(mappings = mappings)

  def setData(data: Map[String, Any]): Form[T] = copy(data = data)

  def setValue(value: T): Form[T] = copy(value = Option(value))

  def setErrors(errors: Seq[FormError]): Form[T] = copy(errors = errors)

  def fill(values: T): Form[T] =
    val filledFields  = values match {
      case _values: Tuple =>
        tupleToData(_values)

      case caseclass: Product =>
        val tuple = Tuple.fromProduct(caseclass)
        tupleToData(tuple)
    }
    setMappings(filledFields)

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

  def bind(data: Map[String, Seq[String]]): Form[T] = {
    val newMappings = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        /*
         * For Seq[_] field type, the type param can have a square bracket "[" after the key.
         * e.g. "foo[]", "foo[0]", "foo"
         */
        case f: Field[t] if f.tpe.startsWith("[") =>
          val values = data.getOrElse(f.name,
            data.filter(_._1.startsWith(f.name + "[")).flatMap(_._2).toList
          )
          f.copy(value = Field.toValueOf(f.tpe, values))

        case f: Field[t] => f.copy(value = Field.toValueOf(f.tpe, data.get(f.name).orNull))
    }
    setData(data).setMappings(newMappings)
  }

  def bind(js: JsValue, query: Map[String, Seq[String]]): Form[T] = {
    //WIP
    val newMappings = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.copy(value = Field.toValueOf(f.tpe, (js \ f.name).asOpt[Any]))
    }
    setData(null/*FIXME*/).setMappings(newMappings).setValue(null.asInstanceOf[T])
  }

  private def tupleToData(values: Product): Field[_] *: Tuple = {
    val valuesIter = values.productIterator
    val filledFields = mappings.map[[F] =>> Field[_]](
      [F] => (f: F) => f match {
        case f: Field[_] => f.copy(value = valuesIter.nextOption())
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

}