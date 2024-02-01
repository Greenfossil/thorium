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

object RecaptchaFormPage:

  def apply(sitekey: String, endpoint: Endpoint)(using request: Request): Tag =
    html(
      head(
        script(src:="https://www.google.com/recaptcha/api.js" /*async defer ? how to add this to htmltag?*/),
        script(
          """function onSubmit(token){
            |  document.getElementById('demo-form').submit();
            |}""".stripMargin
        )
      ),
      body(
        ifTrue(request.flash.data.nonEmpty,
          div(style:="background:lightblue;height:40px;", "Recaptcha response:" + request.flash.data.values.mkString(","))
        ),
        hr,
        form(action := endpoint.url, method:="POST", id:="demo-form")(
          div(
            label("Blog"),
            input(tpe:="text", name:="blog", placeholder:="Write your blog")
          ),
          button(cls:="ui button g-recaptcha", cls:="g-recaptcha",  data.sitekey:=sitekey, data.callback:="onSubmit", data.action:="submit", "Submit"),
          button(cls:="ui button g-recaptcha", cls:="g-recaptcha",  data.sitekey:=sitekey, data.callback:="onSubmit", data.action:="cancel", "Cancel")
        )
      )
    )
