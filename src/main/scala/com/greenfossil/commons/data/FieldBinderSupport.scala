//package com.greenfossil.commons.data
//
//import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}
//
//import scala.compiletime.*
//import Formatter.*
//
//inline def binderOf[A]: Formatter[A] =
//  inline erasedValue[A] match
//    case _: String             => stringFormat.asInstanceOf[Formatter[A]]
//    case _: Int                => intFormat.asInstanceOf[Formatter[A]]
//    case _: Long               => longFormat.asInstanceOf[Formatter[A]]
//    case _: Double             => doubleFormat.asInstanceOf[Formatter[A]]
//    case _: Float              => floatFormat.asInstanceOf[Formatter[A]]
//    case _: Boolean            => booleanFormat.asInstanceOf[Formatter[A]]
//    case _: LocalDateTime      => localDateTimeFormat.asInstanceOf[Formatter[A]]
//    case _: LocalDate          => localDateFormat.asInstanceOf[Formatter[A]]
//    case _: LocalTime          => localTimeFormat.asInstanceOf[Formatter[A]]
//    case _: YearMonth          => yearMonthFormat.asInstanceOf[Formatter[A]]
//    case _: java.sql.Timestamp => sqlTimestampFormat.asInstanceOf[Formatter[A]]
//    case _: java.sql.Date      => sqlDateFormat.asInstanceOf[Formatter[A]]
//    case _: java.util.Date     => dateFormat.asInstanceOf[Formatter[A]]
//    case _: java.util.UUID     => uuidFormat.asInstanceOf[Formatter[A]]
//    case _: Byte               => byteFormat.asInstanceOf[Formatter[A]]
//    case _: Short              => shortFormat.asInstanceOf[Formatter[A]]
//    case _: BigDecimal         => bigDecimalFormat.asInstanceOf[Formatter[A]]
//    case _: Char               => charFormat.asInstanceOf[Formatter[A]]
//    case _: Seq[a]             => binderOf[a].asInstanceOf[Formatter[A]]
//    case _: Option[a]          => binderOf[a].asInstanceOf[Formatter[A]]
//    case _                     => null
//
//
