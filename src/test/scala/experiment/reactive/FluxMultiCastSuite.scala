package experiment.reactive

import reactor.core.Disposable
import reactor.core.publisher.{ConnectableFlux, Flux, FluxSink}
import reactor.core.scheduler.Schedulers

import java.time.Duration.*

class FluxMultiCastSuite extends munit.FunSuite {
  val timePub: Flux[Long]  =
    Flux
      .create((fluxSink: FluxSink[Long]) => {
        println(s"FluxSink thread ${Thread.currentThread().getName}")
        while (true) {
          fluxSink.next(System.currentTimeMillis())
          Thread.sleep(1000)
        }
      })
      .publish() /*ConnectableFlux[Long]*/
      .autoConnect()

  val sub1: Disposable = timePub.doOnSubscribe(s => println("Sub1"))
    .share()
    .subscribeOn(Schedulers.parallel())
    .subscribe(x => println(s"Thread ${Thread.currentThread().getName} sub1 - " + x))

  val sub2: Disposable = timePub.doOnSubscribe(s => println("Sub2"))
    .share()
    .subscribeOn(Schedulers.parallel())
    .subscribe(x => println(s"Thread ${Thread.currentThread().getName} sub2 - " + x))

  test("create Source and Sink") {
   Thread.sleep(30000)

  }

}
