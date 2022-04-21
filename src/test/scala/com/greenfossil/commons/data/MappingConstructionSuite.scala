package com.greenfossil.commons.data

import java.time.*

class MappingConstructionSuite extends munit.FunSuite{

  /**
   * Check Scalar type
   */

  checkScalar("String", Mapping.of[String])
  checkScalar("Int", Mapping.of[Int])
  checkScalar("Long", Mapping.of[Long])
  checkScalar("Double", Mapping.of[Double])
  checkScalar("Float", Mapping.of[Float])
  checkScalar("LocalDateTime", Mapping.of[LocalDateTime])
  checkScalar("LocalDate", Mapping.of[LocalDate])
  checkScalar("LocalTime", Mapping.of[LocalTime])
  checkScalar("YearMonth", Mapping.of[YearMonth])
  checkScalar("SqlTimestamp", Mapping.of[java.sql.Timestamp])
  checkScalar("SqlDate", Mapping.of[java.sql.Date])
  checkScalar("Date", Mapping.of[java.util.Date])
  checkScalar("UUID", Mapping.of[java.util.UUID])
  checkScalar("Byte", Mapping.of[Byte])
  checkScalar("Short", Mapping.of[Short])
  checkScalar("BigDecimal", Mapping.of[BigDecimal])
  checkScalar("Char", Mapping.of[Char])

  /**
   * check tuple
   */
  checkTuple("Tuple2", tuple(
    "f1" -> Mapping.of[String],
    "f2" -> Mapping.of[Int]
  ), "String", "Int")

  /**
   * check mapping
   */

  case class Address(f1: String, f2: Int)
  checkMapping("Address", mapping[Address](
    "f1" -> Mapping.of[String],
    "f2" -> Mapping.of[Int]
  ), "Address", "String", "Int")

  /**
   *  check optional scalar
   */

  checkOptional("Int", optional[Int])

  /**
   * check optional tuple
   */

  val optTuple: Mapping[Option[(String, Int)]] = optionalTuple(
    "f1" -> Mapping.of[String],
    "f2" -> Mapping.of[Int]
  )
  checkOptional("P-", optTuple)

  val tupleElemField = optTuple.asInstanceOf[OptionalMapping[?]].elemField
  checkTuple("", tupleElemField, "String", "Int")

  /**
   * check optional mapping
   */
  val optMapping = optionalMapping[Address](
    "f1" -> Mapping.of[String],
    "f2" -> Mapping.of[Int]
  )
  checkOptional("P+", optMapping)

  val mappingElemField = optMapping.asInstanceOf[OptionalMapping[?]].elemField
  checkMapping("", mappingElemField,"Address", "String", "Int" )

  /**
   * Check Seq of scalar
   */
  checkSeq("Int", seq[Int])
  checkSeq("String", seq[String])

  /**
   * Check Repeated tuple
   */
  val repTuple = repeatedTuple(
    "name" -> Mapping.of[String],
    "number" -> Mapping.of[Int]
  )
  checkSeq("P-",repTuple)
  val repTupElem = repTuple.asInstanceOf[SeqMapping[?]].elemField
  checkTuple("", repTupElem, "String", "Int")

  /**
   * Check Repeated mapping
   */

  val repMapping = repeatedMapping[Address](
    "f1" -> Mapping.of[String],
    "f2" -> Mapping.of[Int]
  )
  checkSeq("P+", repMapping)
  val repMappingElem = repMapping.asInstanceOf[SeqMapping[?]].elemField
  checkMapping("Address", repMappingElem, "Address", "String", "Int")



  //--------------------------

  def checkScalar(name: String, f: Mapping[?])(using munit.Location): Unit = {
    test(name){
      assert(f.isInstanceOf[ScalarMapping[?]], s"Field is not a ScalaField $name")
      assertNoDiff(f.tpe, name)
    }
  }

  def checkTuple(name: String, f: Mapping[?], typeList: String*)(using munit.Location): Unit =  {
    test(s"Tuple[${name}]"){
      assert(f.isInstanceOf[ProductMapping[?]], s"Field is not an TupleField $name")
      val prodF = f.asInstanceOf[ProductMapping[?]]
      assertNoDiff(prodF.tpe, "P-")
      assert(prodF.mirrorOpt.isEmpty)
      val mappings: List[Mapping[?]] = prodF.mappings.toList.asInstanceOf[List[Mapping[?]]]
      assertEquals(mappings.size, typeList.size)
      mappings.zip(typeList) foreach{case (mapping, t) =>
        assertNoDiff(mapping.tpe, t)
      }
    }
  }

  def checkMapping(name: String, f: Mapping[?], className: String, typeList: String*)(using munit.Location): Unit =  {
    test(s"Mapping[${name}]"){
      assert(f.isInstanceOf[ProductMapping[?]], s"Field is not an MappingField $name")
      val prodF = f.asInstanceOf[ProductMapping[?]]
      assert(prodF.tpe.startsWith("P+"))
      assert(prodF.mirrorOpt.isDefined)
      assertNoDiff(prodF.mirrorOpt.get.toString, className)
      val mappings: List[Mapping[?]] = prodF.mappings.toList.asInstanceOf[List[Mapping[?]]]
      assertEquals(mappings.size, typeList.size)
      mappings.zip(typeList) foreach{case (mapping, t) =>
        assertNoDiff(mapping.tpe, t)
      }
    }
  }

  def checkOptional(elemTypeName: String, f: Mapping[?])(using munit.Location): Unit =  {
    test(s"Option[${elemTypeName}]"){
      val optF = f.asInstanceOf[OptionalMapping[?]]
      assertNoDiff(optF.tpe, "?")
      assertNoDiff(optF.elemField.tpe, elemTypeName)
    }
  }

  def checkSeq(elemTypeName: String, f: Mapping[?])(using munit.Location): Unit = {
    test(s"Seq[$elemTypeName]"){
      assert(f.isInstanceOf[SeqMapping[?]], s"Field is not a SeqField $elemTypeName")
      val seqF = f.asInstanceOf[SeqMapping[?]]
      assertNoDiff(seqF.tpe, "[Seq")
      assertNoDiff(seqF.elemField.tpe, elemTypeName)
    }
  }

  def checkRepeatedTuple(elemTypeName: String, f: Mapping[?])(using munit.Location): Unit = {
    test(s"Seq[$elemTypeName]"){
      assert(f.isInstanceOf[SeqMapping[?]], s"Field is not a SeqField $elemTypeName")
      val seqF = f.asInstanceOf[SeqMapping[?]]
      assertNoDiff(seqF.tpe, "[")
      assertNoDiff(seqF.elemField.tpe, elemTypeName)
    }
  }

}
