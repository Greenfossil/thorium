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

package experiment.armeria

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.annotation.*
import com.linecorp.armeria.common.multipart.MultipartFile

import java.io.File
import java.nio.file.Path

@main def formMain =
  val sb = Server.builder();
  // Configure an HTTP port.
  sb.http(8080)
  sb.annotatedService(new Object(){
    @Post("/upload")
    def upload(@Param param: String, @Param file: File, @Param path: Path, @Param multiPartFile: MultipartFile) = {
      HttpResponse.of(s"File uploaded ${file.getName}")
    }
  })
  val server = sb.build()
  val future = server.start()
  future.join()

  //curl http://localhost:8080/upload -H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryaDaB4MtEkj4a1pYx'  --data-raw $'------WebKitFormBoundaryaDaB4MtEkj4a1pYx--'