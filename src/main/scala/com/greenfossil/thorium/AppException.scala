/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium


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

  private val generator = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
  
  def nextId = java.lang.Long.toString(generator.incrementAndGet(), 26)
  

trait AppException extends UsefulException

trait ExceptionSource extends AppException