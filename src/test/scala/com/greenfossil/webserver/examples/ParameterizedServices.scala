package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param, Post}

object ParameterizedServices extends Controller {

  /*
   * curl http://localhost:8080/howdy - this does not seems to be working?
   * curl http://localhost:8080/howdy/
   * curl http://localhost:8080/howdy/helloworld
   *
   */
  @Get("prefix:/howdy")
  def prefixEndpoint = "prefix - howdy"

  @Get("/{name}/{age}/:contact")
  def bracedParams(@Param name: String, @Param age:Int, @Param contact: String) =
    s"braced-param name:${name} age:${age} contact:${contact}"

  /*
   * curl http://localhost:8080/regex/string/homer - this works
   * curl http://localhost:8080/regex/string/123 - this wont work
   */
  @Get("regex:^/string/(?<name>[^0-9]+)$") //is different without ^$ "regex:/string/(?<name>[^0-9]+)"
  def regexStringEndpoint(@Param name : String) = s"regexString - name:$name"

  @Get("regex:^/number/(?<n1>[0-9]+)$") //$ is import or else this link works curl http://localhost:8080/number/123a
  def regexNumberEndpoint(@Param n1 : Int) = s"regexNumber - $n1"

  @Get("regex:/string2/(?<min>\\w+)/(?<max>\\w+)") //curl http://localhost:8080/string2/min/max
  def regexString2Endpoint(@Param min : String, @Param max: String) = s"regexString2 - min:$min - max:$max"

  @Get("regex:/number2/(?<min>\\d+)/(?<max>\\d+)") //curl http://localhost:8080/number2/10/20
  def regexNumber2Endpoint(@Param min : Int, @Param max: Int) = s"regexNumber2 - min:$min - max:$max"

  @Get("regex:/mix/(?<min>\\d+)/(?<max>\\w+)") //curl http://localhost:8080/mix/10/last
  def regexMix1Endpoint(@Param min : Int, @Param max: String) = s"regexMix1-Int,String - min:$min - max:$max"

  @Get("regex:/mix/(?<min>\\w+)/(?<max>\\d+)") //curl http://localhost:8080/mix/first/20
  def regexMix2Endpoint(@Param min : String, @Param max: Int) = s"regexMix2-String,Int - min:$min - max:$max"

  @Get("regex:/mixbad/:min/(?<max>\\d+)") //this will not work curl http://localhost:8080/mixbad/first/20
  def regexMixBadEndpoint(@Param min : String, @Param max: Int) = s"regex3Mix-String,Int - min:$min - max:$max"

//  @Get("glob:/*/hello/**")
//  def globEndpoint(@Param("0") prefix: String, @Param("1") name: String) = s"glob [$name] and [$name]"


}

