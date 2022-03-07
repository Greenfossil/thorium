package com.greenfossil.webserver.data

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

import java.time.LocalDate

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

  def asTuple[A  <: (String, Field[_]) *: Tuple](tuple: A): TupleMapper[FormMappings[A]] =
    val fs = tuple.map[[X] =>> Field[_]]([X] => (x: X) => x match {
      case (name: String, f: Field[_]) => f.copy(name = name)
    })
    TupleMapper[FormMappings[A]](fs)

  import scala.deriving.*

  def asClass[A](using m: Mirror.ProductOf[A])(tuple: Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]): CaseClassMapper[A, Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]] =
    val xs = tuple.map[[X] =>> Field[_]]([X] => (x: X) =>
      x match {
        case (name: String, f: Field[_]) => f.copy(name = name)
      }
    )
    CaseClassMapper[A, Tuple.Zip[m.MirroredElemLabels, NameFieldMappings[m.MirroredElemTypes]]](xs.asInstanceOf[Field[_] *: Tuple])

}

case class CaseClassMapper[T, U](mappings: Field[_] *: Tuple = null, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[T] = None) extends FormMappings[CaseClassMapper[T, U], T]{
  override def setMappings(mapping: Field[_] *: Tuple): CaseClassMapper[T, U] = copy(mappings = mapping)

  override def setData(data: Map[String, Any]): CaseClassMapper[T, U] = copy(data = data)

  override def setValue(value: T): CaseClassMapper[T, U] = copy(value = Option(value))

  override def setErrors(errors: Seq[FormError]): CaseClassMapper[T, U] = copy(errors = errors)
}

case class TupleMapper[T <: Tuple](mappings: Field[_] *: Tuple, data: Map[String, Any] = Map.empty, errors: Seq[FormError] = Nil, value: Option[T] = None) extends FormMappings[TupleMapper[T], T] {
  override def setMappings(mapping: Field[_] *: Tuple): TupleMapper[T] = copy(mappings = mapping)

  override def setData(data: Map[String, Any]): TupleMapper[T] = copy(data = data)

  override def setValue(value: T): TupleMapper[T] = copy(value = Option(value))

  override def setErrors(errors: Seq[FormError]): TupleMapper[T] = copy(errors = errors)
}