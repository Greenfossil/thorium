package com.greenfossil.webserver.examples

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param, Post}

/*
 * Please ensure com.greenfossil.webserver.examples.main is started
 */
object ParameterizedServices extends Controller {

  /*
   * curl http://localhost:8080/howdy - this does not seems to be working?
   * curl http://localhost:8080/howdy/
   * curl http://localhost:8080/howdy/helloworld
   *
   */
  @Get("prefix:/howdy")
  def prefixEndpoint = "prefix - howdy"

  /*
   * curl http://localhost:8080/braced-params/home/42/1234
   */
  @Get("/braced-params/{name}/{age}/:contact")
  def bracedParams(@Param name: String, @Param age:Int, @Param contact: String) =
    s"braced-param name:${name} age:${age} contact:${contact}"

  /*
   * curl http://localhost:8080/string/homer - this works
   * curl http://localhost:8080/string/123 - this wont work
   */
  @Get("regex:^/string/(?<name>[^0-9]+)$") //This path is different without ^$ "regex:/string/(?<name>[^0-9]+)"
  def regexStringEndpoint(@Param name : String) = s"regexString - name:$name"

  /*
   * curl http://localhost:8080/number/123
   * '$' is important or else this link works curl http://localhost:8080/number/123a
   */
  @Get("regex:^/number/(?<n1>[0-9]+)$") //
  def regexNumberEndpoint(@Param n1 : Int) = s"regexNumber - $n1"

  /*
   * curl http://localhost:8080/string2/min/max
   */
  @Get("regex:/string2/(?<min>\\w+)/(?<max>\\w+)")
  def regexString2Endpoint(@Param min : String, @Param max: String) = s"regexString2 - min:$min - max:$max"

  /*
   * curl http://localhost:8080/number2/10/20
   */
  @Get("regex:/number2/(?<min>\\d+)/(?<max>\\d+)")
  def regexNumber2Endpoint(@Param min : Int, @Param max: Int) = s"regexNumber2 - min:$min - max:$max"

  /*
   * curl http://localhost:8080/mix/10/last
   */
  @Get("regex:/mix/(?<min>\\d+)/(?<max>\\w+)")
  def regexMix1Endpoint(@Param min : Int, @Param max: String) = s"regexMix1-Int,String - min:$min - max:$max"

  /*
   * curl http://localhost:8080/mix/first/20
   */
  @Get("regex:/mix/(?<min>\\w+)/(?<max>\\d+)")
  def regexMix2Endpoint(@Param min : String, @Param max: Int) = s"regexMix2-String,Int - min:$min - max:$max"

  /*
   * this will not work
   * curl http://localhost:8080/mixbad/first/20
   */
  @Get("regex:/mixbad/:min/(?<max>\\d+)")
  def regexMixBadEndpoint(@Param min : String, @Param max: Int) = s"regex3Mix-String,Int - min:$min - max:$max"

  /*
   * glob: is currently not supported
   */
//  @Get("glob:/*/hello/**")
//  def globEndpoint(@Param("0") prefix: String, @Param("1") name: String) = s"glob [$name] and [$name]"

  /**
   * The following routes work:
   * curl http://localhost:8080/ci/Homer
   * curl http://localhost:8080/ci/homer
   * curl http://localhost:8080/ci/marge
   *
   * The following routes do not work:
   * curl http://localhost:8080/ci/lisa
   * curl http://localhost:8080/CI/homer
   * @param name
   * @return
   */
  @Get("regex:^/ci/(?<name>(?i)Homer|Marge)")
  def caseInsensitivity(@Param name: String) = name

  /**
   * The following routes work:
   * curl http://localhost:8080/ci2/Homer
   * curl http://localhost:8080/ci2/homer
   * curl http://localhost:8080/ci2/marge
   * curl http://localhost:8080/CI2/homer
   *
   * The following routes do not work:
   * curl http://localhost:8080/ci2/lisa
   * @param name
   * @return
   */
  @Get("regex:(?i)^/ci2/(?<name>Homer|Marge)")
  def caseInsensitivity2(@Param name: String) = name

  /**
   * The following routes work:
   * curl http://localhost:8080/ci3/Homer
   * curl http://localhost:8080/ci3/Marge
   * curl http://localhost:8080/CI3/Homer
   *
   * The following routes do not work:
   * curl http://localhost:8080/ci3/homer
   * curl http://localhost:8080/ci3/lisa
   * @param name
   * @return
   */
  @Get("regex:^/(?i)ci3(?-i)/(?<name>Homer|Marge)")
  def caseInsensitivity3(@Param name: String) = name

  /**
   * The following routes work:
   * curl http://localhost:8080/ci4/Homer
   * curl http://localhost:8080/ci4/Marge
   * curl http://localhost:8080/ci4/homer
   *
   * The following routes do not work:
   * curl http://localhost:8080/CI4/Homer
   * curl http://localhost:8080/ci4/lisa
   * @param name
   * @return
   */
  @Get("regex:^/ci4/(?<name>(?i)Homer|Marge(?-i))")
  def caseInsensitivity4(@Param name: String) = name
}

