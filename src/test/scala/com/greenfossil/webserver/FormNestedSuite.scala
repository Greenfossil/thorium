//package com.greenfossil.webserver
//
//class FormNestedSuite extends munit.FunSuite {
//
//  import com.greenfossil.webserver.data.{*, given}
//
//  test("Nested Form") {
//    case class AddressData(street: String, city: String)
//    case class UserAddressData(name: String, address: AddressData)
//
//    val addrForm = Form.mapping[AddressData](
//      "street" -> text,
//      "city"   -> text
//    )
//
//    val userFormNested: Form[UserAddressData] = Form.mapping[UserAddressData](
//      "name" -> text,
//      "address" -> Form.mapping[AddressData](
//        "street" -> text,
//        "city"   -> text
//      )
//    )
//    
//    println("")
//  }
//
//}
