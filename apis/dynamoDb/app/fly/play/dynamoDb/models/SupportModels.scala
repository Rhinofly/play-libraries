package fly.play.dynamoDb.models

import java.util.Date
import play.api.libs.json.Json.toJson
import play.api.libs.json.Reads
import play.api.libs.json.Format
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Writes

trait JsonUtils {
  def optional[T](key:(String, Option[T]))(implicit wrt: Writes[T]):Option[(String, JsValue)] =
    key._2 map (value => key._1 -> toJson(value))
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

case class KeySchema(hashKeyElement: Attribute, rangeKeyElement: Option[Attribute] = None)

object KeySchema extends ((Attribute, Option[Attribute]) => KeySchema) {
  implicit object KeySchemaFormat extends Format[KeySchema] with JsonUtils {
    def writes(k: KeySchema): JsValue = JsObject(Seq(
      "HashKeyElement" -> toJson(k.hashKeyElement)) ++
      optional("RangeKeyElement" -> k.rangeKeyElement))

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

case class AttributeExpectation(exists: Boolean, value: Option[AttributeValue] = None) {
  require((exists && value.isDefined) || (!exists && value.isEmpty), "If exists is false, value should be None. If exists is true, value should be Some")
}

object AttributeExpectation extends ((Boolean, Option[AttributeValue]) => AttributeExpectation) {
  implicit object AttributeExpectationWrites extends Writes[AttributeExpectation] with JsonUtils {
    def writes(a: AttributeExpectation): JsValue = JsObject(Seq(
      "Exists" -> toJson(a.exists)) ++
      optional("Value" -> a.value))
  }
}

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
    def writes(g: GetRequest): JsValue = JsObject(Seq(
      "Keys" -> toJson(g.keys)) ++
      optional("AttributesToGet" -> g.attributesToGet))

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
    def writes(c: Condition): JsValue = JsObject(Seq(
      "ComparisonOperator" -> toJson(c.comparisonOperator)) ++
      optional("AttributeValueList" -> c.attributeValueList))
  }
}