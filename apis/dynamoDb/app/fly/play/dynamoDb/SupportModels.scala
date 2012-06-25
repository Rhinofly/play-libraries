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
    case _ => throw new Exception("Could not create AttributeType from '" + value + "'")
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

sealed abstract class AttributeValue {
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

    def writes(a: AttributeValue): JsValue = {

      val value = a match {
        case SimpleAttributeValue(_, value) => toJson(value)
        case SeqAttributeValue(_, value) => toJson(value)
      }

      JsObject(Seq(a.tpe.value -> value))
    }
  }

  def apply(tpe: SimpleAttributeType, value: String): AttributeValue = SimpleAttributeValue(tpe, value)
  def apply(tpe: SeqAttributeType, value: Seq[String]): AttributeValue = SeqAttributeValue(tpe, value)

  def unapply(a: AttributeValue): Option[(AttributeType, Any)] = if (a == null) None else Some((a.tpe, a.value))
}

case class SimpleAttributeValue(tpe: SimpleAttributeType, value: String) extends AttributeValue { type ValueType = String }
case class SeqAttributeValue(tpe: SeqAttributeType, value: Seq[String]) extends AttributeValue { type ValueType = Seq[String] }

case class AttributeExpectation(exists: Boolean, value: Option[AttributeValue] = None) {
  require((exists && value.isDefined) || (!exists && value.isEmpty), "If exists is false, value should be None. If exists is true, value should be Some")
}

object AttributeExpectation extends ((Boolean, Option[AttributeValue]) => AttributeExpectation) {
  implicit object AttributeExpectationWrites extends Writes[AttributeExpectation] with JsonUtils {
    def writes(a: AttributeExpectation): JsValue = JsObject(List(
      "Exists" -> toJson(a.exists)) :::
      optional("Value", a.value))
  }
}

sealed abstract class ReturnValuesType(val value: String)

object ReturnValuesType {
  implicit object ReturnValuesTypeWrites extends Writes[ReturnValuesType] {
    def writes(r: ReturnValuesType): JsValue = JsString(r.value)
  }
}

case object NONE extends ReturnValuesType("NONE")
case object ALL_OLD extends ReturnValuesType("ALL_OLD")
case object ALL_NEW extends ReturnValuesType("ALL_NEW")
case object UPDATED_OLD extends ReturnValuesType("UPDATED_OLD")
case object UPDATED_NEW extends ReturnValuesType("UPDATED_NEW")

case class Key(hashKeyElement: AttributeValue, rangeKeyElement: Option[AttributeValue] = None)

object Key extends ((AttributeValue, Option[AttributeValue]) => Key) {
  implicit object KeyFormat extends Format[Key] with JsonUtils {
    def writes(k: Key): JsValue = JsObject(List(
      "HashKeyElement" -> toJson(k.hashKeyElement)) :::
      optional("RangeKeyElement", k.rangeKeyElement))

    def reads(json: JsValue) = Key(
      (json \ "HashKeyElement").as[AttributeValue],
      (json \ "RangeKeyElement").as[Option[AttributeValue]])
  }
}

sealed abstract class AttributeUpdateAction(val value: String)

object AttributeUpdateAction {
  implicit object AttributeUpdateActionWrites extends Writes[AttributeUpdateAction] {
    def writes(a: AttributeUpdateAction): JsValue = JsString(a.value)
  }
}

case object PUT extends AttributeUpdateAction("PUT")
case object DELETE extends AttributeUpdateAction("DELETE")
case object ADD extends AttributeUpdateAction("ADD")

case class AttributeUpdate(value: AttributeValue, action: AttributeUpdateAction = PUT) {
  require(action != DELETE || (value match {
    case SeqAttributeValue(_, value) if value.size > 0 => true
    case _ => false
  }), "Delete action only works when providing a seq type attribute that is not empty")

  require(action != ADD || (value match {
    case SimpleAttributeValue(S, _) => false
    case _ => true
  }), "Add action does not work for simple string type attributes")
}

object AttributeUpdate extends ((AttributeValue, AttributeUpdateAction) => AttributeUpdate) {
  implicit object AttributeUpdateWrites extends Writes[AttributeUpdate] {
    def writes(a: AttributeUpdate): JsValue = JsObject(Seq(
      "Value" -> toJson(a.value),
      "Action" -> toJson(a.action)))
  }
}

sealed trait BatchRequest

object BatchRequest {
  implicit object BatchRequestFormat extends Format[BatchRequest] {
    def writes(b: BatchRequest): JsValue = JsObject(Seq(
      b match {
        case PutRequest(item) => "PutRequest" -> JsObject(Seq("Item" -> toJson(item)))
        case DeleteRequest(key) => "DeleteRequest" -> JsObject(Seq("Key" -> toJson(key)))
      }))

    def reads(json: JsValue) = {
      val attributeFormat = json.as[Map[String, JsObject]]
      attributeFormat.head match {
        case ("PutRequest", value: JsObject) => PutRequest((value \ "Item").as[Map[String, AttributeValue]])
        case ("DeleteRequest", value: JsObject) => DeleteRequest((value \ "Key").as[Key])
        case _ => throw new Exception("Invalid key - value combination: " + json)
      }
    }
  }
}

case class PutRequest(item: Map[String, AttributeValue]) extends BatchRequest
case class DeleteRequest(key: Key) extends BatchRequest

case class GetRequest(keys: Seq[Key], attributesToGet: Option[Seq[String]])

object GetRequest extends ((Seq[Key], Option[Seq[String]]) => GetRequest) {
  implicit object GetRequestFormat extends Format[GetRequest] with JsonUtils {
    def writes(g: GetRequest): JsValue = JsObject(List(
      "Keys" -> toJson(g.keys)) :::
      optional("AttributesToGet", g.attributesToGet))

    def reads(json: JsValue) = GetRequest(
      (json \ "Keys").as[Seq[Key]],
      (json \ "AttributesToGet").as[Option[Seq[String]]])
  }
}

case class TableItems(items: Seq[Map[String, AttributeValue]], consumedCapacityUnits: Double)

object TableItems extends ((Seq[Map[String, AttributeValue]], Double) => TableItems) {
  implicit object TableItemsReads extends Reads[TableItems] {
    def reads(json: JsValue) = TableItems(
      (json \ "Items").as[Seq[Map[String, AttributeValue]]],
      (json \ "ConsumedCapacityUnits").as[Double])
  }
}

sealed abstract class ComparisonOperator(val value: String)

object ComparisonOperator {
  implicit object ComparisonOperatorWrites extends Writes[ComparisonOperator] {
    def writes(a: ComparisonOperator): JsValue = JsString(a.value)
  }
}

case object EQ extends ComparisonOperator("EQ")
case object LE extends ComparisonOperator("LE")
case object LT extends ComparisonOperator("LT")
case object GE extends ComparisonOperator("GE")
case object GT extends ComparisonOperator("GT")
case object BEGINS_WITH extends ComparisonOperator("BEGINS_WITH")
case object BETWEEN extends ComparisonOperator("BETWEEN")
case object NE extends ComparisonOperator("NE")
case object NOT_NULL extends ComparisonOperator("NOT_NULL")
case object NULL extends ComparisonOperator("NULL")
case object CONTAINS extends ComparisonOperator("CONTAINS")
case object NOT_CONTAINS extends ComparisonOperator("NOT_CONTAINS")
case object IN extends ComparisonOperator("IN")

case class Condition(comparisonOperator: ComparisonOperator, attributeValueList: Option[Seq[AttributeValue]]) {
  comparisonOperator match {
    case EQ | LE | LT | GE | GT | NE | CONTAINS | NOT_CONTAINS => require(attributeValueList match {
      case Some(Seq(SimpleAttributeValue(_, _))) => true
      case _ => false
    }, "The operator " + comparisonOperator + " requires that the attribute value list has a size of 1 and that it contains 1 simple attribute value")
    case BEGINS_WITH => require(attributeValueList match {
    	case Some(Seq(SimpleAttributeValue(S, _))) => true
    	case _ => false
    }, "The operator BEGINS_WITH requires that the attribute value list has a size of 1 and that it contains a string attribute value")
    case BETWEEN => require(attributeValueList match {
      case Some(Seq(AttributeValue(S, _), AttributeValue(S, _))) => true
      case Some(Seq(AttributeValue(N, _), AttributeValue(N, _))) => true
      case _ => false
    }, "The operator BETWEEN requires that the attribute value list has a size of 2 and that it contains two number or two string values")
    case NOT_NULL | NULL => require(attributeValueList match {
      case None => true
      case _ => false
    }, "The operator " + comparisonOperator + " requires that no attribute value list is specified")
    case IN => require(attributeValueList match {
      case Some(x) if (x.size > 0 && x.forall(_.isInstanceOf[SimpleAttributeValue])) => true
      case _ => false
    }, "The operator IN requires that the attribute value list has a size of at least 1 and that it contains simple value attributes")
  }
}

object Condition extends ((ComparisonOperator, Option[Seq[AttributeValue]]) => Condition) {
  implicit object ConditionWrites extends Writes[Condition] with JsonUtils {
    def writes(c: Condition): JsValue = JsObject(List(
      "ComparisonOperator" -> toJson(c.comparisonOperator)) :::

      optional("AttributeValueList", c.attributeValueList))
  }
}