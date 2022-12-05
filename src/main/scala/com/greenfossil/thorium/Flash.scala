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

import scala.annotation.targetName

object Flash:
  def apply(): Flash = new Flash(Map.empty.withDefaultValue(""))

case class Flash(data: Map[String, String]):
  export data.{+ as _, *}
  
  @targetName("add")
  def +(tup: (String, String)): Flash =
    copy(data =  data + tup )