package com.typesafe.config

import java.time.{Duration, Period}
import java.time.temporal.TemporalAmount
import scala.jdk.CollectionConverters.*

extension (config: Config)
  def getStringOpt(path: String): Option[String] =
    if config.getIsNull(path) then None else Option(config.getString(path))

  def getBooleanOpt(path: String): Option[Boolean] =
    if config.getIsNull(path) then None else Option(config.getBoolean(path))

  def getIntOpt(path: String): Option[Int] =
    if config.getIsNull(path) then None else Option(config.getInt(path))

  def getLongOpt(path: String): Option[Long] =
    if config.getIsNull(path) then None else Option(config.getLong(path))

  def getDoubleOpt(path: String): Option[Double] =
    if config.getIsNull(path) then None else Option(config.getDouble(path))

  def getNumberOpt(path: String): Option[Number] =
    if config.getIsNull(path) then None else Option(config.getNumber(path))

  def getDurationOpt(path: String): Option[Duration] =
    if config.getIsNull(path) then None else Option(config.getDuration(path))

  def getPeriodOpt(path: String): Option[Period] =
    if config.getIsNull(path) then None else Option(config.getPeriod(path))

  def getTemporalOption(path: String): Option[TemporalAmount] =
    if config.getIsNull(path) then None else Option(config.getTemporal(path))

  def getEnumOpt[T <: Enum[T]](clazz: Class[T], path: String): Option[T] =
    if config.getIsNull(path) then None else Option(config.getEnum(clazz, path))

  def getBooleanList(path: String): Seq[Boolean] =
    if config.getIsNull(path)
    then Nil
    else
      config.getBooleanList(path)
        .asScala
        .toSeq
        .map(x => Boolean.unbox(x))

  def getIntList(path: String): Seq[Int] =
    if config.getIsNull(path)
    then Nil
    else
      config.getIntList(path)
        .asScala
        .toSeq
        .map(x => Int.unbox(x))

  def getLongList(path: String): Seq[Long] =
    if config.getIsNull(path)
    then Nil
    else
      config.getLongList(path)
        .asScala
        .toSeq
        .map(x => Long.unbox(x))

  def getDoubleList(path: String): Seq[Double] =
    if config.getIsNull(path)
    then Nil
    else
      config.getDoubleList(path)
        .asScala
        .toSeq
        .map(x => Double.unbox(x))

  def getNumberList(path: String): Seq[Number] =
    if config.getIsNull(path)
    then Nil
    else
      config.getNumberList(path)
        .asScala
        .toSeq

  def getStringList(path: String): Seq[String] =
    if config.getIsNull(path)
    then Nil
    else
      config.getStringList(path)
        .asScala
        .toSeq

  def getDurationList(path: String): Seq[Duration] =
    if config.getIsNull(path)
    then Nil
    else
      config.getDurationList(path)
        .asScala
        .toSeq

  def getEnumList[T <: Enum[T]](clazz: Class[T], path: String): Seq[T] =
    if config.getIsNull(path)
    then Nil
    else
      config.getEnumList(clazz, path)
        .asScala
        .toSeq

  def getConfigList(path: String): Seq[Config] =
    if config.getIsNull(path)
    then Nil
    else
      config.getConfigList(path)
        .asScala
        .toSeq

  def getAnyRefList(path: String): Seq[AnyRef] =
    if config.getIsNull(path)
    then Nil
    else
      config.getAnyRefList(path)
        .asScala
        .toSeq