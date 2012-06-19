package fly.play.dynamoDb

import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.{JsValue, Reads, Writes, JsObject}


/*
 * LIST TABLES
 */

case class ListTablesRequest(limit: Option[Int] = None, exclusiveStartTableName: Option[String] = None)

object ListTablesRequest extends ((Option[Int], Option[String]) => ListTablesRequest) {
  implicit object ListTablesRequestWrites extends Writes[ListTablesRequest] with JsonUtils {
    def writes(r: ListTablesRequest): JsValue = JsObject(
      optional("Limit", r.limit) :::
        optional("ExclusiveStartTableName", r.exclusiveStartTableName))
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

case class DescribeTableResponse(table:Table)

object DescribeTableResponse extends (Table => DescribeTableResponse) {
  implicit object DescribeTableResponseReads extends Reads[DescribeTableResponse] {
    def reads(j:JsValue) = DescribeTableResponse((j \ "Table").as[Table])
  }
}

/*
 * UPDATE TABLE
 */

case class UpdateTableRequest(name:String, provisionedThroughput:ProvisionedThroughput)

object UpdateTableRequest extends ((String, ProvisionedThroughput) => UpdateTableRequest) {
  implicit object UpdateTableRequestWrites extends Writes[UpdateTableRequest] {
    def writes(r:UpdateTableRequest):JsValue = JsObject(Seq(
        "TableName" -> toJson(r.name),
        "ProvisionedThroughput" -> toJson(r.provisionedThroughput)))
  }
}

case class UpdateTableResponse(tableDescription:TableDescription)

object UpdateTableResponse extends (TableDescription => UpdateTableResponse) {
  implicit object UpdateTableResponseReads extends Reads[UpdateTableResponse] {
    def reads(j:JsValue) = UpdateTableResponse((j \ "TableDescription").as[TableDescription])
  }
}
