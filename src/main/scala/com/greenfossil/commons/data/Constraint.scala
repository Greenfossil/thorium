package com.greenfossil.commons.data

/**
 * A form constraint.
 *
 * @tparam T type of values handled by this constraint
 * @param name the constraint name, to be displayed to final user
 * @param args the message arguments, to format the constraint name
 * @param f the validation function
 */
case class Constraint[-T](name: Option[String], args: Seq[Any])(f: T => ValidationResult) {

  /**
   * Run the constraint validation.
   *
   * @param t the value to validate
   * @return the validation result
   */
  def apply(t: T): ValidationResult = f(t)
}

/**
 * This object provides helpers for creating `Constraint` values.
 *
 * For example:
 * {{{
 *   val negative = Constraint[Int] {
 *     case i if i < 0 => Valid
 *     case _ => Invalid("Must be a negative number.")
 *   }
 * }}}
 */
object Constraint {

  /**
   * Creates a new anonymous constraint from a validation function.
   *
   * @param f the validation function
   * @return a constraint
   */
  def apply[T](f: (T => ValidationResult)): Constraint[T] = apply(None, Nil)(f)

  /**
   * Creates a new named constraint from a validation function.
   *
   * @param name the constraint name
   * @param args the constraint arguments, used to format the constraint name
   * @param f the validation function
   * @return a constraint
   */
  def apply[T](name: String, args: Any*)(f: (T => ValidationResult)): Constraint[T] = apply(Some(name), args.toSeq)(f)

}
