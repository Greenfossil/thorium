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

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.common.AggregatedHttpRequest
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.{Get, Param, RequestConverter, RequestConverterFunction}

import java.lang.reflect.ParameterizedType

/*
 * Please ensure com.greenfossil.webserver.examples.main is started
 */
object SimpleServices extends Controller {

  //curl http://localhost:8080/simple-text/homer%20simpson/2022-12-31
  @Get("/simple-text/{name}/:date")
  def simpleText(@Param name: String)(@Param date: java.time.LocalDate)(using request: Request) =
    s"HelloWorld ${name}! $date path:${request.requestContext.path()}"


 //curl -v  --get --data-urlencode "date=2022-12-31T10:10:05" --data-urlencode "msg=how are you?"  http://localhost:8080/simple-query/homer
 //curl -v  --get --data-urlencode "date=2022-12-31T10:10:05" --data "msg=how+are+you?"  http://localhost:8080/simple-query/homer
  @Get("/simple-query/:name")
  def simpleQuery(@Param name: String, @Param msg: String, @Param date: java.time.LocalDateTime)(using request: Request) =
    s"HelloWorld - Name:${name}! Date:$date Msg:$msg Path:${request.requestContext.path()}"


 //Note: Binding to Scala Collection will not work, Binding with `[]` e.g. name[] = will not work - use data-mapping to do binding.
 //curl -v  --get --data "names[]=Homer+Simpson" --data "names[]=Marge+Simpson" http://localhost:8080/simple-list - this will not work
 //curl -v  --get --data "names=Homer" --data "names=Marge" http://localhost:8080/simple-list
 //curl -v  --get --data-urlencode "names=Homer Simpson" --data-urlencode "names=Marge Simpson" http://localhost:8080/simple-list
 //curl -v  --get --data "names=Homer+Simpson" --data "names=Marge+Simpson" http://localhost:8080/simple-list
  @Get("/simple-list")
  def simpleList(@Param("names") names: java.util.List[String])(using request: Request) =
    import scala.jdk.CollectionConverters.*
    s"""Howdy! names:${names.asScala.mkString("[", ",", "]")}"""
}

class SeqConverter extends RequestConverterFunction {
  override def convertRequest(ctx: ServiceRequestContext,
                              request: AggregatedHttpRequest,
                              expectedResultType: Class[?],
                              expectedParameterizedResultType: ParameterizedType): AnyRef =
    if(expectedResultType == classOf[Seq[Any]]) {
      println(s"expectedResultType = ${expectedResultType}")
      println(s"expectedParameterizedResultType = ${expectedParameterizedResultType}")
      Seq.empty
    }else RequestConverterFunction.fallthrough()

}
