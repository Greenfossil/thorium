//package com.greenfossil.commons.data
//
//import com.greenfossil.commons.json.JsValue
//import com.greenfossil.commons.data.Field.fieldType
//import com.greenfossil.commons.data.Form.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}
//
//import scala.deriving.Mirror
//
//object Field extends FieldSupport {
//
//  inline def of[A]: Field[A] = Field(fieldType[A])
//
//  inline def of[A](name: String): Field[A] = Field(fieldType[A], name = name)
//
//}
//
//case class Field[A](tpe: String,
//                    form: Form[_] = null,
//                    name: String = null,
//                    constraints:Seq[Constraint[A]] = Nil,
//                    format: Option[(String, Seq[Any])] = None,
//                    errors: Seq[FormError] = Nil,
//                    value: Option[A] = None,
//
//                    /*
//                     * these params are meant for use in embedded class use
//                     */
//                    mappings: Field[_] *: Tuple = null,
//                    mirrorOpt: Option[scala.deriving.Mirror.ProductOf[A]] = None) extends ConstraintVerifier[Field, A](name, constraints) {
//
//  def isOptional: Boolean = tpe.startsWith("?")
//  def isSeq: Boolean = tpe.startsWith("[") && !isSeqProduct
//  def isProduct: Boolean = tpe.startsWith("C-")
//  def isSeqProduct: Boolean = tpe.startsWith("[C-")
//
//  def rawValue: Any = if isOptional then value else value.orNull
//
//  def fill(newValue: A):Field[A] = copy(value = Option(newValue))
//
//  def fill(newValueOpt: Option[?]): Field[A] = copy(value = newValueOpt.asInstanceOf[Option[A]])
//
//  def bind(any: Any): Field[A] =
//   any match {
//      case data: Seq[(String, Any)] =>
//        //Seq - name-value pair where name can have duplicates
//        bindDataMapObj(data.groupMap(_._1)(_._2))
//      case data: Map[String, Any] =>
//        bindDataMapObj(data)
//      case value: Any =>
//        bindUntypedValue(value)
//    }
//
//  def bindJsValue(jsValue: JsValue): Field[A] =
//    val newValueOpt =  Field.toValueOf[A](tpe, jsValue.asOpt[Any])
//    bindTypedValueToField(newValueOpt)
//
//  private def bindDataMapObj(data:Map[String, Any]): Field[A] = {
//    if isSeqProduct then
//      bindSeqClass(data)
//    else if isProduct then
//      bindClass(data)
//    else
//      bindDataMapValue(data)
//  }
//
//  private def bindDataMapValue(data: Map[String, Any]): Field[A] = {
//    val value =  if isSeq
//    then
//    /*
//     * Attempt to get keys that matches f.name + '['
//     * if fail then use f.name as key to retrieve value from data
//     */
//      data.toList.filter((key: String, value: Any) => key.startsWith(s"${name}[")).map(_._2) match {
//        case Nil =>
//          data.getOrElse(name, None)
//        case values =>
//          //flatten the values
//          values.foldLeft(Seq.empty){(res, v) =>
//            v match {
//              case xs: Seq[_] => res ++ xs
//              case x => res :+ x
//            }
//          }
//      }
//    else data.getOrElse(name, None)
//
//    bindUntypedValue(value)
//  }
//
//  private def bindSeqClass(data: Map[String, Any]): Field[A] =
//    /*
//      * Filter all name-value list that matches 'field.name' + '.'
//      */
//    val keyMatchRegex = s"$name\\[\\d+]\\..+"
//    val keyReplaceRegex = s"$name\\[(\\d+)]"
//    //Group name-value pairs by index
//    val valueList: Seq[(Int, (String, Any))] = data.toList.collect{ case (key, x) if key.matches(keyMatchRegex) =>
//      key.replaceAll(keyReplaceRegex, "$1").split("\\.",2) match {
//        case Array(index, fieldKey) =>
//          index.toInt -> (fieldKey, x)
//      }
//    }
//    val nvPairsByIndex = valueList.groupMap(_._1)(_._2)
//    val sortedIndices = nvPairsByIndex.keys.toList.sorted
//    val value = sortedIndices.flatMap{index =>
//      val map = nvPairsByIndex(index).toMap
//
//      //Bind to product mappings
//      val bindedField = bindClass(map)
//      bindedField.value
//    }
//    copy(value = Some(value.asInstanceOf[A]))
//
//  private def bindClass(data: Map[String, Any]): Field[A] = {
//    /*
//     * Filter all name-value list that matches 'field.name' + '.'
//     */
//    val xs: Map[String, Any] =
//      if isProduct && name != null /*null name implies a child or a ProductClass */ then
//        data.collect { case (key, value) if key.startsWith(name + ".") =>
//          key.replace(s"${name}.", "") -> value
//        }
//      else data
//
//    val newMappings = bindDataToMappings(mappings, xs)
//    bindedFieldsToValue(newMappings, mirrorOpt,
//      (newData, newMappings, newValue, newErrors) =>
//        copy(mappings = newMappings, value = Option(newValue), errors = newErrors))
//  }
//
//  private def bindTypedValueToField(newValueOpt: Option[A]) : Field[A] =
//    newValueOpt match {
//      case Some(value: A) =>
//        val formErrors = applyConstraints(value)
//        copy(value = newValueOpt, errors = formErrors)
//
//      case None =>
//        copy(value = None)
//    }
//
//  private def bindUntypedValue(value: Any): Field[A] =
//    val newValueOpt = Field.toValueOf(tpe, value)
//    bindTypedValueToField(newValueOpt)
//
//  override def verifying(newConstraints: Constraint[A]*): Field[A] =
//    copy(constraints = constraints ++ newConstraints)
//
//  //If same type, retain all settings, if, if not same all constraints will be dropped
//  //Transform should start before the verifying
//  inline def transform[B](fn: A => B, fn2: B => A): Field[B] =
//    Field.of[B](name).copy(form = this.form)
//
//}
