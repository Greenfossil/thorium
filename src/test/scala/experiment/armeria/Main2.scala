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

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpHeaders, HttpRequest, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.{Server, ServiceRequestContext}
import com.linecorp.armeria.server.annotation.{Param, Post, ResponseConverterFunction}

trait Action:
  def apply(req: ActionRequest): String

object Action:
  def apply(fn: ActionRequest => String): Action = (req: ActionRequest) => fn(req)

case class ActionRequest(agg: AggregatedHttpRequest):
  def content: String = agg.contentUtf8()

val actionResponseConverter: ResponseConverterFunction = {
  (ctx: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders) =>
    result match
      case action: Action =>
        val httpResponse = ctx.request().aggregate().thenApply(agg => {
          val actionResultString = action.apply(ActionRequest(agg))
          HttpResponse.of(actionResultString)
        })
        HttpResponse.from(httpResponse)
      case _ => ResponseConverterFunction.fallthrough()
}

object TestService {

  @Post("/post1")
  def post1(@Param queryArg: String, @Param content: String) =
    HttpResponse.of(s"queryArg:$queryArg content:${content}")

  @Post("/post2")
  def post2(@Param queryArg: String, request: HttpRequest) =
    HttpResponse.from(request.aggregate().thenApply(aggregate => {
      HttpResponse.of(s"queryArg:$queryArg content:${aggregate.contentUtf8()}")
    }))

  @Post("/post3")
  def post3(@Param queryArg: String) =
    Action{implicit req: ActionRequest =>
      s"queryArg:$queryArg content:${req.content}"
    }

  @Post("/post4")
  def post4(@Param queryArg: String, ctx: ServiceRequestContext, request: AggregatedHttpRequest) =
    ctx.updateRequest(request.toHttpRequest())
    Action{implicit req: ActionRequest =>
      s"queryArg:$queryArg content:${req.content}"
    }

}

@main def main2 =  {
  import scala.jdk.CollectionConverters.*
  Server.builder().http(8080)
    .annotatedService(TestService)
    .annotatedServiceExtensions(Nil.asJava, List(actionResponseConverter).asJava, Nil.asJava)
    .build().start().join()
}

//curl -X POST http://localhost:8080/post1\?queryArg\=hello -d "content=world"
//curl -v -X POST http://localhost:8080/post2\?queryArg\=hello -d "name=world"
//curl -v -X POST http://localhost:8080/post3\?queryArg\=hello
// curl -v -X POST http://localhost:8080/post3\?queryArg\=hello -d "name=world"

//Query string cause an exception in Armeria, there is a fix in the future
//  @Post("/post3") //curl -v -X POST http://localhost:8080/post2\?queryArg\=hello -d "name=world"
//  def post3(@Param queryArg: String, request: HttpRequest) =
//    HttpResponse.from(ServiceRequestContext.current().request().aggregate().thenApply(aggregate => {
//      val content = aggregate.contentUtf8()
//      HttpResponse.of(s"queryArg:$queryArg content:${content}")
//    }))