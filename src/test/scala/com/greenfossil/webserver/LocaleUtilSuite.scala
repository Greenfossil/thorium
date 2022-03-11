package com.greenfossil.webserver

import munit.FunSuite

import java.util.Locale
import java.util.Locale.LanguageRange
import scala.jdk.CollectionConverters.*

class LocaleUtilSuite extends FunSuite{

  test("Get best match"){
    val acceptedLanguages = LanguageRange.parse("en-SG;q=1.0,zh-CN;q=0.5,fr-CA;q=0.0").asScala.toList
    val availableLanguages = Seq("zh", "en", "kr").map(x => new Locale(x))

    val bestMatch = LocaleUtil.getBestMatchLocale(acceptedLanguages, availableLanguages, None)
    assertEquals(bestMatch, new Locale("en"))
  }

  test("Get best match with variant"){
    val acceptedLanguages = LanguageRange.parse("en-SG;q=1.0,zh-CN;q=0.5,fr-CA;q=0.0").asScala.toList
    val availableLanguages = Seq("zh", "en", "kr").map(x => new Locale(x))

    val bestMatch = LocaleUtil.getBestMatchLocale(acceptedLanguages, availableLanguages, Some("elemx"))
    assertEquals(bestMatch, new Locale("en", "", "elemx"))
  }

  test("Get best match with region variant"){
    val acceptedLanguages = LanguageRange.parse("en-SG;q=1.0,zh-CN;q=0.5,fr-CA;q=0.0").asScala.toList
    val availableLanguageTags = Seq("zh-CN", "en-SG")

    val bestMatch = LocaleUtil.getBestMatchFromLanguageTags(acceptedLanguages, availableLanguageTags, Some("elemx"))
    assertEquals(bestMatch, new Locale("en", "SG", "elemx"))
  }

}
