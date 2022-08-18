package experiment.reactive

import org.reactivestreams.{Subscriber, Subscription}
import reactor.core.Disposable
import reactor.core.publisher.*
import reactor.core.scheduler.Schedulers

import java.time.Duration.*
import java.util.concurrent.CompletableFuture

class SinksManyMultiCastSuite extends munit.FunSuite {

  val fluxSink = Sinks.many().multicast().directAllOrNothing[Long]()
  CompletableFuture.runAsync(() => while (true) {
    fluxSink.tryEmitNext(System.currentTimeMillis())
    Thread.sleep(1000)
  })

  val timePub = fluxSink.asFlux()

  val sub1: Disposable = timePub.doOnSubscribe(s => println("Sub1"))
//    .share()
//    .subscribeOn(Schedulers.parallel())
    .subscribe(x => println(s"Thread ${Thread.currentThread().getName} sub1 - " + x))

  val sub2: Disposable = timePub.doOnSubscribe(s => println("Sub2"))
//    .share()
//    .subscribeOn(Schedulers.parallel())
    .subscribe(x => println(s"Thread ${Thread.currentThread().getName} sub2 - " + x))

  test("create Source and Sink") {
   Thread.sleep(30000)

  }

}
