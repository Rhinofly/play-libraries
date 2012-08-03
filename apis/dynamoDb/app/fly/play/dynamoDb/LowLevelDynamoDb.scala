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
import fly.play.aws.AwsException

import models._

object LowLevelDynamoDb {

  val version = "DynamoDB_20111205."
  val url = "https://dynamodb.us-east-1.amazonaws.com"

  /**
   * Creates session credentials from the given AwsCredentials
   */
  def sessionCredentials(implicit credentials: AwsCredentials) = credentials match {
    case x: AwsSessionCredentials => x
    case x => AwsSessionCredentials(x)
  }

  /**
   * Implicit object for ContentTypeOf[String] used specifically by Amazon DynamoDb
   */
  implicit object XAmzJson extends ContentTypeOf[String](Some("application/x-amz-json-1.0"))

  /**
   * Helper method to construct a response
   */
  def response[T](response: Response)(implicit rds: Reads[T]): Either[DynamoDbException, T] = response.status match {
    case 200 => Right(response.json.as[T])
    case n => Left(response.json.as[DynamoDbException])
  }

  /**
   * Makes a post to the DynamoDb service
   */
  def post[S, T](action: String, body: S, converter: Response => Either[DynamoDbException, T])(implicit wrt: Writes[S], credentials: AwsCredentials) =
    try {
      Aws
        .withSigner3(sessionCredentials)
        .url(url)
        .withHeaders("x-amz-target" -> (version + action))
        .post(toJson(body))
        .map(converter)

    } catch {
      case e: AwsException => Promise.pure(Left(DynamoDbException(e)))
      case e => throw e
    }

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_ListTables.html */
  def apply(request: ListTablesRequest)(implicit credentials: AwsCredentials) = post("ListTables", request, response[ListTablesResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_CreateTable.html */
  def apply(request: CreateTableRequest)(implicit credentials: AwsCredentials) = post("CreateTable", request, response[CreateTableResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_DeleteTable.html */
  def apply(request: DeleteTableRequest)(implicit credentials: AwsCredentials) = post("DeleteTable", request, response[DeleteTableResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_DescribeTables.html */
  def apply(request: DescribeTableRequest)(implicit credentials: AwsCredentials) = post("DescribeTable", request, response[DescribeTableResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_UpdateTable.html */
  def apply(request: UpdateTableRequest)(implicit credentials: AwsCredentials) = post("UpdateTable", request, response[UpdateTableResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_PutItem.html */
  def apply(request: PutItemRequest)(implicit credentials: AwsCredentials) = post("PutItem", request, response[Option[PutItemResponse]])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_DeleteItem.html */
  def apply(request: DeleteItemRequest)(implicit credentials: AwsCredentials) = post("DeleteItem", request, response[Option[DeleteItemResponse]])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_UpdateItem.html */
  def apply(request: UpdateItemRequest)(implicit credentials: AwsCredentials) = post("UpdateItem", request, response[Option[UpdateItemResponse]])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_GetItem.html */
  def apply(request: GetItemRequest)(implicit credentials: AwsCredentials) = post("GetItem", request, response[GetItemResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_BatchWriteItem.html */
  def apply(request: BatchWriteItemRequest)(implicit credentials: AwsCredentials) = post("BatchWriteItem", request, response[BatchWriteItemResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_BatchGetItems.html */
  def apply(request: BatchGetItemRequest)(implicit credentials: AwsCredentials) = post("BatchGetItem", request, response[BatchGetItemResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_Query.html */
  def apply(request: QueryRequest)(implicit credentials: AwsCredentials) = post("Query", request, response[QueryResponse])

  /** @see http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_Scan.html */
  def apply(request: ScanRequest)(implicit credentials: AwsCredentials) = post("Scan", request, response[ScanResponse])

}