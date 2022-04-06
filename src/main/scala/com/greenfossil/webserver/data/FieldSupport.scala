//package com.greenfossil.commons.data
//
//import com.greenfossil.commons.data.Form.{FieldConstructor, FieldTypeExtractor, toNamedFieldTuple}
//
//import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}
//import scala.deriving.Mirror
//
//trait FieldSupport {
//
//  import scala.compiletime.*
//
//  inline def fieldType[A]: String =
//    inline erasedValue[A] match {
//      case _: Int                => "Int"
//      case _: String             => "String"
//      case _: Long               => "Long"
//      case _: Double             => "Double"
//      case _: Float              => "Float"
//      case _: Boolean            => "Boolean"
//      case _: LocalDate          => "LocalDate"
//      case _: LocalTime          => "LocalTime"
//      case _: LocalDateTime      => "LocalDateTime"
//      case _: YearMonth          => "YearMonth"
//      case _: java.sql.Date      => "SqlDate"
//      case _: java.util.Date     => "Date"
//      case _: java.sql.Timestamp => "SqlTimestamp"
//      case _: java.util.UUID     => "UUID"
//      case _: Byte               => "Byte"
//      case _: Short              => "Short"
//      case _: BigDecimal         => "BigDecimal"
//      case _: Char               => "Char"
//      case _: Seq[a]             => "[" + fieldType[a]
//      case _: Option[a]          => "?" + fieldType[a]
//    }
//
//  def toValueOf[A](tpe: String, value: Any): Option[A] = {
//    if value == null || value == Some(null) || value == "" then None
//    else {
//      val optValue = tpe match {
//
//        case "String" =>
//          value match {
//            case s: String => Option(s)
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Int" =>
//          value match {
//            case x: Int => Option(x)
//            case x: Long => Option(x.toInt)
//            case x: BigDecimal => Option(x.toInt)
//            case s: String => s.toIntOption
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Long" =>
//          value match {
//            case x: Int => Option(x.toLong)
//            case x: Long  => Option(x)
//            case s: BigDecimal /* This is to handle JsNumber type */ => Option(s.toLong)
//            case s: String => s.toLongOption
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Double" =>
//          value match {
//            case x: Double => Option(x)
//            case x: Float => Option(x.toDouble)
//            case x: BigDecimal => Option(x.toDouble)
//            case s: String => s.toDoubleOption
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Float" =>
//          value match {
//            case x: Float => Option(x)
//            case x: Double => Option(x.toFloat)
//            case x: BigDecimal => Option(x.toFloat)
//            case s: String => s.toFloatOption
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Boolean" =>
//          value match {
//            case x: Boolean => Option(x)
//            case s: String => s.toBooleanOption
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "LocalDate" =>
//          value match {
//            case x: LocalDate => Option(x)
//            case s: String => Option(LocalDate.parse(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "LocalTime" =>
//          value match {
//            case x: LocalTime => Option(x)
//            case s: String => Option(LocalTime.parse(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "YearMonth" =>
//          value match {
//            case x: YearMonth => Option(x)
//            case s: String => Option(YearMonth.parse(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "SqlDate" =>
//          value match {
//            case x: java.sql.Date => Option(x)
//            case s: String => Option(java.sql.Date.valueOf(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Date" =>
//          value match {
//            case x: java.util.Date => Option(x)
//            case s: String => Option(java.util.Date.parse(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "SqlTimestamp" =>
//          value match {
//            case x: java.sql.Timestamp => Option(x)
//            case s: String => Option(java.sql.Timestamp.valueOf(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "LocalDateTime" =>
//          value match {
//            case x: LocalDateTime => Option(x)
//            case s: String => Option(LocalDateTime.parse(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "UUID" =>
//          value match {
//            case x: java.util.UUID => Option(x)
//            case s: String => Option(java.util.UUID.fromString(s))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Byte" =>
//          value match {
//            case x: Byte => Option(x)
//            case s: String => Option(s.toByte)
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Short" =>
//          value match {
//            case x: Short => Option(x)
//            case s: String => Option(s.toShort)
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "BigDecimal" =>
//          value match {
//            case x: BigDecimal => Option(x)
//            case s: String => Option(s).filter(_.nonEmpty).map(x => BigDecimal(x))
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case "Char" =>
//          value match {
//            case x: Char => Option(x)
//            case s: String => s.headOption
//            case opt: Option[_] => opt.flatMap{toValueOf(tpe, _)}
//            case xs: Seq[_] => xs.headOption.flatMap{toValueOf(tpe, _)}
//            case _ => None
//          }
//
//        case seqType if tpe.startsWith("[") =>
//          value match {
//            case opt: Option[_] =>
//              //Seq is wrapped in Opt, unwrap it first and bind as Seq
//              opt.flatMap(toValueOf(tpe, _))
//            case xs: Seq[_] =>
//              Option(xs.flatMap(toValueOf(tpe.tail,_)))
//            case s: String =>
//              Option(Seq(toValueOf(tpe.tail, s)))
//          }
//
//        case optType if tpe.startsWith("?") =>
//          value match {
//            case opt: Option[_] => opt.map{toValueOf(tpe.tail, _)}
//            case xs: Seq[_] => xs.headOption.map{toValueOf(tpe.tail, _)}
//            case s: String => toValueOf(tpe.tail, s)
//          }
//      }
//      optValue.asInstanceOf[Option[A]]
//    }
//  }
//
//}
//
//
//
///*
// * https://www.playframework.com/documentation/2.8.x/api/scala/play/api/data/Forms$.html
// */
////Numeric
//inline def boolean = Field.of[Boolean]
//inline def byteNumber = Field.of[Byte]
//inline def byteNumber(min: Byte = Byte.MinValue, max: Byte = Byte.MaxValue, strict: Boolean = false) =
//  Field.of[Byte].verifying(Constraints.min(min, strict), Constraints.max(max, strict))
//
//inline def shortNumber = Field.of[Short]
//inline def shortNumber(min: Short = Short.MinValue, max: Short = Short.MinValue, strict: Boolean = false) =
//  Field.of[Short].verifying(Constraints.min[Short](min, strict), Constraints.max[Short](max, strict))
//inline def number = Field.of[Int]
//inline def number(min:Int, max:Int) = Field.of[Int].verifying(Constraints.min(min), Constraints.max(max))
//inline def longNumber = Field.of[Long]
//inline def longNumber(min: Long = Long.MinValue, max: Long = Long.MaxValue, strict: Boolean = false) =
//  Field.of[Long].verifying(Constraints.min[Long](min, strict), Constraints.max[Long](max, strict))
//inline def double = Field.of[Double]
//inline def float = Field.of[Float]
//inline def bigDecimal = Field.of[BigDecimal]
//inline def bigDecimal(precision: Int, scale: Int) =
//  Field.of[BigDecimal].verifying(Constraints.precision(precision, scale))
//
////Text
//inline def char = Field.of[Char]
//
//inline def text:Field[String] = Field.of[String]
//
//inline def text(minLength: Int, maxLength: Int, trim: Boolean): Field[String] = (minLength, maxLength)  match {
//  case (min, Int.MaxValue) => text.verifying(Constraints.minLength(min))
//  case (0, max)            => text.verifying(Constraints.maxLength(max))
//  case (min, max)          => text.verifying(Constraints.minLength(min), Constraints.maxLength(max))
//}
//
//inline def nonEmptyText =
//  Field.of[String].verifying(Constraints.nonEmpty)
//
//inline def nonEmptyText(minLength: Int = 0, maxLength: Int = Int.MaxValue) =
//  Field.of[String].verifying(Constraints.minLength(minLength), Constraints.maxLength(maxLength))
//
//inline def email =
//  Field.of[String].verifying(Constraints.emailAddress)
//
////Temporal
//inline def date = Field.of[java.util.Date]
//
//inline def date(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) = Field.of[java.util.Date]
//inline def localDate = Field.of[java.time.LocalDate]
//inline def localDate(pattern: String) = Field.of[java.time.LocalDate]
//inline def localDateTime = Field.of[java.time.LocalDateTime]
//inline def localDateTime(pattern: String) = Field.of[java.time.LocalDateTime]
//inline def localTime = Field.of[java.time.LocalTime]
//inline def localTime(pattern: String) = Field.of[java.time.LocalTime]
//inline def yearMonth = Field.of[java.time.YearMonth]
//inline def yearMonth(pattern: String) = Field.of[java.time.YearMonth]
//inline def sqlDate = Field.of[java.sql.Date]
//inline def sqlDate(pattern: String) = Field.of[java.sql.Date]
//inline def sqlTimestamp = Field.of[java.sql.Timestamp]
//inline def sqlTimestamp(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =  Field.of[java.sql.Timestamp]
//
////Collection
//inline def indexedSeq[A] = Field.of[IndexedSeq[A]]
//inline def list[A] = Field.of[List[A]]
//@deprecated("Use list[A]", "")
//inline def list[A](a: Field[A]): Field[List[A]] = Field.of[List[A]]
//inline def seq[A]: Field[Seq[A]] = Field.of[Seq[A]]
//
//inline def mapping[A](using m: Mirror.ProductOf[A])(
//  nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]
//): Field[A] =
//  new Field(tpe = s"C-$m", mappings = Form.toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))
//
//inline def mappingRepeat[A](using m: Mirror.ProductOf[A])(
//  nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]
//): Field[Seq[A]] =
//  val mappingField =  new Field[A](tpe = s"C-$m", mappings = Form.toNamedFieldTuple(nameValueTuple), mirrorOpt = Some(m))
//  new Field[Seq[A]](tpe = s"[C-$m", mappings = mappingField *: EmptyTuple)
//
//inline def tuple[A <: Tuple](nameValueTuple: A): Field[FieldTypeExtractor[A]] =
//  new Field(tpe = s"C-", mappings = toNamedFieldTuple(nameValueTuple))
//
//inline def tupleRepeat[A <: Tuple](nameValueTuple: A): Field[Seq[FieldTypeExtractor[A]]] =
//  val mappingField = new Field[A](tpe = s"C-", mappings = toNamedFieldTuple(nameValueTuple))
//  new Field[Seq[FieldTypeExtractor[A]]]("[C-", mappings = mappingField *: EmptyTuple)
//
//@deprecated("Use seq[A]", "")
//inline def seq[A](a: Field[A]): Field[Seq[A]] = Field.of[Seq[A]]
//inline def set[A] = Field.of[Set[A]]
////inline def vector[A] = Field.of[Vector[A]]
//inline def optional[A] = Field.of[Option[A]]
//@deprecated("Use optional[A]", "")
//inline def optional[A](a: Field[A]): Field[Option[A]] = Field.of[Option[A]]
//
//inline def uuid = Field.of[java.util.UUID]
//inline def checked(msg: String) = Field.of[Boolean].verifying(msg, _ == true)
//inline def default[A](mapping: Field[A], value: A): Field[A] = Field.of[A].fill(value) //FIXME OptionalMapping(mapping).transform(_.getOrElse(value), Some(_))
//inline def ignored[A](value: A): Field[A] = ??? // of(ignoredFormat(value))
