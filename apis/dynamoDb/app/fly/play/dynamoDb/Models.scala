package fly.play.dynamoDb

import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

trait DefaultWrites {


  implicit object IntWrites extends Writes[Int] {
    def writes(o: Int) = JsString(o.toString)
  }
}

trait JsonUtils {
	def key[T](name: String)(value: T)(implicit wrt: Writes[T]): (String, JsValue) = name -> toJson(value)
			
			def optional[T](name: String, option: Option[T])(implicit wrt: Writes[T]): List[(String, JsValue)] = option.map(key(name)).toList
}

trait DynamoDbException {
  def message:String
}

object DynamoDbException {
  implicit object DynamoDbExceptionReads extends Reads[DynamoDbException] {
    def reads(json:JsValue) = convert((json \ "__type").as[String], (json \ "Message").as[String])
    
    def convert(tpe:String, message:String):DynamoDbException = tpe match {
      case "com.amazon.coral.service#SerializationException" => SerializationException(message)
    }
  }
}

case class SerializationException(message:String) extends DynamoDbException

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
