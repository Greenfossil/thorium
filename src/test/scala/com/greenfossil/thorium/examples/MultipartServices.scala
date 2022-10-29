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

package com.greenfossil.thorium.examples

import com.greenfossil.data.mapping.*
import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param, Post}

object MultipartServices {

  /*
   * Method Multipart, both data and query arg does not work from Armeria 1.19.0 and before
   */
  @Post("/mp-form") //curl -v -F name=homer http://localhost:8080/mp-form\?queryArg\=12345
  def multipartForm(@Param queryArg: Int) =
    Action.multipart{request =>
      s"queryArg:[${queryArg}] req:[${request.asFormUrlEncoded}] uri:[${request.uri}]"
    }

  /*
   * Method Post, both data and query arg will work only from Armeria 1.19.0 onwards
   */
  @Post("/urlencoded-form") //curl -v -d name=homer http://localhost:8080/urlencoded-form\?queryArg\=12345
  def urlencodedForm(@Param queryArg: Int) =
    Action {request =>
      s"queryArg:[${queryArg}] req:[${request.asFormUrlEncoded}] uri:[${request.uri}]"
    }
  
}
