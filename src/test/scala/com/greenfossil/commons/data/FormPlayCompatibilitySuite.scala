package com.greenfossil.commons.data

class FormPlayCompatibilitySuite extends munit.FunSuite {

  /*
   * Html form-url-encoded
   */
  test("valid default value") {
    val form: Field[(String, Boolean)] = tuple(
      "text" -> default(text, "Foo"),
      "isChecked" -> checked("this should be checked")
    )

    val filledForm = form.bind(
      "text" -> "chicken",
      "isChecked" -> "true"
    )

    filledForm.fold(
      errorForm => fail("should not have errors"),
      data => {
        println(s"data = ${data}")
        assertEquals(data, ("chicken", true))
      }
    )
  }

  test("empty default value") {
    val form: Field[(String, Boolean)] = tuple(
      "text" -> default(text, "Foo"),
      "isChecked" -> checked("this should be checked")
    )

    val filledForm = form.bind("isChecked" -> "true")
    filledForm.fold(
      errorForm => fail("should not have errors"),
      data => {
        println(s"data = ${data}")
        assertEquals(data, ("Foo", true))
      }
    )
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[2]=2' -d 'seq[3]=3' -o /dev/null
  */
  test("repeated with valid index []") {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[2]" -> "2",
      "seq[3]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[1]=2' -d 'seq[1]=3' -o /dev/null
  */
  test("repeated with same index []".fail) { //Will not be compatible
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[1]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'seq[3]=1' -d 'seq[2]=2' -d 'seq[1]=3' -o /dev/null
   */
  test("repeated with inverted index []") {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[3]" -> "1",
      "seq[2]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(3, 2, 1)))
  }
  /*
   * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[2]=2' -d 'seq[4]=3' -o /dev/null
   */
  test("repeated with gap in index []") {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[2]" -> "2",
      "seq[4]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }
  /*
  * curl http://localhost:9000/form -X POST -d 'seq[]=1' -d 'seq[]=2' -d 'seq[]=3' -o /dev/null
  */


  test("repeated with no []") {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[]" -> "1",
      "seq[]" -> "2",
      "seq" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }
  

  test("repeated with 0 as an index []") {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[0]" -> "1",
      "seq[1]" -> "2",
      "seq[2]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[-1]=1' -d 'seq[0]=2' -d 'seq[1]=3' -o /dev/null
  */
  test("repeated with negative value as an index []") {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[-1]" -> "1",
      "seq[0]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(2, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[3]=3' -o /dev/null
   */
  test("repeated with 1 empty index []") {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[3]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(2, 1, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[]=3' -o /dev/null
   */
  test("repeated with 2 empty index [] ".fail) {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(2, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[]=2' -d 'seq[2]=3' -o /dev/null
  */
  test("repeated with same value with empty index [] ".fail) {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
        "seq[1]" -> "1",
        "seq[]" -> "2",
        "seq[]" -> "2",
        "seq[2]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(2, 1, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[]=5' -d 'seq[2]=3' -o /dev/null
  */
  test("repeated with different value with empty index [] ".fail) {
    val form: Field[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
        "seq[1]" -> "1",
        "seq[]" -> "2",
        "seq[]" -> "5",
        "seq[2]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(2, 5, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'id=1' -d 'address.postalCode="123456"' -d 'address.country="Singapore"' -o /dev/null
   */
  test("nested field") {
    val form: Field[(Long, (String, String))] = tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    val boundForm = form.bind(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore"
    )
    assertEquals(boundForm.value, Some((1L, ("123456", "Singapore"))))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'id=1' -d 'address.postalCode="123456"' -d 'address.country="Singapore"' -d 'address.numList.num=1' -d 'address.numList.num2=2' -o /dev/null
   */
  test("double nested tuple fields") {
    val form: Field[(Long, (String, String, (Long, Long, (String, String))))] = tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text,
        "numList" -> tuple(
          "num" -> longNumber,
          "num2" -> longNumber,
          "member" -> tuple(
            "name1" -> text,
            "name2" -> text
          )
        )
      )
    )

    val boundForm = form.bind(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore",
      "address.numList.num" -> "1",
      "address.numList.num2" -> "2",
      "address.numList.member.name1" -> "John",
      "address.numList.member.name2" -> "Doe",
    )
    assertEquals(boundForm.value, Some((1L, ("123456", "Singapore", (1L, 2L, ("John", "Doe"))))))
  }

  test("bound ignored"){
    val form = ignored("Foo").name("s")
    val boundForm = form.bind("s" -> "Bar")
    assertEquals(boundForm.value, Some("Foo"))
  }

  test("filled ignored"){
    val form = ignored("Foo").name("s")
    val boundForm = form.fill("Bar")
    assertEquals(boundForm.value, Some("Foo"))
  }
}
