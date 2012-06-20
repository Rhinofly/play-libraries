package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import fly.play.sts.AwsSessionCredentials
import fly.play.aws.auth.AwsCredentials
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue
import java.util.Date
import org.specs2.specification.Example

object DynamoDbSpec extends Specification with Before {

  sequential

  def f = FakeApplication(new java.io.File("./test/"))
  def before = play.api.Play.start(f)

  def waitForStatus(name: String, status: TableStatus, example: => Example): Example = DynamoDb.describeTable(DescribeTableRequest(name)).value.get match {
    case Right(DescribeTableResponse(Table(_, status, _, _, _, _, _))) if (status == ACTIVE) =>
      println("found " + name + " in " + status + " state, performing action")
      example
    case Right(DescribeTableResponse(Table(_, status, _, _, _, _, _))) =>
      Thread sleep 1000
      println("found " + name + " in " + status + " state, waiting")
      waitForStatus(name, status, example)
    case Left(ResourceNotFoundException(_)) | Right(_) =>
      Thread sleep 1000
      println("Test does not yet exist, waiting for table " + name + " to be " + status)
      waitForStatus(name, status, example)
    case Left(error) =>
      failure("error calling describe table => " + error.getClass.getName + ": " + error.message)
  }

  "create table" should {
    println("create table")

    val createRequest = CreateTableRequest("Test", KeySchema(Attribute("test", S)))

    "create a Test table" in {
      println("create table - create a Test table")

      DynamoDb.createTable(createRequest).value.get must beLike {
        case Right(CreateTableResponse(
          TableDescription("Test", CREATING, Some(x: Date),
            Some(KeySchema(Attribute("test", S), None)),
            ProvisionedThroughput(5, 10, None, None)))) => ok
      }
    }

    "create a Test2 table" in {
      println("create table - create a Test2 table")

      DynamoDb(CreateTableRequest("Test2", KeySchema(Attribute("string", S), Some(Attribute("number", N))))).value.get must beLike {
        case Right(CreateTableResponse(
          TableDescription("Test2", CREATING, Some(x: Date),
            Some(KeySchema(Attribute("string", S), Some(Attribute("number", N)))),
            ProvisionedThroughput(5, 10, None, None)))) => ok
      }

    }
    "return an exception when trying to create the same table" in {
      println("create table - return an exception when trying to create the same table")

      DynamoDb.createTable(createRequest).value.get must beLike {
        case Left(ResourceInUseException(_)) => ok
      }
    }
  }

  "list tables" should {
    println("list tables")

    "make a successfull request" in {
      println("list tables - make a successfull request")

      DynamoDb.listTables(ListTablesRequest(Some(10))).value.get must beLike {
        case Right(ListTablesResponse(Seq("Test", "Test2"), None)) => ok
      }
      DynamoDb(ListTablesRequest(Some(10))).value.get must beLike {
        case Right(ListTablesResponse(Seq("Test", "Test2"), None)) => ok
      }

    }
  }

  "describe table" should {
    println("describe table")

    "return information for Test" in {
      println("describe table - return information for Test")

      DynamoDb.describeTable(DescribeTableRequest("Test")).value.get must beLike {
        case Right(DescribeTableResponse(Table("Test", _, _, _, _, _, _))) => ok
      }
    }
    "return information for Test2" in {
      println("describe table - return information for Test2")

      DynamoDb(DescribeTableRequest("Test2")).value.get must beLike {
        case Right(DescribeTableResponse(Table("Test2", _, _, _, _, _, _))) => ok
      }

    }
  }

  "update table" should {
    println("update table")

    "wait for table to be active and update the throughput of Test table" in {
      println("update table - wait for table to be active and update the throughput of Test table")

      waitForStatus("Test", ACTIVE, {
        println("updating table")
        DynamoDb(UpdateTableRequest("Test", ProvisionedThroughput(10, 2))).value.get must beLike {
          case Right(UpdateTableResponse(TableDescription("Test", UPDATING, _, _, ProvisionedThroughput(2, 2, Some(x), Some(y))))) => ok
        }
      })
      ok
    }
  }

  "put item" should {
    println("put item")

    "wait for table to be active and put an item" in {
      println("put item - wait for table to be active and put an item")

      waitForStatus("Test", ACTIVE, {
        println("table active")
        ok
      })
      DynamoDb(PutItemRequest("Test", Map("test" -> AttributeValue(S, "soep"), "test2" -> AttributeValue(S, "kip")))).value.get must beLike {
        case Right(None) => {
          println("Got No response")
          ok
        }
      }
      println("putting item")
      DynamoDb(PutItemRequest("Test", Map("test" -> AttributeValue(S, "soep"), "test2" -> AttributeValue(S, "kip2")), ALL_OLD)).value.get must beLike {
        case Right(Some(PutItemResponse(x: Map[String, AttributeValue], _))) if (x == Map("test" -> AttributeValue(S, "soep"), "test2" -> AttributeValue(S, "kip"))) => {
          println("got response")
          ok
        }
      }
      println("putting item")
      DynamoDb(PutItemRequest("Test", Map("test" -> AttributeValue(S, "soep"), "test2" -> AttributeValue(S, "kip3")), ALL_OLD, Some(Map("test2" -> AttributeExpectation(true, Some(AttributeValue(S, "kip2"))))))).value.get must beLike {
        case Right(Some(PutItemResponse(x: Map[String, AttributeValue], _))) if (x == Map("test" -> AttributeValue(S, "soep"), "test2" -> AttributeValue(S, "kip2"))) => ok
      }

      ok
    }
  }

  "delete table" should {
    println("delete table")

    def deleteTable(name: String): Example = DynamoDb(DeleteTableRequest(name)).value.get match {
      case Right(DeleteTableResponse(TableDescription(_, DELETING, _, _, _))) => {
        println("deleted table " + name)
        ok
      }
      case Left(error) => failure("error calling delete table => " + error.getClass.getName + ": " + error.message)
      case Right(x) => failure("unexpected response from delete: " + x)
    }

    "wait for Test1 table to be active using describe table and delete the table" in {
      println("wait for Test1 table to be active using describe table and delete the table")

      waitForStatus("Test", ACTIVE, deleteTable("Test"))
      ok

    }
    "wait for Test2 table to be active using describe table and delete the table" in {
      println("wait for Test2 table to be active using describe table and delete the table")

      waitForStatus("Test2", ACTIVE, deleteTable("Test2"))
      ok
    }
  }

  "cleanup" should {
    println("cleanup")
    def checkRemoval(name: String): Example = DynamoDb.describeTable(DescribeTableRequest(name)).value.get match {
      case Right(DescribeTableResponse(Table(_, status, _, _, _, _, _))) => status match {
        case DELETING => {
          Thread sleep 1000
          println("waiting for table " + name + " to be removed")
          checkRemoval(name)
        }
        case x => failure("Unexpected status: " + x)
      }
      case Left(ResourceNotFoundException(_)) => ok
      case Left(error) => {
        failure(error.message)
      }
    }

    "wait for table Test to have been deleted" in {
      println("waiting for table Test to have been deleted")
      checkRemoval("Test")
      ok
    }
    "wait for table Test2 to have been deleted" in {
      println("waiting for table Test2 to have been deleted")
      checkRemoval("Test2")
      ok
    }
  }

}