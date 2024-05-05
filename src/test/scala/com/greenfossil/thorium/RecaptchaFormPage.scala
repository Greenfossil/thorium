/*
 *  Copyright 2022 Greenfossil Pte Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.greenfossil.thorium

import com.greenfossil.htmltags.*

import scala.language.implicitConversions

object RecaptchaFormPage:

  def apply(endpoint: Endpoint, isMultipart: Boolean, useRecaptcha: Boolean = false)(using request: Request): Tag =
    val siteKey = request.httpConfiguration.recaptchaConfig.siteKey
    html(
        head(
          ifTrue(useRecaptcha, script(src:="https://www.google.com/recaptcha/api.js" /*async defer ? how to add this to htmltag?*/)),
          ifTrue(useRecaptcha, script(
            s"""function onSubmit(token){
              |  document.getElementById('demo-form').submit();
              |}""".stripMargin
          ))
        ),
        body(
          ifTrue(request.flash.data.nonEmpty,
            div(style:="background:lightblue;height:40px;", "Recaptcha response:" + request.flash.data.values.mkString(","))
          ),
          hr,
          form(action := endpoint.url, method:="POST", id:="demo-form", ifTrue(isMultipart, enctype := "multipart/form-data"))(
            div(
              label("Blog"),
              input(tpe:="text", name:="blog", placeholder:="Write your blog")
            ),
            button(cls:="ui button g-recaptcha", cls:="g-recaptcha", tpe:="submit", name:="action", value:="submit", data.sitekey:=siteKey, data.callback:="onSubmit", data.action:="submit", "Submit"),
            button(cls:="ui button g-recaptcha", cls:="g-recaptcha", tpe:="submit", name:="action", value:="cancel", data.sitekey:=siteKey, data.callback:="onSubmit", data.action:="cancel", "Cancel")
          )
        )
      )
