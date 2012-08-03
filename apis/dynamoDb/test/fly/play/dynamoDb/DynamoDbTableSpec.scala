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
import play.api.libs.concurrent.Promise
import org.specs2.matcher.MatchResult
import org.specs2.execute.ResultLike

object DynamoDbTableSpec extends Specification with Before {

  sequential

  def f = FakeApplication(new java.io.File("./test/"))
  def before = play.api.Play.start(f)

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
  	
  */
  "create table" should {
    println("create table")
    val createRequest = CreateTableRequest("Test", KeySchema(Attribute("test", S)))

    "create a Test table" in {
      println("create table - create a Test table")

      LowLevelDynamoDb(createRequest).value.get must beLike {
        case Right(CreateTableResponse(
          TableDescription("Test", CREATING, Some(x: Date),
            Some(KeySchema(Attribute("test", S), None)),
            ProvisionedThroughput(5, 10, None, None), None, None))) => ok
      }
    }

    "create a Test2 table and wait for it's creation" in {
      println("create table - create a Test2 table and wait for it's creation")

      val result = DynamoDb.createActiveTable(
        "Test2",
        KeySchema(Attribute("string", S), Some(Attribute("number", N))),
        whileWaiting = Some(status => println("Found Test2 in status " + status + ", waiting")))
        .await(60000).get must beLike {
          case Right(
            TableDescription("Test2", ACTIVE, Some(x: Date),
              Some(KeySchema(Attribute("string", S), Some(Attribute("number", N)))),
              ProvisionedThroughput(5, 10, None, None), Some(0), Some(0))) => {
                println("Test2 table created")
               ok 
              }
        }

      println("moving to next test")
      
      result
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
        case Right(DescribeTableResponse(TableDescription("Test", _, _, _, _, _, _))) => ok
      }
    }
    "return information for Test2" in {
      println("describe table - return information for Test2")

      LowLevelDynamoDb(DescribeTableRequest("Test2")).value.get must beLike {
        case Right(DescribeTableResponse(TableDescription("Test2", _, _, _, _, _, _))) => ok
      }

    }
  }

  "update table" should {
    println("update table")

    "wait for table to be active and update the throughput of Test table" in {
      println("update table - wait for table to be active and update the throughput of Test table")

      DynamoDb.waitForTableStatus(
        "Test",
        ACTIVE,
        whileWaiting = Some(status => println("Found table Test in state " + status + ", waiting")))
        .await(60000).get must beLike {
          case Right(t) => LowLevelDynamoDb(UpdateTableRequest("Test", ProvisionedThroughput(6, 3))).value.get must beLike {
            case Right(UpdateTableResponse(TableDescription("Test", UPDATING, _, _, _, _, _))) => ok
          }
        }
    }
  }

  "delete table" should {
    println("delete table")

    def deleteTable(name: String): MatchResult[_] = LowLevelDynamoDb(DeleteTableRequest(name)).value.get match {
      case Right(DeleteTableResponse(TableDescription(_, DELETING, _, _, _, None, None))) => {
        println("deleted table " + name)
        ok
      }
      case Left(error) => failure("error calling delete table => " + error.getClass.getName + ": " + error.message)
      case Right(x) => failure("unexpected response from delete: " + x)
    }

    "wait for Test table to be active using describe table and delete the table" in {
      println("wait for Test1 table to be active using describe table and delete the table")

      DynamoDb.waitForTableStatus(
        "Test",
        ACTIVE,
        whileWaiting = Some(status => println("Found table Test in state " + status + ", waiting")))
        .await(60000).get must beLike {
          case Right(t) => deleteTable(t.name)
        }
    }

    "delete the table Test2 table" in {
      println("delete the table Test2 table")

      DynamoDb.waitForTableStatus(
        "Test2",
        ACTIVE,
        whileWaiting = Some(status => println("Found table Test2 in state " + status + ", waiting")))
        .await(60000).get.fold(
          error => failure(error.toString),
          t => deleteTable("Test2"))
    }
  }

  "cleanup" should {
    println("cleanup")
    def checkRemoval(name: String): Example = LowLevelDynamoDb(DescribeTableRequest(name)).value.get match {
      case Right(DescribeTableResponse(TableDescription(_, status, _, _, _, _, _))) => status match {
        case DELETING => {
          Thread sleep 3000
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