package fly.play.dynamoDb.models

import play.api.libs.json.{Format, JsValue, JsObject, Reads}
import play.api.libs.json.Json.toJson

sealed trait BatchRequest

object BatchRequest {
  implicit object BatchRequestFormat extends Format[BatchRequest] {
    def writes(b: BatchRequest): JsValue = JsObject(Seq(
      b match {
        case BatchPutRequest(item) => "PutRequest" -> JsObject(Seq("Item" -> toJson(item)))
        case BatchDeleteRequest(key) => "DeleteRequest" -> JsObject(Seq("Key" -> toJson(key)))
      }))

    def reads(json: JsValue) = {
      val attributeFormat = json.as[Map[String, JsObject]]
      attributeFormat.head match {
        case ("PutRequest", value: JsObject) => BatchPutRequest((value \ "Item").as[Map[String, AttributeValue]])
        case ("DeleteRequest", value: JsObject) => BatchDeleteRequest((value \ "Key").as[Key])
        case _ => throw new Exception("Invalid key - value combination: " + json)
      }
    }
  }
}

case class BatchPutRequest(item: Map[String, AttributeValue]) extends BatchRequest
case class BatchDeleteRequest(key: Key) extends BatchRequest

case class BatchGetRequest(keys: Seq[Key], attributesToGet: Option[Seq[String]])

object BatchGetRequest extends ((Seq[Key], Option[Seq[String]]) => BatchGetRequest) {
  implicit object GetRequestFormat extends Format[BatchGetRequest] with JsonUtils {
    def writes(g: BatchGetRequest): JsValue = JsObject(Seq(
      "Keys" -> toJson(g.keys)) ++
      optional("AttributesToGet" -> g.attributesToGet))

    def reads(json: JsValue) = BatchGetRequest(
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