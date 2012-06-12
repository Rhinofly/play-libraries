package fly.play.dynamoDb

import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsObject

trait DefaultWrites {
  implicit object IntWrites extends Writes[Int] {
    def writes(o: Int) = JsString(o.toString)
  }
}

trait JsonUtils {
  def key[T](name: String)(value: T)(implicit wrt: Writes[T]): (String, JsValue) =
    name -> toJson(value)

  def optional[T](name: String, option: Option[T])(implicit wrt: Writes[T]): List[(String, JsValue)] =
    option.map(key(name)).toList
}

/*
 * SUPPORT CLASSES
 */

case class ProvisionedThroughPut(writeCapacityUnits: Int = 5, readCapacityUnits: Int = 10)

object ProvisionedThroughPut extends ((Int, Int) => ProvisionedThroughPut) {
  implicit object ProvisionedThroughPutWrites extends Writes[ProvisionedThroughPut] {
    def writes(p: ProvisionedThroughPut): JsValue = JsObject(Seq(
      "ReadCapacityUnits" -> toJson(p.readCapacityUnits),
      "WriteCapacityUnits" -> toJson(p.writeCapacityUnits)))
  }
}

sealed abstract class AttributeType(val value: String)

case object S extends AttributeType("S")
case object N extends AttributeType("N")

case class Attribute(name: String, tpe: AttributeType) {
  require(name.length > 0, "The given name should have length")
  require(name.length < 256, "The given name has a length that exceeds 255")
}

object Attribute extends ((String, AttributeType) => Attribute) {
  implicit object AttributeWrites extends Writes[Attribute] {
    def writes(a: Attribute): JsValue = JsObject(Seq(
      "AttributeName" -> toJson(a.name),
      "AttributeType" -> toJson(a.tpe.value)))
  }
}

case class KeySchema(hashKeyElement: Attribute, rangeKeyElement: Option[Attribute] = None)

object KeySchema extends ((Attribute, Option[Attribute]) => KeySchema) {
  implicit object KeySchemaWrites extends Writes[KeySchema] with JsonUtils {
    def writes(k: KeySchema): JsValue = JsObject(List(
      "HashKeyElement" -> toJson(k.hashKeyElement)) :::
      optional("RangeKeyElement", k.rangeKeyElement))
  }
}

/*
 * EXCEPTIONS
 */

trait DynamoDbException {
  def message: String
}

object DynamoDbException {
  implicit object DynamoDbExceptionReads extends Reads[DynamoDbException] {
    def reads(json: JsValue) = convert((json \ "__type").as[String], (json \ "Message").as[String])

    def convert(tpe: String, message: String): DynamoDbException = tpe match {
      case "com.amazon.coral.service#SerializationException" => SerializationException(message)
    }
  }
}

case class SerializationException(message: String) extends DynamoDbException

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
case class CreateTableRequest(name: String, keySchema: KeySchema, provisionedThroughPut: ProvisionedThroughPut = ProvisionedThroughPut()) {
  require(name.length > 2, "The given name must have a length of at least 3")
  require(name.length < 256, "The given name has a length that exceeds 255")
  require("""^[a-zA-Z0-9_\-\.]*$""".r.pattern.matcher(name).matches, "Allowed characters for table names are a-z, A-Z, 0-9, '_' (underscore), '-' (dash), and '.' (dot).")
}

object CreateTableRequest extends ((String, KeySchema, ProvisionedThroughPut) => CreateTableRequest) {
  implicit object CreateTableRequestWrites extends Writes[CreateTableRequest] {
    def writes(r: CreateTableRequest): JsValue = JsObject(Seq(
      "TableName" -> toJson(r.name),
      "KeySchema" -> toJson(r.keySchema),
      "ProvisionedThroughput" -> toJson(r.provisionedThroughPut)))
  }
}
