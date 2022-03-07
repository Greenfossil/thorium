package com.greenfossil.webserver.data

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

/**
 * F Bound type
 * @tparam F
 * @tparam T
 */
trait FormMappings[F <: FormMappings[F, T], T]{ self: F =>

  val mappings: Field[_] *: Tuple

  def setMappings(mapping: Field[_] *: Tuple): F

  val data: Map[String, Any]

  def setData(data: Map[String, Any]): F

  val value: Option[T]

  def setValue(value: T): F

  val errors: Seq[FormError]

  def setErrors(errors: Seq[FormError]): F

  def fill(values: T): F =
    val filledFields  = values match {
      case _values: Tuple =>
        tupleToData(_values)

      case caseclass: Product =>
        val tuple = Tuple.fromProduct(caseclass)
        tupleToData(tuple)
    }
    setMappings(filledFields)

  def bindFromRequest()(using request: com.greenfossil.webserver.Request): FormMappings[F, T] =
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

  def bind(data: Map[String, Seq[String]]): FormMappings[F, T] = {
    val newMappings = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.copy(value = Field.toValueOf(f.tpe, data.get(f.name).orNull))
    }
    setData(data).setMappings(newMappings)
  }

  def bind(js: JsValue, query: Map[String, Seq[String]]): FormMappings[F, T] = {
    //WIP
    val newMappings = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.copy(value = Field.toValueOf(f.tpe, (js \ f.name).asOpt[String]))
    }
    setData(null/*FIXME*/).setMappings(newMappings)
  }

  private def tupleToData(values: Product): Field[_] *: Tuple = {
    val valuesIter = values.productIterator
    val filledFields = mappings.map[[F] =>> Field[_]](
      [F] => (f: F) => f match {
        case f: Field[_] => f.copy(value = valuesIter.nextOption())
      })
    filledFields
  }

  def fold[R](hasErrors: FormMappings[F, T] => R, success: T => R): R = value match {
    case Some(v) if errors.isEmpty => success(v)
    case _ => hasErrors(this)
  }

  inline def apply[A](key: String): Field[A] =
    mappings
      .productIterator
      .find(_.asInstanceOf[Field[A]].name == key)
      .map(_.asInstanceOf[Field[A]])
      .getOrElse(Field.of[A])

}