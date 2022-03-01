package com.greenfossil.webserver.data

import com.greenfossil.commons.json.JsValue
import com.linecorp.armeria.common.HttpMethod

type FormTupleMappings[Xs] <: Tuple = Xs match {
  case EmptyTuple => EmptyTuple
  case (String, Field[a]) *: xs => a *: FormTupleMappings[xs]
}

type FormClassMappings[Xs] <: Tuple = Xs match {
  case EmptyTuple => EmptyTuple
  case (String, Field[a]) *: xs => a *: FormTupleMappings[xs]
}


object Form {

//  def apply[A](mappings: A): Form[MappingsTuple[A]] = ???

  def asTuple[A](mappings: A): Form[FormTupleMappings[A]] =
    Form[FormTupleMappings[A]](mappings.asInstanceOf[Tuple])

  import scala.deriving.Mirror
  def asClass[A](mappings: Tuple)(using m: Mirror.Of[A]): Form[A] =

    Form[A](mappings)
}

case class Form[A](mappings: Tuple, data: Map[String, Any] = Map.empty , errors: Seq[FormError] = Nil, value: Option[A] = None) {

  import scala.deriving.Mirror

  def fill(values: A): Form[A] =
    val data = values match {
      case _values: Tuple =>
       tupleToData(_values)

      case caseclass: Product =>
        val tuple = Tuple.fromProduct(caseclass)
        tupleToData(tuple)

      case _ => Map.empty
    }
    copy(data= data)

  def bindFromRequest()(using request: com.greenfossil.webserver.Request): Form[A] =
    val querydata: Map[String, Seq[String]] =
      request.method() match {
        case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Map.empty
        case _ => Map.empty //FIXME - request.queryString
      }
    request match {
      case req if req.asFormUrlEncoded.nonEmpty =>
        bind(req.asFormUrlEncoded ++querydata)
      case req if req.asMultipartFormData.bodyPart.nonEmpty =>
        bind(req.asMultipartFormData.asFormUrlEncoded ++ querydata)
      case req if req.asJson.asOpt.isDefined =>
        bind(req.asJson, querydata)
    }

  def bind(data: Map[String, Seq[String]]): Form[A] = {
      ???
  }

  def bind(js: JsValue, query: Map[String, Seq[String]]): Form[A] = {
    ???
  }

  private def tupleToData(values: Tuple): Map[String, Any] = {
    val xs = (0 until mappings.productArity) .map { i =>
      val (name: String, f) = mappings.productElement(i)
      val value = values.productElement(i)
      (name,value)
    }
    xs.toMap
  }

  def fold[R](hasErrors: Form[A] => R, success: A => R): R = value match {
    case Some(v) if errors.isEmpty => success(v)
    case _ => hasErrors(this)
  }

  def apply(key: String): Field[_] =
    Field(this, key, Nil, data.get(key))

}

trait FieldMapper {
  def of[A](name:String): Field[A]
}

object Field {
  def apply[A](name: String): Field[A] =
    Field(null, name, Nil, None)
}

case class Field[A](form: Form[_], name: String, errors: Seq[FormError], value: Option[A])

def longNumber: Field[Long] = Field("Long")
def text: Field[String] = Field("String")
def seq[A] : Field[Seq[A]] = Field("Seq")

case class FormError(key: String, messages: Seq[String], args: Seq[Any] = Nil)