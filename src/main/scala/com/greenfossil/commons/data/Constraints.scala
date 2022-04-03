package com.greenfossil.commons.data

/**
 * Defines a set of built-in constraints.
 */
object Constraints extends Constraints

/**
 * Defines a set of built-in constraints.
 *
 * @define emailAddressDoc Defines an ‘emailAddress’ constraint for `String` values which will validate email addresses.
 *
 * '''name'''[constraint.email]
 * '''error'''[error.email]
 *
 * @define nonEmptyDoc Defines a ‘required’ constraint for `String` values, i.e. one in which empty strings are invalid.
 *
 * '''name'''[constraint.required]
 * '''error'''[error.required]
 */
trait Constraints {

  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  /**
   * $emailAddressDoc
   */
  def emailAddress(errorMessage: String = "error.email"): Constraint[String] = Constraint[String]("constraint.email") {
    e =>
      if (e == null) Invalid(ValidationError(errorMessage))
      else if (e.trim.isEmpty) Invalid(ValidationError(errorMessage))
      else
        emailRegex
          .findFirstMatchIn(e)
          .map(_ => Valid)
          .getOrElse(Invalid(ValidationError(errorMessage)))
  }

  /**
   * $emailAddressDoc
   *
   */
  def emailAddress: Constraint[String] = emailAddress()

  /**
   * $nonEmptyDoc
   */
  def nonEmpty(errorMessage: String = "error.required"): Constraint[String] =
    Constraint[String]("constraint.required") { o =>
      if (o == null) Invalid(ValidationError(errorMessage))
      else if (o.trim.isEmpty) Invalid(ValidationError(errorMessage))
      else Valid
    }

  /**
   * $nonEmptyDoc
   *
   */
  def nonEmpty: Constraint[String] = nonEmpty()

  /**
   * Defines a minimum value for `Ordered` values, by default the value must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.min(minValue)]
   * '''error'''[error.min(minValue)] or [error.min.strict(minValue)]
   */
  def min[T](
              minValue: T,
              strict: Boolean = false,
              errorMessage: String = "error.min",
              strictErrorMessage: String = "error.min.strict"
            )(implicit ordering: scala.math.Ordering[T]): Constraint[T] = Constraint[T]("constraint.min", minValue) { o =>
    (ordering.compare(o, minValue).signum, strict) match {
      case (1, _) | (0, false) => Valid
      case (_, false)          => Invalid(ValidationError(errorMessage, minValue))
      case (_, true)           => Invalid(ValidationError(strictErrorMessage, minValue))
    }
  }

  /**
   * Defines a maximum value for `Ordered` values, by default the value must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.max(maxValue)]
   * '''error'''[error.max(maxValue)] or [error.max.strict(maxValue)]
   */
  def max[T](
              maxValue: T,
              strict: Boolean = false,
              errorMessage: String = "error.max",
              strictErrorMessage: String = "error.max.strict"
            )(implicit ordering: scala.math.Ordering[T]): Constraint[T] = Constraint[T]("constraint.max", maxValue) { o =>
    (ordering.compare(o, maxValue).signum, strict) match {
      case (-1, _) | (0, false) => Valid
      case (_, false)           => Invalid(ValidationError(errorMessage, maxValue))
      case (_, true)            => Invalid(ValidationError(strictErrorMessage, maxValue))
    }
  }

  /**
   * Defines a minimum length constraint for `String` values, i.e. the string’s length must be greater than or equal to the constraint parameter
   *
   * '''name'''[constraint.minLength(length)]
   * '''error'''[error.minLength(length)]
   */
  def minLength(length: Int, errorMessage: String = "error.minLength"): Constraint[String] =
    Constraint[String]("constraint.minLength", length) { o =>
      require(length >= 0, "string minLength must not be negative")
      if (o == null) Invalid(ValidationError(errorMessage, length))
      else if (o.size >= length) Valid
      else Invalid(ValidationError(errorMessage, length))
    }

  /**
   * Defines a maximum length constraint for `String` values, i.e. the string’s length must be less than or equal to the constraint parameter
   *
   * '''name'''[constraint.maxLength(length)]
   * '''error'''[error.maxLength(length)]
   */
  def maxLength(length: Int, errorMessage: String = "error.maxLength"): Constraint[String] =
    Constraint[String]("constraint.maxLength", length) { o =>
      require(length >= 0, "string maxLength must not be negative")
      if (o == null) Invalid(ValidationError(errorMessage, length))
      else if (o.size <= length) Valid
      else Invalid(ValidationError(errorMessage, length))
    }

  /**
   * Defines a regular expression constraint for `String` values, i.e. the string must match the regular expression pattern
   *
   * '''name'''[constraint.pattern(regex)] or defined by the name parameter.
   * '''error'''[error.pattern(regex)] or defined by the error parameter.
   */
  def pattern(
               regex: => scala.util.matching.Regex,
               name: String = "constraint.pattern",
               error: String = "error.pattern"
             ): Constraint[String] = Constraint[String](name, () => regex) { o =>
    require(regex != null, "regex must not be null")
    require(name != null, "name must not be null")
    require(error != null, "error must not be null")

    if (o == null) Invalid(ValidationError(error, regex))
    else regex.unapplySeq(o).map(_ => Valid).getOrElse(Invalid(ValidationError(error, regex)))
  }


  /**
   * Checks the precision and scale of the given BigDecimal
   * https://stackoverflow.com/questions/35435691/bigdecimal-precision-and-scale
   */
  def precision(
                 precision: Int,
                 scale: Int,
                 name: String = "constraint.precision",
                 error: String = "error.real.precision",
               ) :Constraint[BigDecimal] = Constraint[BigDecimal](name, (precision, scale)){ bd =>
    if (bd.precision - bd.scale > precision - scale) {
      Invalid(ValidationError(error, precision, scale))
    }else{
      Valid
    }
  }

}
