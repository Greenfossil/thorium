package com.greenfossil.commons.data

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}

class MappingBind1Suite extends munit.FunSuite {
  import com.greenfossil.commons.data.Mapping
  import Mapping.*

  test("int"){
    val field = Mapping[Int]("field")
    val boundField = field.bind("field" -> "1")
    assertEquals(boundField.errors, Nil)
    assertEquals[Any, Any](boundField.value, Option(1))
  }

  test("string"){
    val field = Mapping[String]("field")
    val boundField = field.bind("field" -> "hello world")
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Option("hello world"))
  }

  test("long"){
    val field = Mapping[Long]("field")
    val boundField = field.bind("field" -> "200000")
    assertEquals(boundField.errors, Nil)
    assertEquals[Any, Any](boundField.value, Option(200000L))
  }

  test("double"){
    val field = Mapping[Double]("field")
    val boundField = field.bind("field" -> "200000")
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Option(200000D))
  }

  test("float"){
    val field = Mapping[Float]("field")
    val boundField = field.bind("field" -> "200000")
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Option(200000.0F))
  }

  test("boolean"){
    val value: Boolean = false
    val field = Mapping[Boolean]("field")
    val boundField = field.bind("field" -> "false")
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Option(false))

    val boundField2 = field.bind("field" -> "hello")
    assertEquals(boundField2.errors.head.messages.head, "error.boolean")
    assertEquals(boundField2.value, None)
  }

  test("local date") {
    val now = LocalDate.now
    val field = Mapping[LocalDate]("field")
    val boundField = field.bind("field" -> now.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(now))
  }

  test("local time"){
    val now = LocalTime.now()
    val field = Mapping[LocalTime]("field")
    val boundField = field.bind("field" -> now.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(now))

    //time without millis
    val nowInSecs = now.truncatedTo(ChronoUnit.SECONDS)
    assertNotEquals(now, nowInSecs, "nowInSecs should not have millis")

    val field2 = Mapping[LocalTime]("field")
    val boundField2 = field2.bind("field" -> nowInSecs.toString)
    assertEquals(boundField2.errors, Nil)
    assertEquals[Any,Any](boundField2.value, Some(nowInSecs))

  }

  test("local date time"){
    //By default uses ISO_LOCAL_DATE_TIME -
    // https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE_TIME
    val now = LocalDateTime.now()
    val field = Mapping[LocalDateTime]("field")
    val boundField = field.bind("field" -> now.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(now))

    val nowInSecs = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    assertNotEquals(now, nowInSecs, "nowInSecs should not have millis")

    val field2 = Mapping[LocalDateTime]("field")
    val boundField2 = field.bind("field" -> nowInSecs.toString)
    assertEquals(boundField2.errors, Nil)
    assertEquals[Any,Any](boundField2.value, Some(nowInSecs))
  }

  test("sql date"){
    val now: java.sql.Date = java.sql.Date.valueOf("2022-02-02")
    val field = Mapping[java.sql.Date]("field")
    val boundField = field.bind("field" -> now.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(now))
  }

  test("sql time stamp"){
    val milli: Long = System.currentTimeMillis()
    val now: java.sql.Timestamp = new java.sql.Timestamp(milli)

    val field = Mapping[java.sql.Timestamp]("field")
    val boundField = field.bind("field" -> now.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(now))
  }

  test("uuid"){
    val uuid = java.util.UUID.randomUUID()

    val field = Mapping[java.util.UUID]("field")
    val boundField = field.bind("field" -> uuid.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(uuid))
  }

  test("byte"){
    val value: Byte = 123.toByte
    val field = Mapping[Byte]("field")
    val boundField = field.bind("field" -> value.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(value))
  }

  test("short"){
    val value: Short = 1
    val field = Mapping[Short]("field")
    val boundField = field.bind("field" -> value.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(value))
  }

  test("big decimal"){
    val value: BigDecimal = BigDecimal(200000)
    val field = Mapping[BigDecimal]("field")
    val boundField = field.bind("field" -> value.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(value))
  }

  test("char"){
    val value: Char = 'a'

    val field = Mapping[Char]("field")
    val boundField = field.bind("field" -> value.toString)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any,Any](boundField.value, Some(value))
  }


  test("ignored".only){

    val field =
      ignored[Long](0L)
        .name("field") //Assign a name to the ignore field

    val boundField = field.bind("field" -> "2")
    assertEquals[Any,Any](boundField.value, Option(0L))
  }

  test("default type"){
    val field  =
      default[String](text, "Foo")
        .name("field") // Assigned a name to the default field

    assertEquals(field.value, Option("Foo"))

    val boundField = field.bind("field" -> "Bar")
    assertEquals(boundField.value, Option("Bar"))

    //Default is where field does not exists
    val boundField3 = field.bind("field" -> "")
    assertEquals[Any,Any](boundField3.value, Option(""))
  }

  test("checked type"){
    val field: Mapping[Boolean] =
      checked("Please check this field")
        .name("field") //Assign name to field

    assertEquals(field.bind("field" -> "true").value, Option(true))
    assertEquals(field.bind("field" -> "true").errors.size, 0)
    assertEquals(field.bind("field" -> "false").errors.size, 1)
    assertEquals(field.bind("field" -> "test").errors.size, 1)
    assertEquals(field.bind("field" -> "").errors.size, 1)  //Discuss with Kate for setting value to "" -  Field.toValueOf[A]
  }

  test("year month type"){
    val data = YearMonth.now()
    val pattern = "yy-MM"
    val field: Mapping[YearMonth] = Mapping("field", yearMonth(pattern))

    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
    val boundField = field.bind("field" -> formatter.format(data))
    assertEquals(boundField.value, Some(data))
    assertEquals(boundField.errors, Nil)

    val field2 = yearMonth.name("field")
    val boundField2 = field2.bind("field" -> data.toString)
    assertEquals(boundField2.value, Some(data))
    assertEquals(boundField2.errors, Nil)

  }

  test("LocalDate with pattern field"){
    val now = LocalDate.now()
    val format = "MMMM d, yyyy"
    val nowValue = now.format(DateTimeFormatter.ofPattern(format))
    val field = Mapping("field", localDate(format))

    val boundField = field.bind("field"-> nowValue)
    assertEquals(boundField.errors, Nil)
    assertEquals[Any, Any](boundField.value, Some(now))
  }

  test("Bind error"){
    val field = Mapping[Int]("field")
    val boundField = field.bind("field" -> "abc")
    assertEquals(boundField.errors.head.message, "error.number")
    assertEquals(boundField.value, None)
  }

}
