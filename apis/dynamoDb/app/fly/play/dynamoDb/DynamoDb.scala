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
import fly.play.dynamoDb.models.CreateTableResponse
import fly.play.dynamoDb.models.DeleteTableRequest
import fly.play.dynamoDb.models.DescribeTableRequest
import fly.play.dynamoDb.models.TableStatus
import fly.play.dynamoDb.models.ACTIVE

object DynamoDb {

  /**
   * Calls listTables(Some(limit), None)
   *
   * @see listTables(limit, exclusiveStartTableName)
   */
  def listTables(limit: Int)(implicit credentials: AwsCredentials): Promise[Stream[Either[DynamoDbException, String]]] =
    listTables(Some(limit), None)

  /**
   * Returns a Stream of 'either' table names or errors. This stream ensures
   * that all tables (up to the limit) are returned, even if it needs to make
   * more requests.
   */
  def listTables(limit: Option[Int] = None, exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[Either[DynamoDbException, String]]] =
    createTableStream(limit, exclusiveStartTableName)

  /**
   * Utility method to create a Stream of table objects
   */
  def createTableStream(limit: Option[Int], exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[Either[DynamoDbException, String]]] =
    createTableStream(limit, exclusiveStartTableName, t => t.map(Right(_)).toStream, e => Seq(Left(e)).toStream)

  /**
   * Utility method to create a Stream of table objects
   */
  def createTableStream[T](limit: Option[Int], exclusiveStartTableName: Option[String], seqConverter: Seq[String] => Stream[T], errorConverter: DynamoDbException => Stream[T])(implicit credentials: AwsCredentials): Promise[Stream[T]] =
    LowLevelDynamoDb(ListTablesRequest(limit, exclusiveStartTableName)).map {
      case Right(result) =>
        val s1 = seqConverter(result.tableNames)
        def s2 = {
          result.lastEvaluatedTableName.map { lastTableName =>
            createTableStream(limit, Some(lastTableName), seqConverter, errorConverter).value.get
          }.getOrElse(Stream.empty[T])
        }

        StreamUtils.add(s1, s2)
      case Left(error) => errorConverter(error)
    }

  /**
   * Creates a table with the given name and KeySchema
   */
  def createTable(name: String, keySchema: KeySchema, provisionedThroughput: ProvisionedThroughput = ProvisionedThroughput())(implicit credentials: AwsCredentials): Promise[Either[DynamoDbException, TableDescription]] =
    LowLevelDynamoDb(CreateTableRequest(name, keySchema, provisionedThroughput)).map(_.right.map(_.description))

  /**
   * Creates a table and waits for the table to become active. Note that this can
   * take quite some time.
   */
  def createActiveTable(name: String, keySchema: KeySchema, provisionedThroughput: ProvisionedThroughput = ProvisionedThroughput(), whileWaiting: Option[TableStatus => Unit] = None, checkInterval: Int = 2000)(implicit credentials: AwsCredentials): Promise[Either[DynamoDbException, TableDescription]] = {
    createTable(name, keySchema, provisionedThroughput).flatMap {
      case Right(tableDescription) if (tableDescription.status != ACTIVE) =>
        waitForTableStatus(name, ACTIVE, whileWaiting, checkInterval)
      case x => Promise.pure(x)
    }
  }

  /**
   * Waits until the table is in the specified status. This might take quite some time.
   */
  def waitForTableStatus(name: String, status: TableStatus, whileWaiting: Option[TableStatus => Unit] = None, checkInterval: Int = 2000): Promise[Either[DynamoDbException, TableDescription]] =
    describeTable(name)
      .flatMap {
        case Right(tableDescription) if (tableDescription.status != status) => {
          whileWaiting.foreach(_(tableDescription.status))
          Thread sleep checkInterval
          waitForTableStatus(name, status, whileWaiting, checkInterval)
        }
        case x =>
          Promise.pure(x)
      }

  /**
   * Deletes a table with the given name
   */
  def deleteTable(name: String): Promise[Either[DynamoDbException, TableDescription]] =
    LowLevelDynamoDb(DeleteTableRequest(name)).map(_.right.map(_.description))

  def describeTable(name: String): Promise[Either[DynamoDbException, TableDescription]] =
    LowLevelDynamoDb(DescribeTableRequest(name)).map(_.right.map(_.tableDescription))

  /**
   * The methods in this sub object throw an exception instead of returning an Either[DynamoDbException, ...]
   */
  object throwing {

    def listTables(limit: Int)(implicit credentials: AwsCredentials): Promise[Stream[String]] =
      listTables(Some(limit), None)

    def listTables(limit: Option[Int] = None, exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[String]] =
      createTableStream(limit, exclusiveStartTableName)

    def createTableStream(limit: Option[Int], exclusiveStartTableName: Option[String] = None)(implicit credentials: AwsCredentials): Promise[Stream[String]] =
      DynamoDb.createTableStream(limit, exclusiveStartTableName, t => t.toStream, e => e.throwException)

    def createTable(name: String, keySchema: KeySchema, provisionedThroughput: ProvisionedThroughput = ProvisionedThroughput())(implicit credentials: AwsCredentials): Promise[TableDescription] =
      DynamoDb.createTable(name, keySchema, provisionedThroughput).map {
        case Right(result) => result
        case Left(error) => error.throwException
      }

    def deleteTable(name: String): Promise[TableDescription] =
      DynamoDb.deleteTable(name).map {
        case Right(result) => result
        case Left(error) => error.throwException
      }

  }

}