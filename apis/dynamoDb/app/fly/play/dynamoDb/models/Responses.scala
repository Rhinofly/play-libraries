package fly.play.dynamoDb.models

import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.{ JsValue, Reads, Writes, JsObject }

case class ListTablesResponse(tableNames: Seq[String], lastEvaluatedTableName: Option[String])

object ListTablesResponse extends ((Seq[String], Option[String]) => ListTablesResponse) {
  implicit object ListTablesResultReads extends Reads[ListTablesResponse] {
    def reads(json: JsValue) = ListTablesResponse(
      (json \ "TableNames").as[Seq[String]],
      (json \ "LastEvaluatedTableName").asOpt[String])
  }
}

case class CreateTableResponse(description: TableDescription)

object CreateTableResponse extends (TableDescription => CreateTableResponse) {
  implicit object CreateTableResponseReads extends Reads[CreateTableResponse] {
    def reads(j: JsValue) = CreateTableResponse((j \ "TableDescription").as[TableDescription])
  }
}

case class DeleteTableResponse(description: TableDescription)

object DeleteTableResponse extends (TableDescription => DeleteTableResponse) {
  implicit object DeleteTableResponseReads extends Reads[DeleteTableResponse] {
    def reads(j: JsValue) = DeleteTableResponse((j \ "TableDescription").as[TableDescription])
  }
}

case class DescribeTableResponse(tableDescription: TableDescription)

object DescribeTableResponse extends (TableDescription => DescribeTableResponse) {
  implicit object DescribeTableResponseReads extends Reads[DescribeTableResponse] {
    def reads(j: JsValue) = DescribeTableResponse((j \ "Table").as[TableDescription])
  }
}

case class UpdateTableResponse(tableDescription: TableDescription)

object UpdateTableResponse extends (TableDescription => UpdateTableResponse) {
  implicit object UpdateTableResponseReads extends Reads[UpdateTableResponse] {
    def reads(j: JsValue) = UpdateTableResponse((j \ "TableDescription").as[TableDescription])
  }
}

case class PutItemResponse(attributes: Map[String, AttributeValue], consumedCapacityUnits: Double)

object PutItemResponse extends ((Map[String, AttributeValue], Double) => PutItemResponse) {
  implicit object PutItemResponseReads extends Reads[PutItemResponse] {
    def reads(json: JsValue) = PutItemResponse(
      (json \ "Attributes").as[Map[String, AttributeValue]],
      (json \ "ConsumedCapacityUnits").as[Double])
  }
}

case class DeleteItemResponse(attributes: Map[String, AttributeValue], consumedCapacityUnits: Double)

object DeleteItemResponse extends ((Map[String, AttributeValue], Double) => DeleteItemResponse) {
  implicit object DeleteItemResponseReads extends Reads[DeleteItemResponse] {
    def reads(json: JsValue) = DeleteItemResponse(
      (json \ "Attributes").as[Map[String, AttributeValue]],
      (json \ "ConsumedCapacityUnits").as[Double])
  }
}

case class UpdateItemResponse(attributes: Map[String, AttributeValue], consumedCapacityUnits: Double)

object UpdateItemResponse extends ((Map[String, AttributeValue], Double) => UpdateItemResponse) {
  implicit object UpdateItemResponseReads extends Reads[UpdateItemResponse] {
    def reads(json: JsValue) = UpdateItemResponse(
      (json \ "Attributes").as[Map[String, AttributeValue]],
      (json \ "ConsumedCapacityUnits").as[Double])
  }
}

case class GetItemResponse(item: Map[String, AttributeValue], consumedCapacityUnits: Double)

object GetItemResponse extends ((Map[String, AttributeValue], Double) => GetItemResponse) {
  implicit object GetItemResponseReads extends Reads[GetItemResponse] {
    def reads(json: JsValue) = GetItemResponse(
      (json \ "Item").as[Map[String, AttributeValue]],
      (json \ "ConsumedCapacityUnits").as[Double])
  }
}

case class BatchWriteItemResponse(responses: Map[String, Double], unprocessedItems: Map[String, Seq[BatchRequest]])

object BatchWriteItemResponse extends ((Map[String, Double], Map[String, Seq[BatchRequest]]) => BatchWriteItemResponse) {
  implicit object BatchWriteItemResponseReads extends Reads[BatchWriteItemResponse] {
    def reads(json: JsValue) = BatchWriteItemResponse(
      (json \ "Responses").as[Map[String, JsObject]].map { case (k, v) => k -> (v \ "ConsumedCapacityUnits").as[Double] },
      (json \ "UnprocessedItems").as[Map[String, Seq[BatchRequest]]])
  }
}

case class BatchGetItemResponse(responses: Map[String, TableItems], unprocessedKeys: Map[String, BatchGetRequest])

object BatchGetItemResponse extends ((Map[String, TableItems], Map[String, BatchGetRequest]) => BatchGetItemResponse) {
  implicit object BatchGetItemResponseReads extends Reads[BatchGetItemResponse] {
    def reads(json: JsValue) = BatchGetItemResponse(
      (json \ "Responses").as[Map[String, TableItems]],
      (json \ "UnprocessedKeys").as[Map[String, BatchGetRequest]])
  }
}

case class QueryResponse(count: Int, items: Seq[Map[String, AttributeValue]], lastEvaluatedKey: Option[Key], consumedCapacityUnits: Double)

object QueryResponse extends ((Int, Seq[Map[String, AttributeValue]], Option[Key], Double) => QueryResponse) {
  implicit object QueryResponseReads extends Reads[QueryResponse] {
    def reads(json: JsValue) = QueryResponse(
      (json \ "Count").as[Int],
      (json \ "Items").as[Seq[Map[String, AttributeValue]]],
      (json \ "LastEvaluatedKey").asOpt[Key],
      (json \ "ConsumedCapacityUnits").as[Double])
  }
}

case class ScanResponse(count: Int, items: Option[Seq[Map[String, AttributeValue]]], lastEvaluatedKey: Option[Key], consumedCapacityUnits: Double, scannedCount: Int)

object ScanResponse extends ((Int, Option[Seq[Map[String, AttributeValue]]], Option[Key], Double, Int) => ScanResponse) {
  implicit object ScanResponseReads extends Reads[ScanResponse] {
    def reads(json: JsValue) = ScanResponse(
      (json \ "Count").as[Int],
      (json \ "Items").asOpt[Seq[Map[String, AttributeValue]]],
      (json \ "LastEvaluatedKey").asOpt[Key],
      (json \ "ConsumedCapacityUnits").as[Double],
      (json \ "ScannedCount").as[Int])
  }
}
