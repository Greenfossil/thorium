package com.greenfossil.webserver.data

trait ConstraintVerifier[T[_], V](name: String, constraints: Seq[Constraint[V]]) {

  def verifying(newConstraints: Constraint[V]*): T[V]
  
  def verifying(constraint: V => Boolean): T[V] =
    verifying("error.unknown", constraint)

  /**
   *
   * @param error
   * @param constraintPredicate -  true implies no error, false implies error
   * @return
   */
  def verifying(error: String, constraintPredicate: V => Boolean): T[V] =
    verifying(Constraint{ (a: V) =>
        if constraintPredicate(a) then Valid else Invalid(Seq(ValidationError(error)))
    })
  
  def applyConstraints(value: V): Seq[FormError] = {
    constraints
      .map(_.apply(value))
      .collect{case Invalid(ve) => ve}
      .flatten
      .map(ve => FormError(name, ve.messages, ve.args))
  }

}
