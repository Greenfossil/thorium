package experiment.reactive

import munit.IgnoreSuite
import reactor.core.Disposable
import reactor.core.publisher.{ConnectableFlux, Flux, FluxSink}
import reactor.core.scheduler.Schedulers

import java.time.Duration.*

@IgnoreSuite //This test suit will never terminate 
class FluxMultiCastSuite extends munit.FunSuite {

  test("create Source and Sink") {
    val timePub: Flux[Long] =
      Flux
        .create((fluxSink: FluxSink[Long]) => {
          println(s"FluxSink thread ${Thread.currentThread().getName}")
          1 to 5 foreach { _ =>
            fluxSink.next(System.currentTimeMillis())
            Thread.sleep(1000)
          }

        })
        .publish() /*ConnectableFlux[Long]*/
        .autoConnect()

    val sub1: Disposable = timePub.doOnSubscribe(s => println("Sub1"))
      .share()
      .subscribeOn(Schedulers.parallel())
      .subscribe(x => println(s"Flux Multicast - Thread ${Thread.currentThread().getName} sub1 - " + x))

    val sub2: Disposable = timePub.doOnSubscribe(s => println("Sub2"))
      .share()
      .subscribeOn(Schedulers.parallel())
      .subscribe(x => println(s"Flux Multicast - Thread ${Thread.currentThread().getName} sub2 - " + x))
    Thread.sleep(2000)

  }

}
