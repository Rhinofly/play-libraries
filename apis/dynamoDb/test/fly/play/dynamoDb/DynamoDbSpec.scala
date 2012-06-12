package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import fly.play.sts.AwsSessionCredentials
import fly.play.aws.auth.AwsCredentials
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue

object DynamoDbSpec extends Specification with Before {
  def f = FakeApplication(new java.io.File("./test/"))
  def before = play.api.Play.start(f)

  "DynamoDb" in {
    "list tables" in {
      "ListTableRequest should create correct json" in {
        toJson(ListTablesRequest()) must_== toJson(Map[String, JsValue]())
        toJson(ListTablesRequest(limit = Some(10))) must_== parse("""{ "Limit":10 }""")
        toJson(ListTablesRequest(exclusiveStartTableName = Some("Test"))) must_== parse("""{"ExclusiveStartTableName":"Test"}""")
        toJson(ListTablesRequest(Some(10), Some("Test"))) must_== parse("""{"Limit":10, "ExclusiveStartTableName":"Test"}""")
      }

      "ListTableResponse should be created from json" in {
        fromJson[ListTablesResponse](parse("""{"TableNames":["Table1","Table2","Table3"], "LastEvaluatedTableName":"Table3"}""")) must beLike {
          case ListTablesResponse(Seq("Table1", "Table2", "Table3"), Some("Table3")) => ok
        }
        fromJson[ListTablesResponse](parse("""{"TableNames":["Table1","Table2","Table3"]}""")) must beLike {
          case ListTablesResponse(Seq("Table1", "Table2", "Table3"), None) => ok
        }

      }

      "make a successfull request" in {
        DynamoDb.listTables(ListTablesRequest(Some(10))).value.get must beLike {
          case Right(ListTablesResponse(Seq("Test"), None)) => ok
        }
        DynamoDb(ListTablesRequest(Some(10))).value.get must beLike {
          case Right(ListTablesResponse(Seq("Test"), None)) => ok
        }
      }
    }

    "create table" in {
      "ProvisionedThroughput" in {
        "should create correct json" in {
          toJson(ProvisionedThroughPut()) must_== parse("""{"ReadCapacityUnits":10,"WriteCapacityUnits":5}""")
          toJson(ProvisionedThroughPut(10, 5)) must_== parse("""{"ReadCapacityUnits":5,"WriteCapacityUnits":10}""")
        }
      }
      "Attribute" in {
        "should throw an assertion exception" >> {
          Attribute("a" * 256, S) must throwA[IllegalArgumentException]
          Attribute("", S) must throwA[IllegalArgumentException]
        }
        "should create correct json" in {
          toJson(Attribute("AttributeName1", S)) must_== parse("""{"AttributeName":"AttributeName1","AttributeType":"S"}""")
        }
      }
      "KeySchema" in {
        "should create correct json" in {
          toJson(KeySchema(Attribute("AttributeName1", S))) must_== parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"}}""")
          toJson(KeySchema(Attribute("AttributeName1", S), Some(Attribute("AttributeName2", N)))) must_== parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}}""")
        }
      }
      "CreateTableRequest" in {
        "should throw an assertion exception" >> {
          CreateTableRequest("a", KeySchema(Attribute("key", S)), ProvisionedThroughPut()) must throwA[IllegalArgumentException]
          CreateTableRequest("a" * 256, KeySchema(Attribute("key", S)), ProvisionedThroughPut()) must throwA[IllegalArgumentException]
		  CreateTableRequest("a$a", KeySchema(Attribute("key", S)), ProvisionedThroughPut()) must throwA[IllegalArgumentException]
        }
        "should create correct json" in {
          toJson(CreateTableRequest(
              "Table1", 
              KeySchema(Attribute("AttributeName1", S), Some(Attribute("AttributeName2", N))),
              ProvisionedThroughPut(10, 5))) must_== parse("""{"TableName":"Table1", "KeySchema": {"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"}, "RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}}, "ProvisionedThroughput":{"ReadCapacityUnits":5,"WriteCapacityUnits":10}}""")
        }
      }
    }
  }
}