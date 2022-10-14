package com.greenfossil.webserver

import java.util.concurrent.atomic.AtomicLong


/** A UsefulException is something useful to display in the User browser. */
trait UsefulException extends RuntimeException:
  /** Exception title. */
  val title: String

  /** Exception description. */
  val description: String

  /** Exception cause if defined. */
  val cause: Throwable

  /** Unique id for this exception. */
  val id: String

  override def toString: String = "@" + id + ": " + getMessage

  private val generator = new AtomicLong(System.currentTimeMillis())
  
  def nextId = java.lang.Long.toString(generator.incrementAndGet(), 26)
  

trait AppException extends UsefulException

trait ExceptionSource extends AppException