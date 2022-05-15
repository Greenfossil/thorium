package com.greenfossil.webserver

import java.util.Locale
import java.util.Locale.{Builder, LanguageRange}


trait LocaleUtil:
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
   */
  def getBestMatchLocale(acceptedLanguageRanges: Seq[LanguageRange], availableLocales: Seq[Locale], variantOpt: Option[String]): Locale =
    import scala.jdk.CollectionConverters.*
    val bestMatchLocale = Option(Locale.lookup(acceptedLanguageRanges.asJava, availableLocales.asJava)).getOrElse(Locale.getDefault)
    new Builder().setLocale(bestMatchLocale).setVariant(variantOpt.getOrElse("")).build()

object LocaleUtil extends LocaleUtil
