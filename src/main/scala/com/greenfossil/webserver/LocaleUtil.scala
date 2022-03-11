package com.greenfossil.webserver

import scala.jdk.CollectionConverters.*

import java.util.Locale
import java.util.Locale.LanguageRange
import java.util.Locale.Builder

trait LocaleUtil {

  /**
   * Get best matched locale from the accepted language ranges and available language tags. 
   *
   * @param acceptedLanguageRange Accepted Language Ranges
   * @param availableLanguageTags Available Language Tags, e.g. "en-SG", "en-US", "zh-CN"
   * @param variantOpt            Locale variant
   */
  def getBestMatchFromLanguageTags(acceptedLanguageRange: Seq[LanguageRange], availableLanguageTags: Seq[String], variantOpt: Option[String]): Locale =
    val availableLocales = availableLanguageTags.map(tag => new Locale.Builder().setLanguageTag(tag).build())
    getBestMatchLocale(acceptedLanguageRange, availableLocales, variantOpt)

  /**
   * Get best matched locale from the accepted language ranges and available locales. 
   * 
   * @param acceptedLanguageRanges
   * @param availableLocales
   * @param variationOpt
   * @return
   */
  def getBestMatchLocale(acceptedLanguageRanges: Seq[LanguageRange], availableLocales: Seq[Locale], variationOpt: Option[String]): Locale =
    val bestMatchLocale = Locale.lookup(acceptedLanguageRanges.asJava, availableLocales.asJava)
    new Builder().setLocale(bestMatchLocale).setVariant(variationOpt.getOrElse("")).build()

}

object LocaleUtil extends LocaleUtil
