//package com.greenfossil.commons.data
//
//
///**
// * A form error.
// *
// * @param key The error key (should be associated with a field using the same key).
// * @param messages The form message (often a simple message key needing to be translated), if more than one message
// *                 is passed the last one will be used.
// * @param args Arguments used to format the message.
// */
//case class FormError(key: String, messages: Seq[String], args: Seq[Any] = Nil) {
//
//  def this(key: String, message: String) = this(key, Seq(message), Nil)
//
//  def this(key: String, message: String, args: Seq[Any]) = this(key, Seq(message), args)
//
//  lazy val message = messages.last
//
//  /**
//   * Copy this error with a new Message.
//   *
//   * @param message The new message.
//   */
//  def withMessage(message: String): FormError = FormError(key, message)
//
//  /**
//   * Displays the formatted message, for use in a template.
//   * FIXME - use locale
//   */
////  def format(implicit messages: play.api.i18n.Messages): String = {
////    messages.apply(message, args: _*)
////  }
//}
//
//object FormError {
//
//  def apply(key: String, message: String) = new FormError(key, message)
//
//  def apply(key: String, message: String, args: Seq[Any]) = new FormError(key, message, args)
//
//}