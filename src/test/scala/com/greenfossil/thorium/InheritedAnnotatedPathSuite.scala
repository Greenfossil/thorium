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

import com.greenfossil.thorium.Sub2.foo
import com.linecorp.armeria.server.annotation.Get

trait Base:
  @Get("/base/foo")
  def foo = Action: _ =>
    "hello foo"

  @Get("/base/bar")
  def bar = Action: _ =>
    Redirect(foo)

object Sub1 extends Base:

  @Get("/sub1/foo")
  override def foo = Action: _ =>
    "hello sub1 foo"

  @Get("/sub1/foobaz")
  def foobaz = Action: _ =>
    Redirect(foo)

  def foobazRedirect =  EndpointMcr(foo).url

object Sub2 extends Base:

  @Get("/sub2/foobaz")
  def foobaz = Action {request =>
    Redirect(foo)
  }

  def foobazRedirect = EndpointMcr(foo).url



class InheritedAnnotatedPathSuite extends munit.FunSuite:

  test("inherited Impl1.foo"){
    val ep = Sub1.foo.endpoint
    assertNoDiff(ep.url, "/sub1/foo")
  }

  test("inherited Impl1.toFoo"){
    val redirect1 = Sub1.foobazRedirect
    assertNoDiff(redirect1, "/sub1/foo")


    val redirect2 = Sub2.foobazRedirect
    assertNoDiff(redirect2, "/base/foo")
  }


