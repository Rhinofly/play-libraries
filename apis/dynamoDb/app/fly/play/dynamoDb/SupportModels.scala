package fly.play.dynamoDb

import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.{ JsValue, Reads, Writes, JsObject, Format }
import java.util.Date
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsObject

trait JsonUtils {
  def key[T](name: String)(value: T)(implicit wrt: Writes[T]): (String, JsValue) =
    name -> toJson(value)

  def optional[T](name: String, option: Option[T])(implicit wrt: Writes[T]): List[(String, JsValue)] =
    option.map(key(name)).toList
}

/**
 * increase and decrease values are ignored during conversion to json
 */
case class ProvisionedThroughput(writeCapacityUnits: Int = 5, readCapacityUnits: Int = 10, lastIncreaseDate: Option[Date] = None, lastDescreaseDate: Option[Date] = None)

object ProvisionedThroughput extends ((Int, Int, Option[Date], Option[Date]) => ProvisionedThroughput) {
  implicit object ProvisionedThroughputFormat extends Format[ProvisionedThroughput] {
    def writes(p: ProvisionedThroughput): JsValue = JsObject(Seq(
      "ReadCapacityUnits" -> toJson(p.readCapacityUnits),
      "WriteCapacityUnits" -> toJson(p.writeCapacityUnits)))

    def reads(json: JsValue) = ProvisionedThroughput(
      (json \ "WriteCapacityUnits").as[Int],
      (json \ "ReadCapacityUnits").as[Int],
      (json \ "LastIncreaseDateTime").as[Option[Double]].map(d => new Date(d.toLong)),
      (json \ "LastDecreaseDateTime").as[Option[Double]].map(d => new Date(d.toLong)))
  }
}

sealed trait AttributeType {
  def value: String
}

object AttributeType {
  implicit object AttributeTypetatusFormat extends Format[AttributeType] {
    def reads(json: JsValue) = AttributeType(json.as[String])
    def writes(a: AttributeType): JsValue = toJson(a.value)
  }

  def apply(value: String) = value match {
    case "S" => S
    case "N" => N
    case "NS" => NS
    case "SS" => SS
    case _ => throw new Exception("Could not create TableStatus from '" + value + "'")
  }
}

abstract class SimpleAttributeType(val value: String) extends AttributeType
abstract class SeqAttributeType(val value: String) extends AttributeType

case object S extends SimpleAttributeType("S")
case object N extends SimpleAttributeType("N")
case object NS extends SeqAttributeType("NS")
case object SS extends SeqAttributeType("SS")

sealed abstract class TableStatus(val value: String)

object TableStatus {
  implicit object TableStatusReads extends Reads[TableStatus] {
    def reads(json: JsValue) = json.as[String] match {
      case "CREATING" => CREATING
      case "ACTIVE" => ACTIVE
      case "DELETING" => DELETING
      case "UPDATING" => UPDATING
      case _ => throw new Exception("Could not create TableStatus from '" + json + "'")
    }
  }
}

case object CREATING extends TableStatus("CREATING")
case object ACTIVE extends TableStatus("ACTIVE")
case object DELETING extends TableStatus("DELETING")
case object UPDATING extends TableStatus("UPDATING")

case class Attribute(name: String, tpe: AttributeType) {
  require(name.length > 0, "The given name should have length")
  require(name.length < 256, "The given name has a length that exceeds 255")
}

object Attribute extends ((String, AttributeType) => Attribute) {
  implicit object AttributeFormat extends Format[Attribute] {
    def writes(a: Attribute): JsValue = JsObject(Seq(
      "AttributeName" -> toJson(a.name),
      "AttributeType" -> toJson(a.tpe)))

    def reads(json: JsValue) = Attribute(
      (json \ "AttributeName").as[String],
      (json \ "AttributeType").as[AttributeType])
  }
}

case class KeySchema(hashKeyElement: Attribute, rangeKeyElement: Option[Attribute] = None)

object KeySchema extends ((Attribute, Option[Attribute]) => KeySchema) {
  implicit object KeySchemaFormat extends Format[KeySchema] with JsonUtils {
    def writes(k: KeySchema): JsValue = JsObject(List(
      "HashKeyElement" -> toJson(k.hashKeyElement)) :::
      optional("RangeKeyElement", k.rangeKeyElement))

    def reads(json: JsValue) = KeySchema(
      (json \ "HashKeyElement").as[Attribute],
      (json \ "RangeKeyElement").as[Option[Attribute]])
  }
}

case class TableDescription(name: String, status: TableStatus, creationDate: Option[Date], keySchema: Option[KeySchema], provisionedThroughput: ProvisionedThroughput)

object TableDescription extends ((String, TableStatus, Option[Date], Option[KeySchema], ProvisionedThroughput) => TableDescription) {
  implicit object TableDescriptionReads extends Reads[TableDescription] {
    def reads(json: JsValue) = TableDescription(
      (json \ "TableName").as[String],
      (json \ "TableStatus").as[TableStatus],
      (json \ "CreationDateTime").as[Option[Double]].map(x => new Date(x.toLong)),
      (json \ "KeySchema").as[Option[KeySchema]],
      (json \ "ProvisionedThroughput").as[ProvisionedThroughput])

  }
}

case class Table(name: String, status: TableStatus, creationDate: Option[Date], sizeBytes: Option[Long], itemCount: Option[Int], keySchema: Option[KeySchema], provisionedThroughput: ProvisionedThroughput)

object Table extends ((String, TableStatus, Option[Date], Option[Long], Option[Int], Option[KeySchema], ProvisionedThroughput) => Table) {
  implicit object TableReads extends Reads[Table] {
    def reads(json: JsValue) = Table(
      (json \ "TableName").as[String],
      (json \ "TableStatus").as[TableStatus],
      (json \ "CreationDateTime").as[Option[Double]].map(x => new Date(x.toLong)),
      (json \ "TableSizeBytes").as[Option[Long]],
      (json \ "ItemCount").as[Option[Int]],
      (json \ "KeySchema").as[Option[KeySchema]],
      (json \ "ProvisionedThroughput").as[ProvisionedThroughput])
  }
}

sealed trait AttributeValue {
  type ValueType

  def tpe: AttributeType
  def value: ValueType
}

object AttributeValue {

  implicit object AttributeValueFormat extends Format[AttributeValue] {

    implicit def mapReads[K, V](implicit fmtk: Reads[K], fmtv: Reads[V]): Reads[collection.immutable.Map[K, V]] = new Reads[collection.immutable.Map[K, V]] {
      def reads(json: JsValue) = json match {
        case JsObject(m) => m.map { case (k, v) => JsString(k).as[K] -> v.as[V] }.toMap
        case _ => throw new RuntimeException("Map expected")
      }
    }

    def reads(json: JsValue) = {
      val attributeFormat = json.as[Map[AttributeType, JsValue]]
      attributeFormat.head match {
        case (key: SimpleAttributeType, value: JsString) => AttributeValue(key, value.as[String])
        case (key: SeqAttributeType, value: JsArray) => AttributeValue(key, value.as[Seq[String]])
        case _ => throw new Exception("Invalid key - value combination: " + json)
      }
    }
    
    def writes(a:AttributeValue):JsValue = {
      
      val value = a match {
        case SimpleAttributeValue(_, value) => toJson(value)
        case SeqAttributeValue(_, value) => toJson(value)
      }
      
      JsObject(Seq(a.tpe.value -> value))
    } 
  }

  def apply(tpe: SimpleAttributeType, value: String): AttributeValue = SimpleAttributeValue(tpe, value)
  def apply(tpe: SeqAttributeType, value: Seq[String]): AttributeValue = SeqAttributeValue(tpe, value)
  
}

case class SimpleAttributeValue(tpe: SimpleAttributeType, value: String) extends AttributeValue { type ValueType = String }
case class SeqAttributeValue(tpe: SeqAttributeType, value: Seq[String]) extends AttributeValue { type ValueType = Seq[String] }

case class AttributeExpectation(exists: Boolean, value: Option[AttributeValue] = None) {
  require((exists && value.isDefined) || (!exists && value.isEmpty), "If exists is false, value should be None. If exists is true, value should be Some")
}

object AttributeExpectation extends ((Boolean, Option[AttributeValue]) => AttributeExpectation) {
  implicit object AttributeExpectationWrites extends Writes[AttributeExpectation] with JsonUtils {
    def writes(a:AttributeExpectation):JsValue = JsObject(List(
    		"Exists" -> toJson(a.exists)) ::: 
    		optional("Value", a.value)
    )
  }
}

sealed abstract class ReturnValuesType(val value: String)

object ReturnValuesType {
  implicit object ReturnValuesTypeWrites extends Writes[ReturnValuesType] {
    def writes(r:ReturnValuesType):JsValue = JsString(r.value) 
  }
}

case object NONE extends ReturnValuesType("NONE")
case object ALL_OLD extends ReturnValuesType("ALL_OLD")

case class Key(hashKeyElement:AttributeValue, rangeKeyElement:Option[AttributeValue] = None)

object Key extends ((AttributeValue, Option[AttributeValue]) => Key) {
  implicit object KeyWrites extends Writes[Key] with JsonUtils {
    def writes(k:Key):JsValue = JsObject(List(
        "HashKeyElement" -> toJson(k.hashKeyElement)) :::
        optional("RangeKeyElement", k.rangeKeyElement))
    
  }
}