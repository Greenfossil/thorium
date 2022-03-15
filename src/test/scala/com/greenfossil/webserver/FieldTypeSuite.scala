package com.greenfossil.webserver

import com.greenfossil.webserver.data.*

import java.time.{LocalDate, LocalDateTime, LocalTime}

class FieldTypeSuite extends munit.FunSuite {

  test("int"){
    val value = 1
    val field = Field.of[Int]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }

  test("string"){
    val value = "hello world"
    val field = Field.of[String]("field")

    val boundField = field.bind(value)
    val boundField2 = field.bind(Option(value))
    val boundField3 = field.bind(Seq(value))
    val boundField4 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, None)
  }

  test("long"){
    val value = 200000L
    val field = Field.of[Long]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }

  test("double"){
    val value: Double = 200000D
    val field = Field.of[Double]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }

  test("float"){
    val value: Float = 200000.0F
    val field = Field.of[Float]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }

  test("boolean"){
    val value: Boolean = false
    val field = Field.of[Boolean]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)
    val boundField6 = field.bind("hello")

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
    assertEquals(boundField6.value, None)
  }

  test("seq"){
    val value: Seq[Int] = Seq(1,2,3,4,5)
    val field = Field.of[Seq[Int]]("field")

    val boundField = field.bind(value)
//    val boundField2 = field.bind("[1,2,3,4,5]") //TODO check if this is needed
    val boundField3 = field.bind(Option(value))

    assertEquals(boundField.value, Option(value))
//    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
  }

  test("local date") {
    val now = LocalDate.now
    val field = Field.of[LocalDate]("field")
    val boundField = field.bind(now.toString)
    val boundField2 = field.bind(Option(now.toString))
    val boundField3 = field.bind(now)
    val boundField4 = field.bind(Seq(now))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(now))
    assertEquals(boundField2.value, Option(now))
    assertEquals(boundField3.value, Option(now))
    assertEquals(boundField4.value, Option(now))
    assertEquals(boundField5.value, None)
  }

  test("local time"){
    val now = LocalTime.now()
    val field = Field.of[LocalTime]("field")
    val boundField = field.bind(now.toString)
    val boundField2 = field.bind(Option(now.toString))
    val boundField3 = field.bind(now)
    val boundField4 = field.bind(Seq(now))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(now))
    assertEquals(boundField2.value, Option(now))
    assertEquals(boundField3.value, Option(now))
    assertEquals(boundField4.value, Option(now))
    assertEquals(boundField5.value, None)
  }

  test("local date time"){
    val now = LocalDateTime.now()
    val field = Field.of[LocalDateTime]("field")
    val boundField = field.bind(now.toString)
    val boundField2 = field.bind(Option(now.toString))
    val boundField3 = field.bind(now)
    val boundField4 = field.bind(Seq(now))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(now))
    assertEquals(boundField2.value, Option(now))
    assertEquals(boundField3.value, Option(now))
    assertEquals(boundField4.value, Option(now))
    assertEquals(boundField5.value, None)
  }

  test("sql date"){
    val now: java.sql.Date = java.sql.Date.valueOf("2022-02-02")
    val field = Field.of[java.sql.Date]("field")
    val boundField = field.bind(now.toString)
    val boundField2 = field.bind(Option(now.toString))
    val boundField3 = field.bind(now)
    val boundField4 = field.bind(Seq(now))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(now))
    assertEquals(boundField2.value, Option(now))
    assertEquals(boundField3.value, Option(now))
    assertEquals(boundField4.value, Option(now))
    assertEquals(boundField5.value, None)
  }

  test("sql time stamp"){
    val milli: Long = System.currentTimeMillis();
    val now: java.sql.Timestamp = new java.sql.Timestamp(milli)
    
    val field = Field.of[java.sql.Timestamp]("field")
    val boundField = field.bind(now.toString)
    val boundField2 = field.bind(Option(now.toString))
    val boundField3 = field.bind(now)
    val boundField4 = field.bind(Seq(now))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(now))
    assertEquals(boundField2.value, Option(now))
    assertEquals(boundField3.value, Option(now))
    assertEquals(boundField4.value, Option(now))
    assertEquals(boundField5.value, None)
  }

  test("uuid"){
    val uuid = java.util.UUID.randomUUID()

    val field = Field.of[java.util.UUID]("field")
    val boundField = field.bind(uuid.toString)
    val boundField2 = field.bind(Option(uuid.toString))
    val boundField3 = field.bind(uuid)
    val boundField4 = field.bind(Seq(uuid))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(uuid))
    assertEquals(boundField2.value, Option(uuid))
    assertEquals(boundField3.value, Option(uuid))
    assertEquals(boundField4.value, Option(uuid))
    assertEquals(boundField5.value, None)
  }

  test("byte"){
    val value: Byte = 123.toByte
    val field = Field.of[Byte]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }

  test("short"){
    val value: Short = 1
    val field = Field.of[Short]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }

  test("big decimal"){
    val value: BigDecimal = BigDecimal(200000)
    val field = Field.of[BigDecimal]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }

  test("char"){
    val value: Char = 'a'

    val field = Field.of[Char]("field")

    val boundField = field.bind(value.toString)
    val boundField2 = field.bind(Option(value.toString))
    val boundField3 = field.bind(value)
    val boundField4 = field.bind(Seq(value))
    val boundField5 = field.bind(None)

    assertEquals(boundField.value, Option(value))
    assertEquals(boundField2.value, Option(value))
    assertEquals(boundField3.value, Option(value))
    assertEquals(boundField4.value, Option(value))
    assertEquals(boundField5.value, None)
  }


  test("ignored type"){
    val field = ignored[Long](0L)

    val boundField = field.bind(2L)
    assertEquals(boundField.value, Option(0L))
  }

  test("default type"){
    val field = default(text, "Foo")

    assertEquals(field.value, Option("Foo"))

    val boundField = field.bind("Bar")
    assertEquals(boundField.value, Option("Bar"))

    val boundField2 = field.bind(null)
    assertEquals(boundField2.value, Option("Foo"))
  }

  test("checked type"){
    val field = checked("Please check this field")

    assertEquals(field.bind("true").value, Option(true))
    assertEquals(field.bind("true").errors.size, 0)
    assertEquals(field.bind("false").errors.size, 1)
    assertEquals(field.bind("test").errors.size, 1)
    assertEquals(field.bind("").errors.size, 1)
  }

}
