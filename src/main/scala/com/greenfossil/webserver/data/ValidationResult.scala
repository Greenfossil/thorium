//package com.greenfossil.commons.data
//
///**
// * A validation result.
// */
//sealed trait ValidationResult
//
///**
// * Validation was a success.
// */
//case object Valid extends ValidationResult
//
///**
// * Validation was a failure.
// *
// * @param errors the resulting errors
// */
//case class Invalid(errors: Seq[ValidationError]) extends ValidationResult {
//
//  /**
//   * Combines these validation errors with another validation failure.
//   *
//   * @param other validation failure
//   * @return a new merged `Invalid`
//   */
//  def ++(other: Invalid): Invalid = Invalid(this.errors ++ other.errors)
//}
//
///**
// * This object provides helper methods to construct `Invalid` values.
// */
//object Invalid {
//
//  /**
//   * Creates an `Invalid` value with a single error.
//   *
//   * @param error the validation error
//   * @return an `Invalid` value
//   */
//  def apply(error: ValidationError): Invalid = Invalid(Seq(error))
//
//  /**
//   * Creates an `Invalid` value with a single error.
//   *
//   * @param error the validation error message
//   * @param args the validation error message arguments
//   * @return an `Invalid` value
//   */
//  def apply(error: String, args: Any*): Invalid = Invalid(Seq(ValidationError(error, args: _*)))
//}
//
///**
// * A validation error.
// *
// * @param messages the error message, if more then one message is passed it will use the last one
// * @param args the error message arguments
// */
//case class ValidationError(messages: Seq[String], args: Any*) {
//
//  lazy val message = messages.last
//
//}
//
//object ValidationError {
//
//  //  /**
//  //   * Conversion from a JsonValidationError to a Play ValidationError.
//  //   */
//  //  def fromJsonValidationError(jve: "JsonValidationError"): ValidationError = {
//  ////    ValidationError(jve.message, jve.args: _*)
//  //    ???
//  //  }
//
//  def apply(message: String, args: Any*) = new ValidationError(Seq(message), args: _*)
//
//}
