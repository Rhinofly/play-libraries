package fly.play.dynamoDb

import play.api.libs.json.{JsValue, JsString, Reads}

trait DynamoDbException {
  def message: String
}

object DynamoDbException {
  implicit object DynamoDbExceptionReads extends Reads[DynamoDbException] {
    def reads(json: JsValue) = ((json \ "__type"), (json \ "message")) match {
      case (tpe:JsString, message:JsString) => convert(tpe.as[String], message.as[String])
      case _ => throw new Exception("Unknown error: " + json)
    } 

    def convert(tpe: String, message: String): DynamoDbException = tpe match {
      case "com.amazonaws.dynamodb.v20111205#SerializationException" => SerializationException(message)
      case "com.amazonaws.dynamodb.v20111205#ResourceInUseException" => ResourceInUseException(message)
      case "com.amazonaws.dynamodb.v20111205#ResourceNotFoundException" => ResourceNotFoundException(message)
      case "com.amazonaws.dynamodb.v20111205#ConditionalCheckFailedException" => ConditionalCheckFailedException(message)
      case _ => throw new Exception("Unknown error: " + tpe + " with message: " + message)
    }
  }
}

case class SerializationException(message: String) extends DynamoDbException
case class ResourceInUseException(message: String) extends DynamoDbException
case class ResourceNotFoundException(message: String) extends DynamoDbException
case class ConditionalCheckFailedException(message: String) extends DynamoDbException