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

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

/**
 * A `ScheduledExecutorService` backed by virtual threads (Java 21+).
 *
 * - `execute()` and `submit()` run each task on its own virtual thread via
 *   `Executors.newThreadPerTaskExecutor(Thread.ofVirtual())`. Blocking I/O
 *   (JDBC, HTTP, `Await.result`) inside a task frees the carrier platform
 *   thread — eliminating the 200-thread cap of the default blocking pool.
 * - `schedule*()` methods use a small single-thread platform scheduler for
 *   the delay, then dispatch the actual task to a virtual thread. This is
 *   rarely used by Thorium (the blocking pool is primarily for `execute()`),
 *   so a single scheduler thread is sufficient.
 *
 * This class is only instantiated on Java 21+ (gated by `Runtime.version()`
 * check in `Server.buildServer`). On older JVMs, Armeria's default
 * `blockingTaskExecutor` (platform threads) is used.
 */
final class VirtualThreadBlockingExecutor(threadNamePrefix: String) extends ScheduledExecutorService:

  private val counter = AtomicLong(0)

  private val vtExecutor: ExecutorService =
    val factory = Thread.ofVirtual().factory()
    Executors.newThreadPerTaskExecutor(factory)

  // Single-thread scheduler for delayed/recurring tasks. The delay runs on
  // a platform thread; the actual task is dispatched to a virtual thread.
  private val scheduler = Executors.newSingleThreadScheduledExecutor(
    Thread.ofPlatform().name(s"$threadNamePrefix-scheduler").daemon(true).factory()
  )

  // --- ExecutorService ---

  def execute(command: Runnable): Unit = vtExecutor.execute(command)

  def submit(task: Runnable): java.util.concurrent.Future[?] = vtExecutor.submit(task)

  def submit[T](task: Callable[T]): java.util.concurrent.Future[T] = vtExecutor.submit(task)

  def submit[T](task: Runnable, result: T): java.util.concurrent.Future[T] = vtExecutor.submit(task, result)

  def invokeAll[T](tasks: java.util.Collection[? <: Callable[T]]): java.util.List[java.util.concurrent.Future[T]] =
    vtExecutor.invokeAll(tasks)

  def invokeAll[T](tasks: java.util.Collection[? <: Callable[T]], timeout: Long, unit: TimeUnit): java.util.List[java.util.concurrent.Future[T]] =
    vtExecutor.invokeAll(tasks, timeout, unit)

  def invokeAny[T](tasks: java.util.Collection[? <: Callable[T]]): T = vtExecutor.invokeAny(tasks)

  def invokeAny[T](tasks: java.util.Collection[? <: Callable[T]], timeout: Long, unit: TimeUnit): T =
    vtExecutor.invokeAny(tasks, timeout, unit)

  def shutdown(): Unit =
    vtExecutor.shutdown()
    scheduler.shutdown()

  def shutdownNow(): java.util.List[Runnable] =
    val remaining = vtExecutor.shutdownNow()
    remaining.addAll(scheduler.shutdownNow())
    remaining

  def isShutdown: Boolean = vtExecutor.isShutdown && scheduler.isShutdown

  def isTerminated: Boolean = vtExecutor.isTerminated && scheduler.isTerminated

  def awaitTermination(timeout: Long, unit: TimeUnit): Boolean =
    vtExecutor.awaitTermination(timeout, unit) && scheduler.awaitTermination(0, TimeUnit.MILLISECONDS)

  // --- ScheduledExecutorService ---

  def schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture[?] =
    val task: Runnable = () => vtExecutor.execute(command)
    scheduler.schedule(task, delay, unit)

  def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit): ScheduledFuture[V] =
    val task: Callable[V] = () => vtExecutor.submit(callable).get()
    scheduler.schedule(task, delay, unit)

  def scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture[?] =
    scheduler.scheduleAtFixedRate(() => vtExecutor.execute(command), initialDelay, period, unit)

  def scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture[?] =
    scheduler.scheduleWithFixedDelay(() => vtExecutor.execute(command), initialDelay, delay, unit)