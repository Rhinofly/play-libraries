package fly.play.dynamoDb

import fly.play.dynamoDb.models.KeySchema
import fly.play.dynamoDb.models.TableDescription
import fly.play.dynamoDb.models.CreateTableRequest
import play.api.libs.concurrent.Promise
import fly.play.dynamoDb.models.ProvisionedThroughput
import fly.play.aws.auth.AwsCredentials
import fly.play.dynamoDb.models.ListTablesRequest
import scala.collection.immutable.Stream
import fly.play.dynamoDb.models.ListTablesResponse
import fly.play.dynamoDb.utils.StreamUtils

object DynamoDb {
  def createTable(name: String, keySchema: KeySchema, provisionedThroughput: ProvisionedThroughput = ProvisionedThroughput())(implicit credentials: AwsCredentials): Promise[Either[DynamoDbException, TableDescription]] =
    LowLevelDynamoDb(CreateTableRequest(name, keySchema, provisionedThroughput)).map(_.right.map(_.description))

  /**
   * Returns a Stream of table names in case the result did not return all tables
   */
  def listTables(limit: Int)(implicit credentials: AwsCredentials): Promise[Stream[Either[DynamoDbException, String]]] =
    listTables(Some(limit), None)

  def listTables(limit: Option[Int] = None, exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[Either[DynamoDbException, String]]] =
    TableStream(limit, exclusiveStartTableName)

  object TableStream {

    def apply(limit: Option[Int], exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[Either[DynamoDbException, String]]] =
      apply(limit, exclusiveStartTableName, t => t.map(Right(_)).toStream, e => Seq(Left(e)).toStream)

    def apply[T](limit: Option[Int], exclusiveStartTableName: Option[String], seqConverter: Seq[String] => Stream[T], errorConverter: DynamoDbException => Stream[T])(implicit credentials: AwsCredentials): Promise[Stream[T]] =
      LowLevelDynamoDb(ListTablesRequest(limit, exclusiveStartTableName)).map {
        case Right(result) =>
          val s1 = seqConverter(result.tableNames)
          def s2 = {
            result.lastEvaluatedTableName.map { lastTableName =>
              apply(limit, Some(lastTableName), seqConverter, errorConverter).value.get
            }.getOrElse(Stream.empty[T])
          }

          StreamUtils.add(s1, s2)
        case Left(error) => errorConverter(error)
      }
  }

  object throwing {
    def listTables(limit: Int)(implicit credentials: AwsCredentials): Promise[Stream[String]] =
      listTables(Some(limit), None)

    def listTables(limit: Option[Int] = None, exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[String]] =
      TableStream(limit, exclusiveStartTableName)

    def createTable(name: String, keySchema: KeySchema, provisionedThroughput: ProvisionedThroughput = ProvisionedThroughput())(implicit credentials: AwsCredentials): Promise[TableDescription] =
      DynamoDb.createTable(name, keySchema, provisionedThroughput).map {
        case Right(result) => result
        case Left(error) => error.throwException
      }

    object TableStream {

      def apply(limit: Option[Int], exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[String]] =
        DynamoDb.TableStream(limit, exclusiveStartTableName, t => t.toStream, e => e.throwException)

    }

  }

}