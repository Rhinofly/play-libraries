package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import fly.play.sts.AwsSessionCredentials
import fly.play.aws.auth.AwsCredentials
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue
import java.util.Date

object RequestResponseModelsSpec extends Specification {

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

  "CreateTableRequest" in {
    "should throw an assertion exception" >> {
      CreateTableRequest("a", KeySchema(Attribute("key", S)), ProvisionedThroughput()) must throwA[IllegalArgumentException]
      CreateTableRequest("a" * 256, KeySchema(Attribute("key", S)), ProvisionedThroughput()) must throwA[IllegalArgumentException]
      CreateTableRequest("a$a", KeySchema(Attribute("key", S)), ProvisionedThroughput()) must throwA[IllegalArgumentException]
    }
    "should create correct json" in {
      toJson(CreateTableRequest(
        "Table1",
        KeySchema(Attribute("AttributeName1", S), Some(Attribute("AttributeName2", N))),
        ProvisionedThroughput(10, 5))) must_== parse("""{"TableName":"Table1", "KeySchema": {"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"}, "RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}}, "ProvisionedThroughput":{"ReadCapacityUnits":5,"WriteCapacityUnits":10}}""")
    }
  }

  "CreateTableResponse should be created from json" in {
    fromJson[CreateTableResponse](parse("""{"TableDescription":{"CreationDateTime":1.310506263362E9,"KeySchema":{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}},"ProvisionedThroughput":{"ReadCapacityUnits":5, "WriteCapacityUnits":10 },"TableName":"Table1","TableStatus":"CREATING"}}""")) must beLike {
      case CreateTableResponse(x: TableDescription) => ok
    }
  }

  "DeleteTableRequest should create correct json" in {
    toJson(DeleteTableRequest("Table1")) must_== parse("""{"TableName":"Table1"}""")
  }

  "DeleteTableResponse should be created from json" in {
    fromJson[DeleteTableResponse](parse("""{"TableDescription":{"CreationDateTime":1.313362508446E9,"KeySchema":{"HashKeyElement":{"AttributeName":"user","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"time","AttributeType":"N"}},"ProvisionedThroughput":{"ReadCapacityUnits":10,"WriteCapacityUnits":10},"TableName":"Table1","TableStatus":"DELETING"}}""")) must beLike {
      case DeleteTableResponse(x: TableDescription) => ok
    }
  }

  "DescribeTableRequest should create correct json" in {
    toJson(DescribeTableRequest("Table1")) must_== parse("""{"TableName":"Table1"}""")
  }

  "DescribeTableResponse should be created from json" in {
    fromJson[DescribeTableResponse](parse("""{"Table":{"CreationDateTime":1.309988345372E9,"ItemCount":1,"KeySchema":{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}},"ProvisionedThroughput":{"LastIncreaseDateTime": 1.309988345372E9, "LastDecreaseDateTime": 1.309988345372E9, "ReadCapacityUnits":10,"WriteCapacityUnits":10},"TableName":"Table1","TableSizeBytes":1,"TableStatus":"ACTIVE"}}""")) must beLike {
      case DescribeTableResponse(x: Table) => ok
    }
  }
}