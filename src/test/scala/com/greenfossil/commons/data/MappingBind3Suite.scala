package com.greenfossil.commons.data

import com.greenfossil.commons.json.Json

/*
 * Test for Option and Seq Field
 */
class MappingBind3Suite extends munit.FunSuite {

  test("Option[Int]") {
    val optIntField: Mapping[Option[Int]] =  optional[Int].name("optInt")
    val boundField = optIntField.bind("optInt" -> "1")
    assertEquals[Any, Any](boundField.value, Option(1))
  }

  test("Option[String]") {
    val optIntField = optional[String].name("optString")
    val boundField = optIntField.bind("optString" -> "Hello World!")
    assertEquals[Any, Any](boundField.value, Option("Hello World!"))
  }

  test("Option[Tuple]") {
    val tupleField: Mapping[Option[(String, Int)]] = optionalTuple(
      "name" -> Mapping.of[String],
      "contact" -> Mapping.of[Int]
    ).name("tupleField")

    val boundField = tupleField.bind("tupleField.name" -> "Hello World!", "tupleField.contact" -> "123")
    assertEquals[Any, Any](boundField.value, Option(("Hello World!", 123)))
  }

  test("Option[Mapping]") {
    case class Contact(name: String, contact: Int)
    val tupleField: Mapping[Option[Contact]] = optionalMapping[Contact](
      "name" -> Mapping.of[String],
      "contact" -> Mapping.of[Int]
    ).name("tupleField")

    val boundField = tupleField.bind("tupleField.name" -> "Hello World!", "tupleField.contact" -> "123")
    assertEquals[Any, Any](boundField.value, Option(Contact("Hello World!", 123)))
  }

  test("2 levels optional mapping with value") {
    case class User(id: Long,  address: Option[Address])
    case class Address(postalCode: String, country: String)

    val nestedOptionalField :Mapping[Option[User]] = optionalMapping[User](
      "id" -> longNumber,
      "address" -> optionalMapping[Address](
        "postalCode" -> text,
        "country" -> text
      )
    ).name("nestedOptionalField")

    val boundField = nestedOptionalField.bind("nestedOptionalField.id" -> "1",
      "nestedOptionalField.address.postalCode" -> "123",
      "nestedOptionalField.address.country" -> "Singapore"
    )
    assertEquals[Any, Any](boundField.value, Option(User(1L, Option(Address("123", "Singapore")))))
  }

  test("2 levels optional mapping without value") {
    case class User(id: Long,  address: Option[Address])
    case class Address(postalCode: String, country: String)

    val nestedOptionalField :Mapping[Option[User]] = optionalMapping[User](
      "id" -> longNumber,
      "address" -> optionalMapping[Address](
        "postalCode" -> text,
        "country" -> text
      )
    ).name("nestedOptionalField")

    val boundField = nestedOptionalField.bind("nestedOptionalField.id" -> "1")
    assertEquals[Any, Any](boundField.value, Option(User(1L, None)))
  }

  test("top level optional mapping, inner level repeatMapping with values") {
    case class User(id: Long,  address: Seq[Address])
    case class Address(postalCode: String, country: String)

    val nestedOptionalField :Mapping[Option[User]] = optionalMapping[User](
      "id" -> longNumber,
      "address" -> repeatedMapping[Address](
        "postalCode" -> text,
        "country" -> text
      )
    ).name("nestedOptionalField")

    val boundField = nestedOptionalField.bind("nestedOptionalField.id" -> "1",
      "nestedOptionalField.address.postalCode" -> "123",
      "nestedOptionalField.address.country" -> "Singapore"
    )
    assertEquals[Any, Any](boundField.value, Option(User(1L, Seq(Address("123", "Singapore")))))
  }

  test("top level optional mapping, inner level repeatMapping without values") {
    case class User(id: Long,  address: Seq[Address])
    case class Address(postalCode: String, country: String)

    val nestedOptionalField :Mapping[Option[User]] = optionalMapping[User](
      "id" -> longNumber,
      "address" -> repeatedMapping[Address](
        "postalCode" -> text,
        "country" -> text
      )
    ).name("nestedOptionalField")

    val boundField = nestedOptionalField.bind("nestedOptionalField.id" -> "1")
    assertEquals[Any, Any](boundField.value, Option(User(1L, Nil)))
  }
  
  
}
