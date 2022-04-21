package com.greenfossil.commons.data

import com.greenfossil.commons.json.Json

class FormNestedFieldsSuite extends munit.FunSuite {

  test("bind case-class field") {
    case class Address(postalCode: String, country: String)
    val form: Mapping[(Long, Address)] = tuple(
      "id" -> longNumber,
      "address" -> mapping[Address](
          "postalCode" -> text,
          "country" -> text
      )
    )

    assertNoDiff(form.field("id").tpe, "Long")
    assertNoDiff(form.field("address").tpe, "P+")

    val boundForm = form.bind(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore"
    )

    assertEquals(boundForm.value, Some((1L, Address("123456", "Singapore"))))

  }

  test("bind repeating case class field") {
    case class Address(postalCode: String, country: String)
    val form: Mapping[(Long, Seq[Address])] = tuple(
      "id" -> longNumber,
      "address" -> repeatedMapping[Address](
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.field("id").tpe, "Long")
    assertNoDiff(form.field("address").tpe, "[Seq")

    val boundForm = form.bind(
      "id" -> "1",
      "address[0].postalCode" -> "123456",
      "address[0].country" -> "Singapore"
    )

    assertEquals(boundForm.value, Some((1L, Seq(Address("123456", "Singapore")))))

  }

  test("bind tuple field") {
    val form: Mapping[(Long, (String, String))] = tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.field("id").tpe, "Long")
    assertNoDiff(form.field("address").tpe, "P-")

    val boundForm = form.bind(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore"
    )

    assertEquals(boundForm.value, Some((1L, ("123456", "Singapore"))))

  }

  test("bind repeat tuple field") {
    val form: Mapping[(Long, Seq[(String, String)])] = tuple(
      "id" -> longNumber,
      "address" -> repeatedTuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.field("id").tpe, "Long")
    assertNoDiff(form.field("address").tpe, "[Seq")

    val boundForm = form.bind(
      "id" -> "1",
      "address[0].postalCode" -> "123456",
      "address[0].country" -> "Singapore"
    )

    assertEquals(boundForm.value, Some((1L, Seq(("123456", "Singapore")))))

  }

  test("Json optional nested tuple fields") {
    val form: Mapping[(Long, (String, String, Option[(Long, Long)]))] = tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text,
        "numList" -> optionalTuple(
          "num1" -> longNumber,
          "num2" -> longNumber,
        )
      )
    )

    val jsonObject = Json.obj(
      "id" -> "1",
      "address" -> Json.obj(
        "postalCode" -> "123456",
        "country" -> "Singapore",
        "numList" -> Json.obj(
          "num1" -> 1,
          "num2" -> 2)
      )
    )

    val boundForm = form.bind(jsonObject)
    assertEquals(boundForm.value, Some((1L, ("123456", "Singapore", Some((1L, 2L))))))
  }

  test("Json optional nested tuple fields with no values") {
    val form: Mapping[(Long, (String, String, Option[(Long, Long)]))] = tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text,
        "numList" -> optionalTuple(
          "num1" -> longNumber,
          "num2" -> longNumber,
        )
      )
    )

    val jsonObject = Json.obj(
      "id" -> "1",
      "address" -> Json.obj(
        "postalCode" -> "123456",
        "country" -> "Singapore"
      )
    )

    val boundForm = form.bind(jsonObject)
    assertEquals(boundForm.value, Some((1L, ("123456", "Singapore", None))))
  }

  test("Json optional nested mapping fields with values") {

    case class User(id: Long, address: Address)
    case class Address(postalCode: String, country: String, numList: Option[(Long, Long)])

    val form: Mapping[User] = mapping[User](
      "id" -> longNumber,
      "address" -> mapping[Address](
        "postalCode" -> text,
        "country" -> text,
        "numList" -> optionalTuple(
          "num1" -> longNumber,
          "num2" -> longNumber,
        )
      )
    )

    val jsonObject = Json.obj(
      "id" -> "1",
      "address" -> Json.obj(
        "postalCode" -> "123456",
        "country" -> "Singapore",
        "numList" -> Json.obj(
          "num1" -> 1,
          "num2" -> 2)
      )
    )

    val boundForm = form.bind(jsonObject)
    assertEquals(boundForm.value, Some(User(1L, Address("123456", "Singapore", Some((1L, 2L))))))
  }

  test("Json optional nested mapping fields with no values") {
    case class User(id: Long, address: Address)
    case class Address(postalCode: String, country: String, numList: Option[(Long, Long)])

    val form: Mapping[User] = mapping[User](
      "id" -> longNumber,
      "address" -> mapping[Address](
        "postalCode" -> text,
        "country" -> text,
        "numList" -> optionalTuple(
          "num1" -> longNumber,
          "num2" -> longNumber,
        )
      )
    )

    val jsonObject = Json.obj(
      "id" -> "1",
      "address" -> Json.obj(
        "postalCode" -> "123456",
        "country" -> "Singapore"
      )
    )

    val boundForm = form.bind(jsonObject)
    assertEquals(boundForm.value, Some(User(1L, Address("123456", "Singapore", None))))
  }
}
