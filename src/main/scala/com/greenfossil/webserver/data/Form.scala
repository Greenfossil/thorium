package com.greenfossil.webserver.data

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

import java.time.LocalDate

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

object Form {

  type FormMappings[Xs <: Tuple] <: Tuple = Xs match {
    case EmptyTuple => Xs
    case Field[t] *: ts => t *: FormMappings[ts]
    case (String, Field[t]) *: ts => t *: FormMappings[ts]
  }

  type NameFieldMappings[X <:Tuple] <: Tuple = X match {
    case EmptyTuple => X
    case t *: ts => Field[t] *: NameFieldMappings[ts]
  }

  def asTuple[A  <: (String, Field[_]) *: Tuple](tuple: A): TupleForm[FormMappings[A]] =
    val fs = tuple.map[[X] =>> Field[_]]([X] => (x: X) => x match {
      case (name: String, f: Field[_]) => f.copy(name = name)
    })
    TupleForm[FormMappings[A]](fs)

  import scala.deriving.*

  def asClass[A](using m: Mirror.ProductOf[A])(tuple: Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]): CaseClassForm[A, Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]] =
    val xs = tuple.map[[X] =>> Field[_]]([X] => (x: X) =>
      x match {
        case (name: String, f: Field[_]) => f.copy(name = name)
      }
    )
    CaseClassForm[A, Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]](xs.asInstanceOf[Field[_] *: Tuple])

}

/**
 * F Bound type
 * @tparam F
 * @tparam T
 */
trait Form[F <: Form[F, T], T]{ self: F =>

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

  def bindFromRequest()(using request: com.greenfossil.webserver.Request): Form[F, T] =
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

  def bind(data: Map[String, Seq[String]]): Form[F, T] = {
    val newMappings = mappings.map[[A] =>> Field[_]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.copy(value = Field.toValueOf(f.tpe, data.get(f.name).orNull))
    }
    setData(data).setMappings(newMappings)
  }

  def bind(js: JsValue, query: Map[String, Seq[String]]): Form[F, T] = {
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

  def fold[R](hasErrors: Form[F, T] => R, success: T => R): R = value match {
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

case class CaseClassForm[T, U](mappings: Field[_] *: Tuple = null, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[T] = None) extends Form[CaseClassForm[T, U], T]{
  override def setMappings(mapping: Field[_] *: Tuple): CaseClassForm[T, U] = copy(mappings = mapping)

  override def setData(data: Map[String, Any]): CaseClassForm[T, U] = copy(data = data)

  override def setValue(value: T): CaseClassForm[T, U] = copy(value = Option(value))

  override def setErrors(errors: Seq[FormError]): CaseClassForm[T, U] = copy(errors = errors)
}

case class TupleForm[T <: Tuple](mappings: Field[_] *: Tuple, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[T] = None) extends Form[TupleForm[T], T] {
  override def setMappings(mapping: Field[_] *: Tuple): TupleForm[T] = copy(mappings = mapping)

  override def setData(data: Map[String, Any]): TupleForm[T] = copy(data = data)

  override def setValue(value: T): TupleForm[T] = copy(value = Option(value))

  override def setErrors(errors: Seq[FormError]): TupleForm[T] = copy(errors = errors)
}