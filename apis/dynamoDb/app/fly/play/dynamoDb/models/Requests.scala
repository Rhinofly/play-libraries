package fly.play.dynamoDb.models

import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.{ JsValue, Reads, Writes, JsObject }

sealed abstract class ReturnValuesType(val value: String)

object ReturnValuesType {
  implicit object ReturnValuesTypeWrites extends Writes[ReturnValuesType] {
    def writes(r: ReturnValuesType): JsValue = toJson(r.value)
  }
}

case object NONE extends ReturnValuesType("NONE")
case object ALL_OLD extends ReturnValuesType("ALL_OLD")
case object ALL_NEW extends ReturnValuesType("ALL_NEW")
case object UPDATED_OLD extends ReturnValuesType("UPDATED_OLD")
case object UPDATED_NEW extends ReturnValuesType("UPDATED_NEW")


case class ListTablesRequest(limit: Option[Int] = None, exclusiveStartTableName: Option[String] = None)

object ListTablesRequest extends ((Option[Int], Option[String]) => ListTablesRequest) {
  implicit object ListTablesRequestWrites extends Writes[ListTablesRequest] with JsonUtils {
    def writes(r: ListTablesRequest): JsValue = JsObject(Seq.empty ++
      optional("Limit" -> r.limit) ++
        optional("ExclusiveStartTableName" -> r.exclusiveStartTableName))
  }
}

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

case class DeleteTableRequest(name: String)

object DeleteTableRequest extends (String => DeleteTableRequest) {
  implicit object DeleteTableRequestWrites extends Writes[DeleteTableRequest] {
    def writes(r: DeleteTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name)))
  }
}

case class DescribeTableRequest(name: String)

object DescribeTableRequest extends (String => DescribeTableRequest) {
  implicit object DescribeTableRequestWrites extends Writes[DescribeTableRequest] {
    def writes(r: DescribeTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name)))
  }
}

case class UpdateTableRequest(name: String, provisionedThroughput: ProvisionedThroughput)

object UpdateTableRequest extends ((String, ProvisionedThroughput) => UpdateTableRequest) {
  implicit object UpdateTableRequestWrites extends Writes[UpdateTableRequest] {
    def writes(r: UpdateTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name),
      "ProvisionedThroughput" -> toJson(r.provisionedThroughput)))
  }
}

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

case class BatchWriteItemRequest(requestItems: Map[String, Seq[BatchRequest]])

object BatchWriteItemRequest extends (Map[String, Seq[BatchRequest]] => BatchWriteItemRequest) {
  implicit object BatchWriteItemRequestWrites extends Writes[BatchWriteItemRequest] {
    def writes(r: BatchWriteItemRequest): JsValue = JsObject(Seq(
      "RequestItems" -> toJson(r.requestItems)))
  }
}

case class BatchGetItemRequest(requestItems: Map[String, BatchGetRequest])

object BatchGetItemRequest extends (Map[String, BatchGetRequest] => BatchGetItemRequest) {
  implicit object BatchGetItemRequestWrites extends Writes[BatchGetItemRequest] {
    def writes(r: BatchGetItemRequest): JsValue = JsObject(Seq(
      "RequestItems" -> toJson(r.requestItems)))
  }
}

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