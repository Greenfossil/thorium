package com.greenfossil.commons.data

import com.greenfossil.commons.json.JsValue

/**
 *
 * @tparam T - is either a Form[V] or Field[V]
 * @tparam V - is the param type of T
 */
trait ConstraintVerifier[T[_], V] {

  val name: String 
  
  val constraints: Seq[Constraint[V]]

  def verifying(newConstraints: Constraint[V]*): T[V]

  def verifying(constraint: V => Boolean): T[V] =
    verifying("error.unknown", constraint)

  /**
   *
   * @param error
   * @param successConstraintPredicate -  true implies no error, false implies error
   * @return
   */
  def verifying(error: String, successConstraintPredicate: V => Boolean): T[V] =
    verifying(Constraint{ (a: V) =>
      if successConstraintPredicate(a) then Valid else Invalid(Seq(ValidationError(error)))
    })

  def applyConstraints(value: V): Seq[FormError] = {
    constraints
      .map(_.apply(value))
      .collect{case Invalid(ve) => ve}
      .flatten
      .map(ve => FormError(name, ve.messages, ve.args))
  }

  def boundFieldsToProduct(
                           newMappings: Field[?] *: Tuple,
                           mirrorOpt: Option[scala.deriving.Mirror.ProductOf[V]],
                           fn: (Map[String, Any],  Field[?] *: Tuple, V, Seq[FormError]) => T[V]
                         ): T[V] =

    val newData: Map[String, Any] = newMappings.toList.collect{ case f: Field[?] => f.name -> f.safeValue }.toMap

    val fieldsErrors: List[FormError] =  newMappings.toList.collect{ case f: Field[t] => f.errors }.flatten

    val boundFieldValues: Any *: Tuple = newMappings.map[[A] =>> Any]{
      [X] => (x: X) => x match
        case f: Field[t] => f.safeValue
    }

    val boundValue: V =
    // This is to handle Form with single field to return the actual type of the field [T]
      if newMappings.size == 1
      then
        boundFieldValues(0).asInstanceOf[V]
      else
        //If all values are None, implies value is null
        if boundFieldValues.toList.collect{case None => 1}.sum == newMappings.size
        then null.asInstanceOf[V]
        else mirrorOpt.map(m => m.fromProduct(boundFieldValues)).getOrElse(boundFieldValues.asInstanceOf[V])

    val formConstraintsErrors = applyConstraints(boundValue)

    fn(newData, newMappings, boundValue, formConstraintsErrors ++ fieldsErrors)


  /*
 * TODO - query string params is not implemented yet
 */
  def bindJsValueToMappings(mappings: Field[?] *: Tuple, js: JsValue, query: List[(String, String)]): Field[?] *: Tuple =
    mappings.map[[A] =>> Field[?]] {
      [X] => (x: X) => x match
        case f: Field[t] => f.bind(js)
    }

}
