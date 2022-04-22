package com.greenfossil.commons.data

import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}
import scala.deriving.Mirror

trait MappingInlines { self: Mapping.type =>

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


  import scala.compiletime.*

  //todo - renamed
  inline def fieldOf[A]: Mapping[A] =
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
      case _: Option[a]          => OptionalMapping[a]("?", elemField = fieldOf[a]).asInstanceOf[Mapping[A]]
      case _: Set[a]             => SeqMapping[a]("[Set", elemField = fieldOf[a]).asInstanceOf[Mapping[A]]
      case _: IndexedSeq[a]      => SeqMapping[a]("[IndexSeq", elemField = fieldOf[a]).asInstanceOf[Mapping[A]]
      case _: Vector[a]          => SeqMapping[a]("[Vector", elemField = fieldOf[a]).asInstanceOf[Mapping[A]]
      case _: List[a]            => SeqMapping[a]("[List", elemField = fieldOf[a]).asInstanceOf[Mapping[A]]
      case _: Seq[a]             => SeqMapping[a]("[Seq", elemField = fieldOf[a]).asInstanceOf[Mapping[A]]
      case _: Tuple              => ProductMapping("P-") // "P-"
      case _: Product            => ProductMapping("P+") //"P+" //Product must be tested last
      case _: Any                => ScalarMapping("Any", binder = null)
    }
  /*
 * https://www.playframework.com/documentation/2.8.x/api/scala/play/api/data/Forms$.html
 */
  //Numeric
  inline def boolean = fieldOf[Boolean]

  inline def byteNumber = fieldOf[Byte]

  inline def byteNumber(min: Byte = Byte.MinValue, max: Byte = Byte.MaxValue, strict: Boolean = false) =
   fieldOf[Byte]
     .verifying(Constraints.min(min, strict), Constraints.max(max, strict))

  inline def shortNumber = fieldOf[Short]

  inline def shortNumber(min: Short = Short.MinValue, max: Short = Short.MinValue, strict: Boolean = false) =
    fieldOf[Short]
      .verifying(Constraints.min[Short](min, strict), Constraints.max[Short](max, strict))

  inline def number = fieldOf[Int]

  inline def number(min:Int, max:Int) =
    fieldOf[Int]
      .verifying(Constraints.min(min), Constraints.max(max))

  inline def longNumber = fieldOf[Long]

  inline def longNumber(min: Long = Long.MinValue, max: Long = Long.MaxValue, strict: Boolean = false) =
    fieldOf[Long]
      .verifying(Constraints.min[Long](min, strict), Constraints.max[Long](max, strict))

  inline def double = fieldOf[Double]

  inline def float = fieldOf[Float]

  inline def bigDecimal = fieldOf[BigDecimal]

  inline def bigDecimal(precision: Int, scale: Int) =
    fieldOf[BigDecimal]
      .verifying(Constraints.precision(precision, scale))

  //Text
  inline def char = fieldOf[Char]

  inline def text:Mapping[String] = fieldOf[String]

  inline def text(minLength: Int, maxLength: Int, trim: Boolean): Mapping[String] =
    val _text = if trim then text.transform[String](_.trim) else text
    (minLength, maxLength)  match {
      case (min, Int.MaxValue) => _text.verifying(Constraints.minLength(min))
      case (0, max)            => _text.verifying(Constraints.maxLength(max))
      case (min, max)          => _text.verifying(Constraints.minLength(min), Constraints.maxLength(max))
    }

  inline def nonEmptyText =
    fieldOf[String]
      .verifying(Constraints.nonEmpty)

  inline def nonEmptyText(minLength: Int = 0, maxLength: Int = Int.MaxValue) =
    fieldOf[String]
      .verifying(Constraints.minLength(minLength), Constraints.maxLength(maxLength))

  inline def email =
    fieldOf[String]
      .verifying(Constraints.emailAddress)

  //Temporal
  inline def date = fieldOf[java.util.Date]

  inline def dateUsing(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =
    fieldOf[java.util.Date]
      .binder(Formatter.dateFormat(pattern, timeZone))

  inline def localDate = fieldOf[LocalDate]

  inline def localDateUsing(pattern: String) =
    fieldOf[LocalDate]
      .binder(Formatter.localDateFormat(pattern))

  inline def localDateTime = fieldOf[LocalDateTime]

  inline def localDateTimeUsing(pattern: String) =
    fieldOf[LocalDateTime]
      .binder(Formatter.localDateTimeFormat(pattern))

  inline def localTime = fieldOf[LocalTime]

  inline def localTimeUsing(pattern: String) =
    fieldOf[LocalTime]
      .binder(Formatter.localTimeFormat(pattern))

  inline def yearMonth = fieldOf[YearMonth]

  inline def yearMonthUsing(pattern: String) =
    fieldOf[YearMonth]
      .binder(Formatter.yearMonthFormat(pattern))

  inline def sqlDate = fieldOf[java.sql.Date]

  inline def sqlDateUsing(pattern: String) =
    fieldOf[java.sql.Date]
      .binder(Formatter.sqlDateFormat(pattern))

  inline def sqlTimestamp = fieldOf[java.sql.Timestamp]

  inline def sqlTimestampUsing(pattern: String, timeZone: java.util.TimeZone = java.util.TimeZone.getDefault) =
    fieldOf[java.sql.Timestamp]
      .binder(Formatter.sqlTimestampFormat(pattern, timeZone))

  inline def uuid = fieldOf[java.util.UUID]

  inline def checked(msg: String) =
    fieldOf[Boolean]
      .verifying(msg, _ == true)

  inline def default[A](mapping: Mapping[A], defaultValue: A): Mapping[A] =
    DelegateMapping[A, A](tpe = "#", value = Option(defaultValue), delegate =  mapping, a =>
      Option(a).getOrElse(defaultValue)
    )

  inline def ignored[A](value: A): Mapping[A] = fieldOf[A].transform[A](_ => value)

  inline def tuple[A <: Tuple](nameValueTuple: A): Mapping[FieldTypeExtractor[A]] =
    fieldOf[FieldTypeExtractor[A]]
      .mappings(toNamedFieldTuple(nameValueTuple), null)

  inline def mapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Mapping[A] =
    fieldOf[A]
      .mappings(toNamedFieldTuple(nameValueTuple), m)

  inline def optional[A]:Mapping[Option[A]] = fieldOf[Option[A]]

  inline def optional[A](field: Mapping[A]): Mapping[Option[A]] =
    OptionalMapping[A]("?", elemField = field).asInstanceOf[Mapping[Option[A]]]

  inline def optionalTuple[A <: Tuple](nameValueTuple: A): Mapping[Option[FieldTypeExtractor[A]]] =
    fieldOf[Option[FieldTypeExtractor[A]]]
      .mappings(toNamedFieldTuple(nameValueTuple), null)
      .asInstanceOf[Mapping[Option[FieldTypeExtractor[A]]]]

  inline def optionalMapping[A](using m: Mirror.ProductOf[A])
                               (nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]): Mapping[Option[A]] =
    val elemField = mapping[A](nameValueTuple)
    OptionalMapping(tpe = "?", elemField = elemField).asInstanceOf[Mapping[Option[A]]]

  inline def seq[A] = fieldOf[Seq[A]]

  inline def repeatedTuple[A <: Tuple](nameValueTuple: A) =
    val elemField = tuple[A](nameValueTuple)
    SeqMapping(tpe = "[Seq", elemField = elemField).asInstanceOf[Mapping[Seq[FieldTypeExtractor[A]]]]

  inline def repeatedMapping[A](using m: Mirror.ProductOf[A])(nameValueTuple: Tuple.Zip[m.MirroredElemLabels, FieldConstructor[m.MirroredElemTypes]]) =
    new SeqMapping[A](tpe="[Seq", elemField = mapping[A](nameValueTuple)).asInstanceOf[Mapping[Seq[A]]]

  //Collection
  inline def indexedSeq[A] = fieldOf[IndexedSeq[A]]

  inline def list[A] = fieldOf[List[A]]

  inline def list[A](a: Mapping[A]): Mapping[List[A]] = ???

  inline def set[A] = fieldOf[Set[A]]

  inline def vector[A] = fieldOf[Vector[A]]
}
