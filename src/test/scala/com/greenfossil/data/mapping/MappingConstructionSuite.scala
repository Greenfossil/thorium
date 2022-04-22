package com.greenfossil.data.mapping

import java.time.*

class MappingConstructionSuite extends munit.FunSuite{
  import Mapping.*
  
  /**
   * Check Scalar type
   */

  checkScalar("String", Mapping.mapTo[String])
  checkScalar("Int", Mapping.mapTo[Int])
  checkScalar("Long", Mapping.mapTo[Long])
  checkScalar("Double", Mapping.mapTo[Double])
  checkScalar("Float", Mapping.mapTo[Float])
  checkScalar("LocalDateTime", Mapping.mapTo[LocalDateTime])
  checkScalar("LocalDate", Mapping.mapTo[LocalDate])
  checkScalar("LocalTime", Mapping.mapTo[LocalTime])
  checkScalar("YearMonth", Mapping.mapTo[YearMonth])
  checkScalar("SqlTimestamp", Mapping.mapTo[java.sql.Timestamp])
  checkScalar("SqlDate", Mapping.mapTo[java.sql.Date])
  checkScalar("Date", Mapping.mapTo[java.util.Date])
  checkScalar("UUID", Mapping.mapTo[java.util.UUID])
  checkScalar("Byte", Mapping.mapTo[Byte])
  checkScalar("Short", Mapping.mapTo[Short])
  checkScalar("BigDecimal", Mapping.mapTo[BigDecimal])
  checkScalar("Char", Mapping.mapTo[Char])

  /**
   * check tuple
   */
  checkTuple("Tuple2", tuple(
    "f1" -> Mapping.mapTo[String],
    "f2" -> Mapping.mapTo[Int]
  ), "String", "Int")

  /**
   * check mapping
   */

  case class Address(f1: String, f2: Int)
  checkMapping("Address", mapping[Address](
    "f1" -> Mapping.mapTo[String],
    "f2" -> Mapping.mapTo[Int]
  ), "Address", "String", "Int")

  /**
   *  check optional scalar
   */

  checkOptional("Int", optional[Int])

  /**
   * check optional tuple
   */

  val optTuple: Mapping[Option[(String, Int)]] = optionalTuple(
    "f1" -> Mapping.mapTo[String],
    "f2" -> Mapping.mapTo[Int]
  )
  checkOptional("P-", optTuple)

  val tupleElemField = optTuple.asInstanceOf[OptionalMapping[?]].elemField
  checkTuple("", tupleElemField, "String", "Int")

  /**
   * check optional mapping
   */
  val optMapping = optionalMapping[Address](
    "f1" -> Mapping.mapTo[String],
    "f2" -> Mapping.mapTo[Int]
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
    "name" -> Mapping.mapTo[String],
    "number" -> Mapping.mapTo[Int]
  )
  checkSeq("P-",repTuple)
  val repTupElem = repTuple.asInstanceOf[SeqMapping[?]].elemField
  checkTuple("", repTupElem, "String", "Int")

  /**
   * Check Repeated mapping
   */

  val repMapping = repeatedMapping[Address](
    "f1" -> Mapping.mapTo[String],
    "f2" -> Mapping.mapTo[Int]
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
