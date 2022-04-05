package com.greenfossil.commons.data

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}

class FieldBind1Suite extends munit.FunSuite {

  test("int"){
    val field = Field.of[Int]("field")
    val boundField = field.bind(Map("field" -> "1"))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Option(1))
  }

  test("string"){
    val field = Field.of[String]("field")
    val boundField = field.bind(Map("field" -> "hello world"))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Option("hello world"))
  }

  test("long"){
    val field = Field.of[Long]("field")
    val boundField = field.bind(Map("field" -> "200000"))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Option(200000L))
  }

  test("double"){
    val field = Field.of[Double]("field")
    val boundField = field.bind(Map("field" -> "200000"))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Option(200000D))
  }

  test("float"){
    val field = Field.of[Float]("field")
    val boundField = field.bind(Map("field" -> "200000"))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Option(200000.0F))
  }

  test("boolean"){
    val value: Boolean = false
    val field = Field.of[Boolean]("field")
    val boundField = field.bind(Map("field" -> "false"))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Option(false))

    val boundField2 = field.bind(Map("field" -> "hello"))
    assertEquals(boundField2.errors.head.messages.head, "error.boolean")
    assertEquals(boundField2.value, None)
  }

  test("local date") {
    val now = LocalDate.now
    val field = Field.of[LocalDate]("field")
    val boundField = field.bind(Map("field" -> now.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(now))
  }

  test("local time"){
    val now = LocalTime.now()
    val field = Field.of[LocalTime]("field")
    val boundField = field.bind(Map("field" -> now.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(now))

    //time without millis
    val nowInSecs = now.truncatedTo(ChronoUnit.SECONDS)
    assertNotEquals(now, nowInSecs, "nowInSecs should not have millis")

    val field2 = Field.of[LocalTime]("field")
    val boundField2 = field2.bind(Map("field" -> nowInSecs.toString))
    assertEquals(boundField2.errors, Nil)
    assertEquals(boundField2.value, Some(nowInSecs))

  }

  test("local date time"){
    //By default uses ISO_LOCAL_DATE_TIME -
    // https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE_TIME
    val now = LocalDateTime.now()
    val field = Field.of[LocalDateTime]("field")
    val boundField = field.bind(Map("field" -> now.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(now))

    val nowInSecs = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    assertNotEquals(now, nowInSecs, "nowInSecs should not have millis")

    val field2 = Field.of[LocalDateTime]("field")
    val boundField2 = field.bind(Map("field" -> nowInSecs.toString))
    assertEquals(boundField2.errors, Nil)
    assertEquals(boundField2.value, Some(nowInSecs))
  }

  test("sql date"){
    val now: java.sql.Date = java.sql.Date.valueOf("2022-02-02")
    val field = Field.of[java.sql.Date]("field")
    val boundField = field.bind(Map("field" -> now.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(now))
  }

  test("sql time stamp"){
    val milli: Long = System.currentTimeMillis()
    val now: java.sql.Timestamp = new java.sql.Timestamp(milli)

    val field = Field.of[java.sql.Timestamp]("field")
    val boundField = field.bind(Map("field" -> now.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(now))
  }

  test("uuid"){
    val uuid = java.util.UUID.randomUUID()

    val field = Field.of[java.util.UUID]("field")
    val boundField = field.bind(Map("field" -> uuid.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(uuid))
  }

  test("byte"){
    val value: Byte = 123.toByte
    val field = Field.of[Byte]("field")
    val boundField = field.bind(Map("field" -> value.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(value))
  }

  test("short"){
    val value: Short = 1
    val field = Field.of[Short]("field")
    val boundField = field.bind(Map("field" -> value.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(value))
  }

  test("big decimal"){
    val value: BigDecimal = BigDecimal(200000)
    val field = Field.of[BigDecimal]("field")
    val boundField = field.bind(Map("field" -> value.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(value))
  }

  test("char"){
    val value: Char = 'a'

    val field = Field.of[Char]("field")
    val boundField = field.bind(Map("field" -> value.toString))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(value))
  }


  test("ignored"){

    val field =
      ignored[Long](0L)
        .name("field") //Assign a name to the ignore field

    val boundField = field.bind(Map("field" -> "2"))
    assertEquals(boundField.value, Option(0L))
  }

  test("default type"){
    val field  =
      default[String](text, "Foo")
        .name("field") // Assigned a name to the default field

    assertEquals(field.value, Option("Foo"))

    val boundField = field.bind(Map("field" -> "Bar"))
    assertEquals(boundField.value, Option("Bar"))

    val boundField2 = field.bind(Map())
    assertEquals(boundField2.value, Option("Foo"))

    //Default is where field does not exists
    val boundField3 = field.bind(Map("field" -> ""))
    assertEquals(boundField3.value, Option(""))
  }

  test("checked type"){
    val field: Field[Boolean] =
      checked("Please check this field")
        .name("field") //Assign name to field

    assertEquals(field.bind(Map("field" -> "true")).value, Option(true))
    assertEquals(field.bind(Map("field" -> "true")).errors.size, 0)
    assertEquals(field.bind(Map("field" -> "false")).errors.size, 1)
    assertEquals(field.bind(Map("field" -> "test")).errors.size, 1)
    assertEquals(field.bind(Map("field" -> "")).errors.size, 1)  //Discuss with Kate for setting value to "" -  Field.toValueOf[A]
  }

  test("year month type"){
    val data = YearMonth.now()
    val pattern = "yy-MM"
    val field = yearMonth(pattern).name("field")

    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
    val boundField = field.bind(Map("field" -> formatter.format(data)))
    assertEquals(boundField.value, Some(data))
    assertEquals(boundField.errors, Nil)

    val field2 = yearMonth.name("field")
    val boundField2 = field2.bind(Map("field" -> data.toString))
    assertEquals(boundField2.value, Some(data))
    assertEquals(boundField2.errors, Nil)

  }

  test("LocalDate with pattern field"){
    val now = LocalDate.now()
    val format = "MMMM d, yyyy"
    val nowValue = now.format(DateTimeFormatter.ofPattern(format))
    val field =
      localDate(format)
        .name("field")

    val boundField = field.bind(Map("field"-> nowValue))
    assertEquals(boundField.errors, Nil)
    assertEquals(boundField.value, Some(now))
  }

}
