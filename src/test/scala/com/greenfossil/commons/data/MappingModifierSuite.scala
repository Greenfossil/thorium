package com.greenfossil.commons.data

class MappingModifierSuite extends munit.FunSuite{
  import Mapping.*
  
  test("set scalar field name"){
    val f1 = Mapping[Int]("f1")
    val f2 = Mapping.fieldOf[Int].name("f1")
    assertNoDiff(f1.name, f2.name)
  }

  test("set tuple field name") {
    val tup = tuple(
      "f1" -> Mapping.fieldOf[String],
      "f2" -> Mapping.fieldOf[Int]
    ).name("tupField")

    assertNoDiff(tup.name, "tupField")
  }

  test("set mapping field name"){
    case class Foo(f1: String, f2: Int)
    val mappingField = mapping[Foo](
      "f1" -> Mapping.fieldOf[String],
      "f2" -> Mapping.fieldOf[Int]
    ).name("Foo")
    assertNoDiff(mappingField.name, "Foo")
  }

  test("opt field name"){
    val f1 =
      Mapping.fieldOf[Option[Int]]
        .name("f1")
        .asInstanceOf[OptionalMapping[Int]]

    assertNoDiff(f1.name, "f1")
    assertNoDiff(f1.elemField.name, "f1")
  }

  test("seq field name"){
    val f1 =
      Mapping.fieldOf[Seq[Int]]
        .name("f1")
        .asInstanceOf[SeqMapping[Int]]

    assertNoDiff(f1.name, "f1")
    assertNoDiff(f1.elemField.name, "f1")
  }


}
