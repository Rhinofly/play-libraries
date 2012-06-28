package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue
import java.util.Date
import org.specs2.specification.Example
import fly.play.aws.auth.AwsCredentials
import org.apache.http.MethodNotSupportedException

import models._

object DynamoDbTableSpec extends Specification with Before {

  sequential

  def f = FakeApplication(new java.io.File("./test/"))
  def before = play.api.Play.start(f)

  def waitForStatus(name: String, status: TableStatus, example: => Example): Example = LowLevelDynamoDb(DescribeTableRequest(name)).value.get match {
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

  "low level dynamo db" should {
    
	  "give a credentials error" in {
		  implicit val credentials = AwsCredentials("fake", "credentials")
				  LowLevelDynamoDb(CreateTableRequest("Test", KeySchema(Attribute("test", S)))).value.get must beLike {
				  case Left(x: InvalidClientTokenIdException) => ok
		  }
	  }
  }
  
  /*
  	Uncomment if you want to run table related tests (these take quite some time)
  	
  
  "create table" should {
    println("create table")

    val createRequest = CreateTableRequest("Test", KeySchema(Attribute("test", S)))

    "create a Test table" in {
      println("create table - create a Test table")

      LowLevelDynamoDb(createRequest).value.get must beLike {
        case Right(CreateTableResponse(
          TableDescription("Test", CREATING, Some(x: Date),
            Some(KeySchema(Attribute("test", S), None)),
            ProvisionedThroughput(5, 10, None, None)))) => ok
      }
    }

    "create a Test2 table" in {
      println("create table - create a Test2 table")

      LowLevelDynamoDb(CreateTableRequest("Test2", KeySchema(Attribute("string", S), Some(Attribute("number", N))))).value.get must beLike {
        case Right(CreateTableResponse(
          TableDescription("Test2", CREATING, Some(x: Date),
            Some(KeySchema(Attribute("string", S), Some(Attribute("number", N)))),
            ProvisionedThroughput(5, 10, None, None)))) => ok
      }

    }
    "return an exception when trying to create the same table" in {
      println("create table - return an exception when trying to create the same table")

      LowLevelDynamoDb(createRequest).value.get must beLike {
        case Left(ResourceInUseException(_)) => ok
      }
    }
  }

  "list tables" should {
    println("list tables")

    "make a successfull request" in {
      println("list tables - make a successfull request")

      LowLevelDynamoDb(ListTablesRequest(Some(10))).value.get must beLike {
        case Right(ListTablesResponse(Seq("Test", "Test2", "TestTable1", "TestTable2"), None)) => ok
      }

    }
  }

  "describe table" should {
    println("describe table")

    "return information for Test" in {
      println("describe table - return information for Test")

      LowLevelDynamoDb(DescribeTableRequest("Test")).value.get must beLike {
        case Right(DescribeTableResponse(Table("Test", _, _, _, _, _, _))) => ok
      }
    }
    "return information for Test2" in {
      println("describe table - return information for Test2")

      LowLevelDynamoDb(DescribeTableRequest("Test2")).value.get must beLike {
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
        LowLevelDynamoDb(UpdateTableRequest("Test", ProvisionedThroughput(10, 2))).value.get must beLike {
          case Right(UpdateTableResponse(TableDescription("Test", UPDATING, _, _, ProvisionedThroughput(2, 2, Some(x), Some(y))))) => ok
        }
      })
      ok
    }
  }

  "delete table" should {
    println("delete table")

    def deleteTable(name: String): Example = LowLevelDynamoDb(DeleteTableRequest(name)).value.get match {
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
    def checkRemoval(name: String): Example = LowLevelDynamoDb(DescribeTableRequest(name)).value.get match {
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
  */
}