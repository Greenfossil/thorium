package com.greenfossil.commons.data

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{LocalDate, LocalDateTime, LocalTime, YearMonth}

class FieldFillSuite extends munit.FunSuite {

  test("int"){
    val field = Field.of[Int]("field")
    val filledField = field.fill(1)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Option(1))
  }

  test("string"){
    val field = Field.of[String]("field")
    val filledField = field.fill("hello world")
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Option("hello world"))
  }

  test("long"){
    val field = Field.of[Long]("field")
    val filledField = field.fill(200000)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Option(200000L))
  }

  test("double"){
    val field = Field.of[Double]("field")
    val filledField = field.fill(200000.0)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Option(200000D))
  }

  test("float"){
    val field = Field.of[Float]("field")
    val filledField = field.fill(200000)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Option(200000.0F))
  }

  test("boolean"){
    val value: Boolean = false
    val field = Field.of[Boolean]("field")
    val filledField = field.fill(false)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Option(false))
  }

  test("local date") {
    val now = LocalDate.now
    val field = Field.of[LocalDate]("field")
    val filledField = field.fill(now)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(now))
  }

  test("local time"){
    val now = LocalTime.now()
    val field = Field.of[LocalTime]("field")
    val filledField = field.fill(now)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(now))

    //time without millis
    val nowInSecs = now.truncatedTo(ChronoUnit.SECONDS)
    assertNotEquals(now, nowInSecs, "nowInSecs should not have millis")

    val field2 = Field.of[LocalTime]("field")
    val filledField2 = field2.fill(nowInSecs)
    assertEquals(filledField2.errors, Nil)
    assertEquals(filledField2.value, Some(nowInSecs))

  }

  test("local date time"){
    //By default uses ISO_LOCAL_DATE_TIME -
    // https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE_TIME
    val now = LocalDateTime.now()
    val field = Field.of[LocalDateTime]("field")
    val filledField = field.fill(now)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(now))

    val nowInSecs = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    assertNotEquals(now, nowInSecs, "nowInSecs should not have millis")

    val field2 = Field.of[LocalDateTime]("field")
    val filledField2 = field.fill(nowInSecs)
    assertEquals(filledField2.errors, Nil)
    assertEquals(filledField2.value, Some(nowInSecs))
  }

  test("sql date"){
    val now: java.sql.Date = java.sql.Date.valueOf("2022-02-02")
    val field = Field.of[java.sql.Date]("field")
    val filledField = field.fill(now)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(now))
  }

  test("sql time stamp"){
    val milli: Long = System.currentTimeMillis()
    val now: java.sql.Timestamp = new java.sql.Timestamp(milli)

    val field = Field.of[java.sql.Timestamp]("field")
    val filledField = field.fill(now)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(now))
  }

  test("uuid"){
    val uuid = java.util.UUID.randomUUID()

    val field = Field.of[java.util.UUID]("field")
    val filledField = field.fill(uuid)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(uuid))
  }

  test("byte"){
    val value: Byte = 123.toByte
    val field = Field.of[Byte]("field")
    val filledField = field.fill(value)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(value))
  }

  test("short"){
    val value: Short = 1
    val field = Field.of[Short]("field")
    val filledField = field.fill(value)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(value))
  }

  test("big decimal"){
    val value: BigDecimal = BigDecimal(200000)
    val field = Field.of[BigDecimal]("field")
    val filledField = field.fill(value)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(value))
  }

  test("char"){
    val value: Char = 'a'

    val field = Field.of[Char]("field")
    val filledField = field.fill(value)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(value))
  }


  test("ignored"){

    val field =
      ignored[Long](0L)
        .name("field") //Assign a name to the ignore field

    val filledField = field.fill(2)
    assertEquals(filledField.value, Option(0L))
  }

  test("default type"){
    val field  =
      default[String](text, "Foo")
        .name("field") // Assigned a name to the default field

    assertEquals(field.value, Option("Foo"))

    val filledField = field.fill("Bar")
    assertEquals(filledField.value, Option("Bar"))

    val filledField2 = field.fill(None)
    assertEquals(filledField2.value, Option("Foo"))

  }

  test("checked type"){
    val field: Field[Boolean] =
      checked("Please check this field")
        .name("field") //Assign name to field

    assertEquals(field.fill(true).value, Option(true))
    assertEquals(field.fill(true).errors.size, 0)
    assertEquals(field.fill(false).errors.size, 1)
  }

  test("year month type"){
    val data = YearMonth.now()
    val pattern = "yy-MM"
    val field = yearMonth(pattern).name("field")

    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
    val filledField = field.fill(data)
    assertEquals(filledField.value, Some(data))
    assertEquals(filledField.errors, Nil)

  }

  test("LocalDate with pattern field"){
    val now = LocalDate.now()
    val format = "MMMM d, yyyy"
    val nowValue = now.format(DateTimeFormatter.ofPattern(format))
    val field =
      localDate(format)
        .name("field")

    val filledField = field.fill(now)
    assertEquals(filledField.errors, Nil)
    assertEquals(filledField.value, Some(now))
  }

  test("seq"){
    val value = Seq(1,2,3,4,5)
    val field = Field.of[Seq[Int]]("field")

    val filledField = field.fill(value)
    assertEquals(filledField.value, Option(value))

    val filledField3 = field.fill(Option(value))
    assertEquals(filledField3.value, Option(value))
  }


}
