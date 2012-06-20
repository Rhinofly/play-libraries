package fly.play.dynamoDb

import play.api.libs.json.Json.{ toJson }
import fly.play.aws.Aws
import fly.play.aws.auth.AwsCredentials
import play.api.http.ContentTypeOf
import fly.play.sts.AwsSessionCredentials
import play.api.libs.concurrent.Promise
import play.api.libs.ws.Response
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import fly.play.sts.AwsSessionCredentials
import fly.play.sts.AwsSessionCredentials

object DynamoDb {
	
	val version = "DynamoDB_20111205."
	val url = "https://dynamodb.us-east-1.amazonaws.com"
	  
	def sessionCredentials(implicit credentials:AwsCredentials) = credentials match {
	  case x:AwsSessionCredentials => x
	  case x => AwsSessionCredentials(x)
	}
	
	implicit object XAmzJson extends ContentTypeOf[String](Some("application/x-amz-json-1.0"))
	
	def response[T](response:Response)(implicit rds:Reads[T]):Either[DynamoDbException, T] = response.status match {
	  case 200 => Right(response.json.as[T])
	  case n => Left(response.json.as[DynamoDbException])
	}
	
	def post[S, T](action:String, body:S, converter:Response => Either[DynamoDbException, T])(implicit wrt:Writes[S], credentials:AwsCredentials) =  
	  Aws
	  	.withSigner3(sessionCredentials)
	  	.url(url)
	  	.withHeaders("x-amz-target" -> (version + action))
	  	.post(toJson(body))
	  	.map(converter)
	
	/** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_ListTables.html */
	def listTables(request:ListTablesRequest)(implicit credentials:AwsCredentials) = post("ListTables", request, response[ListTablesResponse])
	def apply(request:ListTablesRequest)(implicit credentials:AwsCredentials) = listTables(request)
	
	/** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_CreateTable.html */
	def createTable(request:CreateTableRequest)(implicit credentials:AwsCredentials) = post("CreateTable", request, response[CreateTableResponse])
	def apply(request:CreateTableRequest)(implicit credentials:AwsCredentials) = createTable(request)
	
	/** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_DeleteTable.html */
	def deleteTable(request:DeleteTableRequest)(implicit credentials:AwsCredentials) = post("DeleteTable", request, response[DeleteTableResponse])
	def apply(request:DeleteTableRequest)(implicit credentials:AwsCredentials) = deleteTable(request)
	
	/** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_DescribeTables.html */
	def describeTable(request:DescribeTableRequest)(implicit credentials:AwsCredentials) = post("DescribeTable", request, response[DescribeTableResponse])
	def apply(request:DescribeTableRequest)(implicit credentials:AwsCredentials) = describeTable(request)
	
	/** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_UpdateTable.html */
	def updateTable(request:UpdateTableRequest)(implicit credentials:AwsCredentials) = post("UpdateTable", request, response[UpdateTableResponse])
	def apply(request:UpdateTableRequest)(implicit credentials:AwsCredentials) = updateTable(request)
	
	/** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_PutItem.html */
	def putItem(request:PutItemRequest)(implicit credentials:AwsCredentials) = post("PutItem", request, response[Option[PutItemResponse]])
	def apply(request:PutItemRequest)(implicit credentials:AwsCredentials) = putItem(request)
	
	/** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_DeleteItem.html */
	def deleteItem(request:DeleteItemRequest)(implicit credentials:AwsCredentials) = post("DeleteItem", request, response[Option[DeleteItemResponse]])
	def apply(request:DeleteItemRequest)(implicit credentials:AwsCredentials) = deleteItem(request)
}