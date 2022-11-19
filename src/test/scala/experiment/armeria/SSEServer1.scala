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

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.sse.ServerSentEvent
import com.linecorp.armeria.server.{Server, ServiceRequestContext}
import com.linecorp.armeria.server.annotation.{Get, Param, ProducesEventStream}
import com.linecorp.armeria.server.streaming.ServerSentEvents
import org.reactivestreams.{Subscriber, Subscription}
import reactor.core.Disposable
import reactor.core.publisher.{ConnectableFlux, Flux, Mono}
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

import java.time.Duration

/**
 * https://www.baeldung.com/reactor-core
 */
object SSEController1 {

  import java.time.LocalTime

  val timePub: Flux[ServerSentEvent] = Flux.create[ServerSentEvent](fluxSink => {
    var cnt = 0
    while(true){
      cnt += 1
      val id = cnt % 3
      println(s"id ${id}, cnt ${cnt} ")
      val sse = ServerSentEvent.builder().id(id.toString).event(LocalTime.now.toString).build()
      fluxSink.next(sse)
      Thread.sleep(2000)
    }
  }).publish.autoConnect()

  @Get("/time/:deviceId")
  @ProducesEventStream
  def time(@Param deviceId: String) =
    println(s"deviceId = ${deviceId} ${Thread.currentThread().getName}")
    timePub.subscribe(sse => {
      println(s"Emitting event for ${deviceId} - ${Thread.currentThread().getName} - sse ${sse}")
      Flux.just(sse)
    })
    println(s"respond first event ${deviceId}- ${Thread.currentThread().getName}")
    Flux.just(ServerSentEvent.ofData(s"Start of event ${deviceId}"))



  @Get("/sse")
  @ProducesEventStream
  def sse(ctx: ServiceRequestContext, req: HttpRequest)/*: Publisher[ServerSentEvent]*/ =
    ctx.setRequestTimeout(Duration.ZERO)
    Flux.just("foo", "bar", ServerSentEvent.ofData)
}

/**
 * curl --http2 -N -v http://localhost:8080/sse
 * curl --http2 -N -v http://localhost:8080/sse1
 * curl --http2 -N -v http://localhost:8080/sse2
 * @return
 */
@main def sseMain1 =
  val sseServer = Server.builder()
    .http(8080)
    // Emit Server-Sent Events with the SeverSentEvent instances published by a publisher.
    .service("/sse1", (ctx, req) => ServerSentEvents.fromPublisher(Flux.just(ServerSentEvent.ofData("foo"), ServerSentEvent.ofData("bar"))))
    // Emit Server-Sent Events with converting instances published by a publisher into
    // ServerSentEvent instances.
    .service("/sse2", (ctx, req) => ServerSentEvents.fromPublisher(Flux.just("foo","bar"), ServerSentEvent.ofData))
    .annotatedService(SSEController1)
    .build()

  println("SSE Server started")
  sseServer.start()