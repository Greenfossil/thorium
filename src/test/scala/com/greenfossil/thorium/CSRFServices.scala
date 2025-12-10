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

import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.*
import com.linecorp.armeria.server.annotation.decorator.{CorsDecorator, RequestTimeout}

import java.util.concurrent.TimeUnit

/*
 * Please ensure com.greenfossil.webserver.examples.main is started
 */
object CSRFServices:

  @Get("/csrf/do-change-email")
  @RequestTimeout(value = 1, unit = TimeUnit.HOURS)
  def startChange = Action { implicit request =>
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |    <meta charset="UTF-8">
       |    <title>Change Email</title>
       |    <link rel="shortcut icon" href="data:;" type="image/x-icon">
       |</head>
       |<body>
       |    <h1>Change Email</h1>
       |    <form action="http://localhost:8080/csrf/email/change" method="POST">
       |        <input type="hidden" name="CSRF-TOKEN" value="${request.generateCSRFToken}" />
       |        <input name="email" value="HelloWorld!" />
       |        <input type="submit" value="Confirm">
       |    </form>
       |</body>
       |</html>
       |""".stripMargin.as(MediaType.HTML_UTF_8)
  }


  @Post("/csrf/email/change")
  @RequestTimeout(value = 1, unit = TimeUnit.HOURS)
  def changePassword = Action : _ =>
        "Password Changed"

  @Get("/csrf/do-delete")
  @RequestTimeout(value  = 1, unit = TimeUnit.HOURS)
  def startDelete = Action { implicit request =>
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |    <meta charset="UTF-8">
       |    <title>SameOrigin</title>
       |    <link rel="shortcut icon" href="data:;" type="image/x-icon">
       |</head>
       |<body>
       |   <h1>Delete Action</h1>
       |   <input type="submit" value="Confirm" onclick="doDelete()">
       |
       |</body>
       |<script>
       |  function doDelete(){
       |    const url = 'http://localhost:8080/csrf/delete/123';
       |    fetch(url, {
       |      method: 'DELETE',
       |    }).then(()=> {
       |      console.log('delete');
       |    }).catch(err => {
       |      console.error(err);
       |    });
       |  }
       |</script>
       |</html>
       |""".stripMargin.as(MediaType.HTML_UTF_8)
  }

  @Options
  @Delete("/csrf/delete/:id")
  @CorsDecorator(origins = Array("*"))
  @RequestTimeout(value = 1, unit = TimeUnit.HOURS)
  def deleteResource(@Param id: String) = Action { request =>
    s"Deleted $id"
  }

  @Post("/csrf/multipart-file")
  def multipartFile: Action = Action.multipart { implicit request =>
    val form = request.asFormUrlEncoded
    request.findFiles((_, _, _, _) => true)
      .map { files =>
        Ok(s"Received multipart request with files: ${files.size}, form:$form")
      }.getOrElse(BadRequest("No file uploaded"))
  }

