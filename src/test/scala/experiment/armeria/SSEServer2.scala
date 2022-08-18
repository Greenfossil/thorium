package experiment.armeria

import com.linecorp.armeria.common.sse.ServerSentEvent
import com.linecorp.armeria.common.*
import com.linecorp.armeria.server.annotation.{Get, Param, ProducesEventStream}
import com.linecorp.armeria.server.streaming.ServerSentEvents
import com.linecorp.armeria.server.{Server, ServiceRequestContext}
import reactor.core.publisher.{Flux, FluxSink}
import reactor.core.scheduler.Schedulers

import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}

object SSEController2 {

  import java.time.LocalTime

  val timePub: Flux[ServerSentEvent] = Flux.create[ServerSentEvent](fluxSink => {
    var cnt = 0
    println(s"FluxSink - Thread name ${Thread.currentThread().getName}")
    while(true){
      cnt += 1
      val id = cnt % 3
      val time = LocalTime.now.toString
      println(s"id ${id}, cnt ${cnt} - ${time}")
      val sse = ServerSentEvent.builder().id(id.toString).event(time).build()
      fluxSink.next(sse)
      Thread.sleep(1000)
    }
  })
    .publish
    .autoConnect()

  var cnt = 0
  val timePub2: Flux[ServerSentEvent] = Flux.generate[ServerSentEvent](fluxSink => {
    println(s"FluxSink - Thread name ${Thread.currentThread().getName}")
    cnt += 1
    val id = cnt % 3
    val time = LocalTime.now.toString
    println(s"id ${id}, cnt ${cnt} - ${time}")
    val sse = ServerSentEvent.builder().id(id.toString).event(time).build()
    fluxSink.next(sse)
    Thread.sleep(5000)
  })
    .publish
    .autoConnect()

  //Start the first subscription on a separate thread
//  timePub.share().subscribeOn(Schedulers.single()).subscribe()

  @Get("/time/:deviceId")
  @ProducesEventStream
  def time(@Param deviceId: String) =
    val ctx = ServiceRequestContext.current()
    ctx.setRequestTimeout(java.time.Duration.ofSeconds(60))
    ServerSentEvents
      .fromPublisher(
        ResponseHeaders.of(200),
        timePub2
          .doOnSubscribe(s => println(s"device:${deviceId} subscribed. Thread ${Thread.currentThread().getName}"))
          .filter(_.id() == deviceId)
          .share())


}

@main def sseMain2 =
  Server.builder()
    .http(8080)
    .annotatedService(SSEController2)
    .build()
    .start()