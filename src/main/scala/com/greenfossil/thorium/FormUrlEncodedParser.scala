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

package com.greenfossil.thorium

import java.net.URLDecoder
import scala.collection.immutable.ListMap
import scala.collection.mutable

/** An object for parsing application/x-www-form-urlencoded data */
object FormUrlEncodedParser:

  /**
   * Parse the content type "application/x-www-form-urlencoded" which consists of a bunch of & separated key=value
   * pairs, both of which are URL encoded. This parser to maintain the original order of the
   * keys by using groupBy as some applications depend on the original browser ordering.
   * @param data The body content of the request, or whatever needs to be so parsed
   * @param encoding The character encoding of data
   * @return A ListMap of keys to the sequence of values for that key
   */
  def parse(data: String, encoding: String = "utf-8"): FormUrlEndcoded =

    // Generate the pairs of values from the string.
    val pairs: Seq[(String, String)] = parseToPairs(data, encoding)

    // Group the pairs by the key (first item of the pair) being sure to preserve insertion order
    FormUrlEndcoded(toMap(pairs))

  private val parameterDelimiter = "[&;]".r

  /**
   * Do the basic parsing into a sequence of key/value pairs
   * @param data The data to parse
   * @param encoding The encoding to use for interpreting the data
   * @return The sequence of key/value pairs
   */
  private def parseToPairs(data: String, encoding: String): Seq[(String, String)] =
    val split = parameterDelimiter.split(data)
    if split.length == 1 && split(0).isEmpty then Seq.empty
    else 
      split.toIndexedSeq.map { param =>
        val parts = param.split("=", -1)
        val key   = URLDecoder.decode(parts(0), encoding)
        val value = URLDecoder.decode(parts.lift(1).getOrElse(""), encoding)
        key -> value
      }

  private def toMap[K, V](seq: Seq[(K, V)]): Map[K, Seq[V]] =
    // This mutable map will not retain insertion order for the seq, but it is fast for retrieval. The value is
    // a builder for the desired Seq[String] in the final result.
    val m = mutable.Map.empty[K, mutable.Builder[V, Seq[V]]]

    // Run through the seq and create builders for each unique key, effectively doing the grouping
    for ((key, value) <- seq) m.getOrElseUpdate(key, Seq.newBuilder[V]) += value

    // Create a builder for the resulting ListMap. Note that this one is immutable and will retain insertion order
    val b = ListMap.newBuilder[K, Seq[V]]

    // Note that we are NOT going through m (didn't retain order) but we are iterating over the original seq
    // just to get the keys so we can look up the values in m with them. This is how order is maintained.
    for ((k, v) <- seq.iterator) b += k -> m.getOrElse(k, Seq.newBuilder[V]).result

    // Get the builder to produce the final result
    b.result