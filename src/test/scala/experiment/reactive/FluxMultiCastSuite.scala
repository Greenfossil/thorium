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
