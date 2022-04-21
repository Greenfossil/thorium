package com.greenfossil.commons.data

class MappingModifierSuite extends munit.FunSuite{

  test("set scalar field name"){
    val f1 = Mapping.of[Int]("f1")
    val f2 = Mapping.of[Int].name("f1")
    assertNoDiff(f1.name, f2.name)
  }

  test("set tuple field name") {
    val tup = tuple(
      "f1" -> Mapping.of[String],
      "f2" -> Mapping.of[Int]
    ).name("tupField")

    assertNoDiff(tup.name, "tupField")
  }

  test("set mapping field name"){
    case class Foo(f1: String, f2: Int)
    val mappingField = mapping[Foo](
      "f1" -> Mapping.of[String],
      "f2" -> Mapping.of[Int]
    ).name("Foo")
    assertNoDiff(mappingField.name, "Foo")
  }

  test("opt field name"){
    val f1 =
      Mapping.of[Option[Int]]
        .name("f1")
        .asInstanceOf[OptionalMapping[Int]]

    assertNoDiff(f1.name, "f1")
    assertNoDiff(f1.elemField.name, "f1")
  }

  test("seq field name"){
    val f1 =
      Mapping.of[Seq[Int]]
        .name("f1")
        .asInstanceOf[SeqMapping[Int]]

    assertNoDiff(f1.name, "f1")
    assertNoDiff(f1.elemField.name, "f1")
  }


}
