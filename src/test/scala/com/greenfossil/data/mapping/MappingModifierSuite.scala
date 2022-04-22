package com.greenfossil.data.mapping

class MappingModifierSuite extends munit.FunSuite{
  import Mapping.*
  
  test("set scalar field name"){
    val f1 = Mapping[Int]("f1")
    val f2 = Mapping.mapTo[Int].name("f1")
    assertNoDiff(f1.name, f2.name)
  }

  test("set tuple field name") {
    val tup = tuple(
      "f1" -> Mapping.mapTo[String],
      "f2" -> Mapping.mapTo[Int]
    ).name("tupField")

    assertNoDiff(tup.name, "tupField")
  }

  test("set mapping field name"){
    case class Foo(f1: String, f2: Int)
    val mappingField = mapping[Foo](
      "f1" -> Mapping.mapTo[String],
      "f2" -> Mapping.mapTo[Int]
    ).name("Foo")
    assertNoDiff(mappingField.name, "Foo")
  }

  test("opt field name"){
    val f1 =
      Mapping.mapTo[Option[Int]]
        .name("f1")
        .asInstanceOf[OptionalMapping[Int]]

    assertNoDiff(f1.name, "f1")
    assertNoDiff(f1.elemField.name, "f1")
  }

  test("seq field name"){
    val f1 =
      Mapping.mapTo[Seq[Int]]
        .name("f1")
        .asInstanceOf[SeqMapping[Int]]

    assertNoDiff(f1.name, "f1")
    assertNoDiff(f1.elemField.name, "f1")
  }


}
