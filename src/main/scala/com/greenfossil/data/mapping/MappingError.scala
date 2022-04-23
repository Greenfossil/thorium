package com.greenfossil.data.mapping

/**
 * A form error.
 *
 * @param key The error key (should be associated with a field using the same key).
 * @param messages The form message (often a simple message key needing to be translated), if more than one message
 *                 is passed the last one will be used.
 * @param args Arguments used to format the message.
 */
case class MappingError(key: String, messages: Seq[String], args: Seq[Any] = Nil) {

  def this(key: String, message: String) = this(key, Seq(message), Nil)

  def this(key: String, message: String, args: Seq[Any]) = this(key, Seq(message), args)

  lazy val message: String = messages.last
  
  def is(key:String, message: String): Boolean = 
    this.key == key && this.message == message

  /**
   * Copy this error with a new Message.
   *
   * @param message The new message.
   */
  def withMessage(message: String): MappingError = MappingError(key, message)
  
}

object MappingError {

  def apply(key: String, message: String) = new MappingError(key, message)

  def apply(key: String, message: String, args: Seq[Any]) = new MappingError(key, message, args)

}