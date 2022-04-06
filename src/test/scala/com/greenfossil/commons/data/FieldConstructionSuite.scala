package com.greenfossil.commons.data

import java.time.*

class FieldConstructionSuite extends munit.FunSuite{

  /**
   * Check Scalar type
   */

  checkScalar("String", Field.of[String])
  checkScalar("Int", Field.of[Int])
  checkScalar("Long", Field.of[Long])
  checkScalar("Double", Field.of[Double])
  checkScalar("Float", Field.of[Float])
  checkScalar("LocalDateTime", Field.of[LocalDateTime])
  checkScalar("LocalDate", Field.of[LocalDate])
  checkScalar("LocalTime", Field.of[LocalTime])
  checkScalar("YearMonth", Field.of[YearMonth])
  checkScalar("SqlTimestamp", Field.of[java.sql.Timestamp])
  checkScalar("SqlDate", Field.of[java.sql.Date])
  checkScalar("Date", Field.of[java.util.Date])
  checkScalar("UUID", Field.of[java.util.UUID])
  checkScalar("Byte", Field.of[Byte])
  checkScalar("Short", Field.of[Short])
  checkScalar("BigDecimal", Field.of[BigDecimal])
  checkScalar("Char", Field.of[Char])

  /**
   * check tuple
   */
  checkTuple("Tuple2", tuple(
    "f1" -> Field.of[String],
    "f2" -> Field.of[Int]
  ), "String", "Int")

  /**
   * check mapping
   */

  case class Address(f1: String, f2: Int)
  checkMapping("Address", mapping[Address](
    "f1" -> Field.of[String],
    "f2" -> Field.of[Int]
  ), "Address", "String", "Int")

  /**
   *  check optional scalar
   */

  checkOptional("Int", optional[Int])

  /**
   * check optional tuple
   */

  val optTuple: OptionalField[(String, Int)] = optionalTuple(
    "f1" -> Field.of[String],
    "f2" -> Field.of[Int]
  )
  checkOptional("P-", optTuple)

  val tupleElemField = optTuple.asInstanceOf[OptionalField[_]].elemField
  checkTuple("", tupleElemField, "String", "Int")

  /**
   * check optional mapping
   */
  val optMapping = optionalMapping[Address](
    "f1" -> Field.of[String],
    "f2" -> Field.of[Int]
  )
  checkOptional("P+", optMapping)

  val mappingElemField = optMapping.asInstanceOf[OptionalField[?]].elemField
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
    "name" -> Field.of[String],
    "number" -> Field.of[Int]
  )
  checkSeq("P-",repTuple)
  val repTupElem = repTuple.asInstanceOf[SeqField[?]].elemField
  checkTuple("", repTupElem, "String", "Int")

  /**
   * Check Repeated mapping
   */

  val repMapping = repeatedMapping[Address](
    "f1" -> Field.of[String],
    "f2" -> Field.of[Int]
  )
  checkSeq("P+", repMapping)
  val repMappingElem = repMapping.asInstanceOf[SeqField[?]].elemField
  checkMapping("Address", repMappingElem, "Address", "String", "Int")



  //--------------------------

  def checkScalar(name: String, f: Field[?])(using munit.Location): Unit = {
    test(name){
      assert(f.isInstanceOf[ScalarField[?]], s"Field is not a ScalaField $name")
      assertNoDiff(f.tpe, name)
    }
  }

  def checkTuple(name: String, f: Field[?], typeList: String*)(using munit.Location): Unit =  {
    test(s"Tuple[${name}]"){
      assert(f.isInstanceOf[ProductField[?]], s"Field is not an TupleField $name")
      val prodF = f.asInstanceOf[ProductField[?]]
      assertNoDiff(prodF.tpe, "P-")
      assert(prodF.mirrorOpt.isEmpty)
      val mappings: List[Field[?]] = prodF.mappings.toList.asInstanceOf[List[Field[?]]]
      assertEquals(mappings.size, typeList.size)
      mappings.zip(typeList) foreach{case (mapping, t) =>
        assertNoDiff(mapping.tpe, t)
      }
    }
  }

  def checkMapping(name: String, f: Field[?], className: String,  typeList: String*)(using munit.Location): Unit =  {
    test(s"Mapping[${name}]"){
      assert(f.isInstanceOf[ProductField[?]], s"Field is not an MappingField $name")
      val prodF = f.asInstanceOf[ProductField[?]]
      assert(prodF.tpe.startsWith("P+"))
      assert(prodF.mirrorOpt.isDefined)
      assertNoDiff(prodF.mirrorOpt.get.toString, className)
      val mappings: List[Field[?]] = prodF.mappings.toList.asInstanceOf[List[Field[?]]]
      assertEquals(mappings.size, typeList.size)
      mappings.zip(typeList) foreach{case (mapping, t) =>
        assertNoDiff(mapping.tpe, t)
      }
    }
  }

  def checkOptional(elemTypeName: String, f: Field[?])(using munit.Location): Unit =  {
    test(s"Option[${elemTypeName}]"){
      val optF = f.asInstanceOf[OptionalField[?]]
      assertNoDiff(optF.tpe, "?")
      assertNoDiff(optF.elemField.tpe, elemTypeName)
    }
  }

  def checkSeq(elemTypeName: String, f: Field[?])(using munit.Location): Unit = {
    test(s"Seq[$elemTypeName]"){
      assert(f.isInstanceOf[SeqField[?]], s"Field is not a SeqField $elemTypeName")
      val seqF = f.asInstanceOf[SeqField[?]]
      assertNoDiff(seqF.tpe, "[")
      assertNoDiff(seqF.elemField.tpe, elemTypeName)
    }
  }

  def checkRepeatedTuple(elemTypeName: String, f: Field[?])(using munit.Location): Unit = {
    test(s"Seq[$elemTypeName]"){
      assert(f.isInstanceOf[SeqField[?]], s"Field is not a SeqField $elemTypeName")
      val seqF = f.asInstanceOf[SeqField[?]]
      assertNoDiff(seqF.tpe, "[")
      assertNoDiff(seqF.elemField.tpe, elemTypeName)
    }
  }

}
