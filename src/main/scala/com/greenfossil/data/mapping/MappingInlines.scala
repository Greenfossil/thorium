package com.greenfossil.data.mapping

import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}
import scala.deriving.Mirror

/*
* Extracts Type  't' from Mapping[t]
*/
type FieldTypeExtractor[Xs <: Tuple] <: Tuple = Xs match {
  case EmptyTuple => Xs
  case Mapping[t] *: ts => t *: FieldTypeExtractor[ts]
  case (String, Mapping[t]) *: ts => t *: FieldTypeExtractor[ts]
}

/*
 * Constructs Mapping[t] from a given type 't'
 */
type FieldConstructor[X <:Tuple] <: Tuple = X match {
  case EmptyTuple => X
  case t *: ts => Mapping[t] *: FieldConstructor[ts]
}

def toNamedFieldTuple(tuple: Tuple): Mapping[?] *: Tuple =
  tuple.map[[X] =>> Mapping[?]]([X] => (x: X) =>
    x match
      case (name: String, f: Mapping[?]) => f.name(name)
  ).asInstanceOf[Mapping[?] *: Tuple]

trait MappingInlines {

  import scala.compiletime.*

  inline def mapTo[A]: Mapping[A] =
    inline erasedValue[A] match {
      case _: String             => ScalarMapping("String", binder = Formatter.checkRequiredFormat.asInstanceOf[Formatter[A]])
      case _: Int                => ScalarMapping("Int", binder = Formatter.intFormat.asInstanceOf[Formatter[A]])
      case _: Long               => ScalarMapping("Long", binder = Formatter.longFormat.asInstanceOf[Formatter[A]])
      case _: Double             => ScalarMapping("Double", binder = Formatter.doubleFormat.asInstanceOf[Formatter[A]])
      case _: Float              => ScalarMapping("Float", binder = Formatter.floatFormat.asInstanceOf[Formatter[A]])
      case _: Boolean            => ScalarMapping("Boolean", binder = Formatter.booleanFormat.asInstanceOf[Formatter[A]])
      case _: LocalDateTime      => ScalarMapping("LocalDateTime", binder = Formatter.localDateTimeFormat.asInstanceOf[Formatter[A]])
      case _: LocalDate          => ScalarMapping("LocalDate", binder = Formatter.localDateFormat.asInstanceOf[Formatter[A]])
      case _: LocalTime          => ScalarMapping("LocalTime", binder = Formatter.localTimeFormat.asInstanceOf[Formatter[A]])
      case _: YearMonth          => ScalarMapping("YearMonth", binder = Formatter.yearMonthFormat.asInstanceOf[Formatter[A]])
      case _: java.sql.Timestamp => ScalarMapping("SqlTimestamp", binder = Formatter.sqlTimestampFormat.asInstanceOf[Formatter[A]])
      case _: java.sql.Date      => ScalarMapping("SqlDate", binder = Formatter.sqlDateFormat.asInstanceOf[Formatter[A]])
      case _: java.util.Date     => ScalarMapping("Date", binder = Formatter.dateFormat.asInstanceOf[Formatter[A]])
      case _: java.util.UUID     => ScalarMapping("UUID", binder = Formatter.uuidFormat.asInstanceOf[Formatter[A]])
      case _: Byte               => ScalarMapping("Byte", binder = Formatter.byteFormat.asInstanceOf[Formatter[A]])
      case _: Short              => ScalarMapping("Short", binder = Formatter.shortFormat.asInstanceOf[Formatter[A]])
      case _: BigDecimal         => ScalarMapping("BigDecimal", binder = Formatter.bigDecimalFormat.asInstanceOf[Formatter[A]])
      case _: Char               => ScalarMapping("Char", binder = Formatter.charFormat.asInstanceOf[Formatter[A]])
      case _: Option[a]          => OptionalMapping[a]("?", elemField = mapTo[a]).asInstanceOf[Mapping[A]]
      case _: Set[a]             => SeqMapping[a]("[Set", elemField = mapTo[a]).asInstanceOf[Mapping[A]]
      case _: IndexedSeq[a]      => SeqMapping[a]("[IndexSeq", elemField = mapTo[a]).asInstanceOf[Mapping[A]]
      case _: Vector[a]          => SeqMapping[a]("[Vector", elemField = mapTo[a]).asInstanceOf[Mapping[A]]
      case _: List[a]            => SeqMapping[a]("[List", elemField = mapTo[a]).asInstanceOf[Mapping[A]]
      case _: Seq[a]             => SeqMapping[a]("[Seq", elemField = mapTo[a]).asInstanceOf[Mapping[A]]
      case _: Tuple              => ProductMapping("P-") // "P-"
      case _: Product            => ProductMapping("P+") //"P+" //Product must be tested last
      case _: Any                => ScalarMapping("Any", binder = null)
    }
  /*
 * https://www.playframework.com/documentation/2.8.x/api/scala/play/api/data/Forms$.html
 */
  //Numeric
  inline def boolean = mapTo[Boolean]

  inline def byteNumber = mapTo[Byte]

  inline def byteNumber(min: Byte = Byte.MinValue, max: Byte = Byte.MaxValue, strict: Boolean = false) =
   mapTo[Byte]
     .verifying(Constraints.min(min, strict), Constraints.max(max, strict))

  inline def shortNumber = mapTo[Short]

  inline def shortNumber(min: Short = Short.MinValue, max: Short = Short.MinValue, strict: Boolean = false) =
    mapTo[Short]
      .verifying(Constraints.min[Short](min, strict), Constraints.max[Short](max, strict))

  inline def number = mapTo[Int]

  inline def number(min:Int, max:Int) =
    mapTo[Int]
      .verifying(Constraints.min(min), Constraints.max(max))

  inline def longNumber = mapTo[Long]

  inline def longNumber(min: Long = Long.MinValue, max: Long = Long.MaxValue, strict: Boolean = false) =
    mapTo[Long]
      .verifying(Constraints.min[Long](min, strict), Constraints.max[Long](max, strict))

  inline def double = mapTo[Double]

  inline def float = mapTo[Float]

  inline def bigDecimal = mapTo[BigDecimal]

  inline def bigDecimal(precision: Int, scale: Int) =
    mapTo[BigDecimal]
      .verifying(Constraints.precision(precision, scale))

  //Text
  inline def char = mapTo[Char]

  inline def text:Mapping[String] = text(trimmed = true)

  inline def text(trimmed: Boolean):Mapping[String] = text(0, Int.MaxValue, trimmed)

  inline def text(minLength: Int, maxLength: Int, trim: Boolean): Mapping[String] =
    val _text = if trim then mapTo[String].transform[String](_.trim) else mapTo[String]
    (minLength, maxLength)  match 
      case (min, Int.MaxValue) => _text.verifying(Constraints.minLength(min))
      case (0, max)            => _text.verifying(Constraints.maxLength(max))
      case (min, max)          => _text.verifying(Constraints.minLength(min), Constraints.maxLength(max))

  inline def nonEmptyText: Mapping[String] = nonEmptyText(1, Int.MaxValue, trimmed = true)


  inline def nonEmptyText(minLength: Int, maxLength: Int, trimmed: Boolean): Mapping[String] =
    val _text = if trimmed then mapTo[String].transform[String](_.trim) else mapTo[String]
    _text.verifying(Constraints.nonEmpty, Constraints.minLength(minLength), Constraints.maxLength(maxLength))

  inline def email =
    mapTo[String]
      .verifying(Constraints.emailAddress)

  //Temporal
  inline def date = mapTo[java.util.Date]

  inline def dateUsing(pattern: String) =
    mapTo[java.util.Date]
      .binder(Formatter.dateFormat(pattern))

  inline def localDate = mapTo[LocalDate]

  inline def localDateUsing(pattern: String) =
    mapTo[LocalDate]
      .binder(Formatter.localDateFormat(pattern))

  inline def localDateTime = mapTo[LocalDateTime]

  inline def localDateTimeUsing(pattern: String) =
    mapTo[LocalDateTime]
      .binder(Formatter.localDateTimeFormat(pattern))

  inline def localTime = mapTo[LocalTime]

  inline def localTimeUsing(pattern: String) =
    mapTo[LocalTime]
      .binder(Formatter.localTimeFormat(pattern))

  inline def yearMonth = mapTo[YearMonth]

  inline def yearMonthUsing(pattern: String) =
    mapTo[YearMonth]
      .binder(Formatter.yearMonthFormat(pattern))

  inline def sqlDate = mapTo[java.sql.Date]

  inline def sqlDateUsing(pattern: String) =
    mapTo[java.sql.Date]
      .binder(Formatter.sqlDateFormat(pattern))

  inline def sqlTimestamp = mapTo[java.sql.Timestamp]

  inline def sqlTimestampUsing(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =
    mapTo[java.sql.Timestamp]
      .binder(Formatter.sqlTimestampFormat(pattern, timeZone))

  inline def uuid = mapTo[java.util.UUID]

  inline def checked(msg: String) =
    mapTo[Boolean]
      .verifying(msg, _ == true)

  inline def default[A](mapping: Mapping[A], defaultValue: A): Mapping[A] =
    DelegateMapping[A, A](tpe = "#", value = Option(defaultValue), delegate =  mapping, a =>
      Option(a).getOrElse(defaultValue)
    )

  inline def ignored[A](value: A): Mapping[A] = mapTo[A].transform[A](_ => value)

  inline def tuple[A <: Tuple](nameValueTuple: A): Mapping[FieldTypeExtractor[A]] =
    mapTo[FieldTypeExtractor[A]]
      .mappings(toNamedFieldTuple(nameValueTuple), null)

  inline def mapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Mapping[A] =
    mapTo[A]
      .mappings(toNamedFieldTuple(nameValueTuple), m)

  inline def optional[A]:Mapping[Option[A]] = mapTo[Option[A]]

  inline def optional[A](field: Mapping[A]): Mapping[Option[A]] =
    OptionalMapping[A]("?", elemField = field).asInstanceOf[Mapping[Option[A]]]

  inline def optionalTuple[A <: Tuple](nameValueTuple: A): Mapping[Option[FieldTypeExtractor[A]]] =
    mapTo[Option[FieldTypeExtractor[A]]]
      .mappings(toNamedFieldTuple(nameValueTuple), null)
      .asInstanceOf[Mapping[Option[FieldTypeExtractor[A]]]]

  inline def optionalMapping[A](using m: Mirror.ProductOf[A])
                               (nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Mapping[Option[A]] =
    val elemField = mapping[A](nameValueTuple)
    OptionalMapping(tpe = "?", elemField = elemField).asInstanceOf[Mapping[Option[A]]]

  inline def seq[A] = mapTo[Seq[A]]

  inline def repeatedTuple[A <: Tuple](nameValueTuple: A) =
    val elemField = tuple[A](nameValueTuple)
    SeqMapping(tpe = "[Seq", elemField = elemField).asInstanceOf[Mapping[Seq[FieldTypeExtractor[A]]]]

  inline def repeatedMapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]) =
    new SeqMapping[A](tpe="[Seq", elemField = mapping[A](nameValueTuple)).asInstanceOf[Mapping[Seq[A]]]

}
