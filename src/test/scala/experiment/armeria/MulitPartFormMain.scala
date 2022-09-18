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