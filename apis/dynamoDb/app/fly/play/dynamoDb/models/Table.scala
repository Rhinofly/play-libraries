package fly.play.dynamoDb.models

import play.api.libs.json.{Format, JsValue, JsObject, Reads}
import play.api.libs.json.Json.toJson
import java.util.Date

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

case class Key(hashKeyElement: AttributeValue, rangeKeyElement: Option[AttributeValue] = None)

object Key extends ((AttributeValue, Option[AttributeValue]) => Key) {
  implicit object KeyFormat extends Format[Key] with JsonUtils {
    def writes(k: Key): JsValue = JsObject(Seq(
      "HashKeyElement" -> toJson(k.hashKeyElement)) ++
      optional("RangeKeyElement" -> k.rangeKeyElement))

    def reads(json: JsValue) = Key(
      (json \ "HashKeyElement").as[AttributeValue],
      (json \ "RangeKeyElement").as[Option[AttributeValue]])
  }
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

sealed abstract class TableStatus(val value: String)
