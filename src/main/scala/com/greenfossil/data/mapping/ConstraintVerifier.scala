package com.greenfossil.data.mapping

import com.greenfossil.commons.json.JsValue

/**
 *
 * @tparam A - is the param type of Mapping
 */
trait ConstraintVerifier[A] {

  val name: String

  val constraints: Seq[Constraint[A]]

  def verifying(newConstraints: Constraint[A]*): Mapping[A]

  def verifying(constraint: A => Boolean): Mapping[A] =
    verifying("error.unknown", constraint)

  /**
   *
   * @param error
   * @param successConstraintPredicate -  true implies no error, false implies error
   * @return
   */
  def verifying(error: String, successConstraintPredicate: A => Boolean): Mapping[A] =
    verifying(Constraint{ (a: A) =>
      if successConstraintPredicate(a) then Valid else Invalid(Seq(ValidationError(error)))
    })

  def applyConstraints(value: A): Seq[MappingError] = {
    constraints
      .map(_.apply(value))
      .collect{case Invalid(ve) => ve}
      .flatten
      .map(ve => MappingError(name, ve.messages, ve.args))
  }

  def boundFieldsToProduct(
                            newMappings: Mapping[?] *: Tuple,
                            mirrorOpt: Option[scala.deriving.Mirror.ProductOf[A]],
                            fn: (Map[String, Any],  Mapping[?] *: Tuple, A, Seq[MappingError]) => Mapping[A]
                         ): Mapping[A] =

    val newData: Map[String, Any] = newMappings.toList.collect{ case f: Mapping[?] => f.name -> f.safeValue }.toMap

    val fieldsErrors: List[MappingError] =  newMappings.toList.collect{ case f: Mapping[t] => f.errors }.flatten

    val boundFieldValues: Any *: Tuple = newMappings.map[[A] =>> Any]{
      [X] => (x: X) => x match
        case f: Mapping[t] => f.safeValue
    }

    val boundValue: A =
    // This is to handle Form with single field to return the actual type of the field [T]
      if newMappings.size == 1
      then
        boundFieldValues(0).asInstanceOf[A]
      else
        //If all values are None, implies value is null
        if boundFieldValues.toList.collect{case None => 1}.sum == newMappings.size
        then null.asInstanceOf[A]
        else mirrorOpt.map(m => m.fromProduct(boundFieldValues)).getOrElse(boundFieldValues.asInstanceOf[A])

    val formConstraintsErrors = applyConstraints(boundValue)

    fn(newData, newMappings, boundValue, formConstraintsErrors ++ fieldsErrors)
  
}
