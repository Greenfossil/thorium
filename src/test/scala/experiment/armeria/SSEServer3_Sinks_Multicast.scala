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

import com.linecorp.armeria.common.*
import com.linecorp.armeria.common.sse.ServerSentEvent
import com.linecorp.armeria.server.annotation.{Get, Param, ProducesEventStream}
import com.linecorp.armeria.server.streaming.ServerSentEvents
import com.linecorp.armeria.server.{Server, ServiceRequestContext}
import reactor.core.publisher.Sinks.EmitFailureHandler
import reactor.core.publisher.{Flux, FluxSink, SignalType, Sinks}
import reactor.core.scheduler.Schedulers

import java.time.LocalTime
import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}


object SSEController3 {

  val sink = Sinks.many().multicast().onBackpressureBuffer[ServerSentEvent]()
  val flux = sink.asFlux()

  CompletableFuture.runAsync(() => {
    var cnt = 0
    println(s"FluxSink - Thread name ${Thread.currentThread().getName}")
    while (true) {
      cnt += 1
      val id = cnt % 3
      val time = LocalTime.now.toString
      println(s"id ${id}, cnt ${cnt} - ${time} -  Thread ${Thread.currentThread().getName}")
      val sse = ServerSentEvent.builder().id(id.toString).event(time).build()
      sink.emitNext(sse, (signalType: SignalType, emitResult: Sinks.EmitResult) => {
        println(s"Emit Next Failure ${signalType} emitResult ${emitResult}  Thread ${Thread.currentThread().getName}")
        true
      })
      Thread.sleep(1000)
    }
  })

  @Get("/time/:deviceId")
  def time(@Param deviceId: String) =
    ServiceRequestContext.current().setRequestTimeout(java.time.Duration.ofSeconds(60))
    ServerSentEvents
      .fromPublisher(
        ResponseHeaders.of(200),
        flux
          .doOnSubscribe(s => println(s"device:${deviceId} subscribed. Thread ${Thread.currentThread().getName}"))
          .filter(_.id() == deviceId)
      )
}

@main def sseMain3 = {
  Server.builder()
    .http(8080)
    .annotatedService(SSEController3)
    .build()
    .start()
  Thread.sleep(2000)
}
