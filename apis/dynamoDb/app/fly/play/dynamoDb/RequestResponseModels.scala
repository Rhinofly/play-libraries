package fly.play.dynamoDb

import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.{ JsValue, Reads, Writes, JsObject }

import models._

/*
 * LIST TABLES
 */

case class ListTablesRequest(limit: Option[Int] = None, exclusiveStartTableName: Option[String] = None)

object ListTablesRequest extends ((Option[Int], Option[String]) => ListTablesRequest) {
  implicit object ListTablesRequestWrites extends Writes[ListTablesRequest] with JsonUtils {
    def writes(r: ListTablesRequest): JsValue = JsObject(Seq.empty ++
      optional("Limit" -> r.limit) ++
        optional("ExclusiveStartTableName" -> r.exclusiveStartTableName))
  }
}

case class ListTablesResponse(tableNames: Seq[String], lastEvaluatedTableName: Option[String])

object ListTablesResponse extends ((Seq[String], Option[String]) => ListTablesResponse) {
  implicit object ListTablesResultReads extends Reads[ListTablesResponse] {
    def reads(json: JsValue) = ListTablesResponse(
      (json \ "TableNames").as[Seq[String]],
      (json \ "LastEvaluatedTableName").asOpt[String])
  }
}

/*
 * CREATE TABLE
 */
case class CreateTableRequest(name: String, keySchema: KeySchema, provisionedThroughput: ProvisionedThroughput = ProvisionedThroughput()) {
  require(name.length > 2, "The given name must have a length of at least 3")
  require(name.length < 256, "The given name has a length that exceeds 255")
  require("""^[a-zA-Z0-9_\-\.]*$""".r.pattern.matcher(name).matches, "Allowed characters for table names are a-z, A-Z, 0-9, '_' (underscore), '-' (dash), and '.' (dot).")
}

object CreateTableRequest extends ((String, KeySchema, ProvisionedThroughput) => CreateTableRequest) {

  implicit object CreateTableRequestWrites extends Writes[CreateTableRequest] {
    def writes(r: CreateTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name),
      "KeySchema" -> toJson(r.keySchema),
      "ProvisionedThroughput" -> toJson(r.provisionedThroughput)))
  }
}

case class CreateTableResponse(description: TableDescription)

object CreateTableResponse extends (TableDescription => CreateTableResponse) {
  implicit object CreateTableResponseReads extends Reads[CreateTableResponse] {
    def reads(j: JsValue) = CreateTableResponse((j \ "TableDescription").as[TableDescription])
  }
}

/*
 * DELETE TABLE
 */

case class DeleteTableRequest(name: String)

object DeleteTableRequest extends (String => DeleteTableRequest) {
  implicit object DeleteTableRequestWrites extends Writes[DeleteTableRequest] {
    def writes(r: DeleteTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name)))
  }
}

case class DeleteTableResponse(description: TableDescription)

object DeleteTableResponse extends (TableDescription => DeleteTableResponse) {
  implicit object DeleteTableResponseReads extends Reads[DeleteTableResponse] {
    def reads(j: JsValue) = DeleteTableResponse((j \ "TableDescription").as[TableDescription])
  }
}

/*
 * DESCRIBE TABLE
 */

case class DescribeTableRequest(name: String)

object DescribeTableRequest extends (String => DescribeTableRequest) {
  implicit object DescribeTableRequestWrites extends Writes[DescribeTableRequest] {
    def writes(r: DescribeTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name)))
  }
}

case class DescribeTableResponse(table: Table)

object DescribeTableResponse extends (Table => DescribeTableResponse) {
  implicit object DescribeTableResponseReads extends Reads[DescribeTableResponse] {
    def reads(j: JsValue) = DescribeTableResponse((j \ "Table").as[Table])
  }
}

/*
 * UPDATE TABLE
 */

case class UpdateTableRequest(name: String, provisionedThroughput: ProvisionedThroughput)

object UpdateTableRequest extends ((String, ProvisionedThroughput) => UpdateTableRequest) {
  implicit object UpdateTableRequestWrites extends Writes[UpdateTableRequest] {
    def writes(r: UpdateTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name),
      "ProvisionedThroughput" -> toJson(r.provisionedThroughput)))
  }
}

case class UpdateTableResponse(tableDescription: TableDescription)

object UpdateTableResponse extends (TableDescription => UpdateTableResponse) {
  implicit object UpdateTableResponseReads extends Reads[UpdateTableResponse] {
    def reads(j: JsValue) = UpdateTableResponse((j \ "TableDescription").as[TableDescription])
  }
}

/*
 * PUT ITEM
 */

case class PutItemRequest(tableName: String, item: Map[String, AttributeValue], returnValues: ReturnValuesType = NONE, expected: Option[Map[String, AttributeExpectation]] = None) {
  require(returnValues match {
    case NONE | ALL_OLD => true
    case _ => false
  }, "Put item only supports NONE and ALL_OLD as return values")
}

object PutItemRequest extends ((String, Map[String, AttributeValue], ReturnValuesType, Option[Map[String, AttributeExpectation]]) => PutItemRequest) {
  implicit object PutItemRequestWrites extends Writes[PutItemRequest] with JsonUtils {
    def writes(r: PutItemRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.tableName),
      "Item" -> toJson(r.item),
      "ReturnValues" -> toJson(r.returnValues)) ++
      optional("Expected" -> r.expected))
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

/*
 * DELETE ITEM
 */
case class DeleteItemRequest(tableName: String, key: Key, returnValues: ReturnValuesType = NONE, expected: Option[Map[String, AttributeExpectation]] = None) {
  require(returnValues match {
    case NONE | ALL_OLD => true
    case _ => false
  }, "Delete item only supports NONE and ALL_OLD as return values")
}

object DeleteItemRequest extends ((String, Key, ReturnValuesType, Option[Map[String, AttributeExpectation]]) => DeleteItemRequest) {
  implicit object DeleteItemRequestWrites extends Writes[DeleteItemRequest] with JsonUtils {
    def writes(r: DeleteItemRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.tableName),
      "Key" -> toJson(r.key),
      "ReturnValues" -> toJson(r.returnValues)) ++
      optional("Expected" -> r.expected))
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

/*
 * UPDATE ITEM
 */

case class UpdateItemRequest(tableName: String, key: Key, attributeUpdates: Map[String, AttributeUpdate], returnValues: ReturnValuesType = NONE, expected: Option[Map[String, AttributeExpectation]] = None)

object UpdateItemRequest extends ((String, Key, Map[String, AttributeUpdate], ReturnValuesType, Option[Map[String, AttributeExpectation]]) => UpdateItemRequest) {
  implicit object UpdateItemRequestWrites extends Writes[UpdateItemRequest] with JsonUtils {
    def writes(r: UpdateItemRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.tableName),
      "Key" -> toJson(r.key),
      "AttributeUpdates" -> toJson(r.attributeUpdates),
      "ReturnValues" -> toJson(r.returnValues)) ++
      optional("Expected" -> r.expected))
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

/*
 * GET ITEM
 */

case class GetItemRequest(tableName: String, key: Key, attributesToGet: Option[Seq[String]] = None, consistentRead: Boolean = false)

object GetItemRequest extends ((String, Key, Option[Seq[String]], Boolean) => GetItemRequest) {
  implicit object GetItemRequestWrites extends Writes[GetItemRequest] with JsonUtils {
    def writes(r: GetItemRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.tableName),
      "Key" -> toJson(r.key),
      "ConsistentRead" -> toJson(r.consistentRead)) ++
      optional("AttributesToGet" -> r.attributesToGet))
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

/*
 * BATCH WRITE ITEM
 */
case class BatchWriteItemRequest(requestItems: Map[String, Seq[BatchRequest]])

object BatchWriteItemRequest extends (Map[String, Seq[BatchRequest]] => BatchWriteItemRequest) {
  implicit object BatchWriteItemRequestWrites extends Writes[BatchWriteItemRequest] {
    def writes(r: BatchWriteItemRequest): JsValue = JsObject(Seq(
      "RequestItems" -> toJson(r.requestItems)))
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

/*
 * BATCH GET ITEM
 */
case class BatchGetItemRequest(requestItems: Map[String, GetRequest])

object BatchGetItemRequest extends (Map[String, GetRequest] => BatchGetItemRequest) {
  implicit object BatchGetItemRequestWrites extends Writes[BatchGetItemRequest] {
    def writes(r: BatchGetItemRequest): JsValue = JsObject(Seq(
      "RequestItems" -> toJson(r.requestItems)))
  }
}

case class BatchGetItemResponse(responses: Map[String, TableItems], unprocessedKeys: Map[String, GetRequest])

object BatchGetItemResponse extends ((Map[String, TableItems], Map[String, GetRequest]) => BatchGetItemResponse) {
  implicit object BatchGetItemResponseReads extends Reads[BatchGetItemResponse] {
    def reads(json: JsValue) = BatchGetItemResponse(
      (json \ "Responses").as[Map[String, TableItems]],
      (json \ "UnprocessedKeys").as[Map[String, GetRequest]])
  }
}

/*
 * QUERY
 */

case class QueryRequest(tableName: String, hashKeyValue: AttributeValue, rangeKeyCondition: Option[Condition] = None, exclusiveStartKey: Option[Key] = None, attributesToGet: Option[Seq[String]] = None, scanIndexForward: Boolean = true, limit: Option[Int] = None, consistentRead: Boolean = false) {
  require((rangeKeyCondition match {
    case Some(Condition(EQ | LE | LT | GE | GT | BEGINS_WITH | BETWEEN, _)) => true
    case Some(_) => false
    case None => true
  }), "The given operator is not valid in a range key condition")
}

object QueryRequest extends ((String, AttributeValue, Option[Condition], Option[Key], Option[Seq[String]], Boolean, Option[Int], Boolean) => QueryRequest) {
  implicit object QueryRequestWrites extends Writes[QueryRequest] with JsonUtils {
    def writes(r: QueryRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.tableName),
      "HashKeyValue" -> toJson(r.hashKeyValue),
      "ScanIndexForward" -> toJson(r.scanIndexForward),
      "ConsistentRead" -> toJson(r.consistentRead)) ++
      optional("RangeKeyCondition" -> r.rangeKeyCondition) ++
      optional("ExclusiveStartKey" -> r.exclusiveStartKey) ++
      optional("AttributesToGet" -> r.attributesToGet) ++
      optional("Limit" -> r.limit))
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

/*
 * SCAN
 */
case class ScanRequest(tableName: String, attributesToGet: Option[Seq[String]] = None, scanFilter: Option[Map[String, Condition]] = None, limit: Option[Int] = None, count: Boolean = false, exclusiveStartKey: Option[Key] = None) {
  require(!count || attributesToGet.isEmpty, "When count is set to true, you may not pass attributes to get")
}

object ScanRequest extends ((String, Option[Seq[String]], Option[Map[String, Condition]], Option[Int], Boolean, Option[Key]) => ScanRequest) {
  implicit object ScanRequestWrites extends Writes[ScanRequest] with JsonUtils {
    def writes(r: ScanRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.tableName),
      "Count" -> toJson(r.count)) ++
      optional("Limit" -> r.limit) ++
      optional("ScanFilter" -> r.scanFilter) ++
      optional("ExclusiveStartKey" -> r.exclusiveStartKey) ++
      optional("AttributesToGet" -> r.attributesToGet))
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
