package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import fly.play.sts.AwsSessionCredentials
import fly.play.aws.auth.AwsCredentials
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue
import java.util.Date

object RequestResponseModelsSpec extends Specification {

  "ListTableRequest should create correct json" >> {
    toJson(ListTablesRequest()) must_== toJson(Map[String, JsValue]())
    toJson(ListTablesRequest(limit = Some(10))) must_== parse("""{ "Limit":10 }""")
    toJson(ListTablesRequest(exclusiveStartTableName = Some("Test"))) must_== parse("""{"ExclusiveStartTableName":"Test"}""")
    toJson(ListTablesRequest(Some(10), Some("Test"))) must_== parse("""{"Limit":10, "ExclusiveStartTableName":"Test"}""")
  }

  "ListTableResponse should be created from json" >> {
    fromJson[ListTablesResponse](parse("""{"TableNames":["Table1","Table2","Table3"], "LastEvaluatedTableName":"Table3"}""")) must beLike {
      case ListTablesResponse(Seq("Table1", "Table2", "Table3"), Some("Table3")) => ok
    }
    fromJson[ListTablesResponse](parse("""{"TableNames":["Table1","Table2","Table3"]}""")) must beLike {
      case ListTablesResponse(Seq("Table1", "Table2", "Table3"), None) => ok
    }

  }

  "CreateTableRequest" should {
    "throw an assertion exception" >> {
      CreateTableRequest("a", KeySchema(Attribute("key", S)), ProvisionedThroughput()) must throwA[IllegalArgumentException]
      CreateTableRequest("a" * 256, KeySchema(Attribute("key", S)), ProvisionedThroughput()) must throwA[IllegalArgumentException]
      CreateTableRequest("a$a", KeySchema(Attribute("key", S)), ProvisionedThroughput()) must throwA[IllegalArgumentException]
    }
    "create correct json" in {
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
    toJson(DeleteTableRequest("Table1")) must_== parse("""
        {
    		"TableName":"Table1"
        }
        """)
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

  "UpdateTableRequest should create correct json" in {
    toJson(UpdateTableRequest("Table1", ProvisionedThroughput(15, 5))) must_== parse("""{"TableName":"Table1","ProvisionedThroughput":{"ReadCapacityUnits":5,"WriteCapacityUnits":15}}""")
  }

  "UpdateTableResponse should be created from json" in {
    fromJson[UpdateTableResponse](parse("""{"TableDescription":{"CreationDateTime":1.321657838135E9,"KeySchema":{"HashKeyElement":{"AttributeName":"AttributeValue1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeValue2","AttributeType":"N"}},"ProvisionedThroughput":{"LastDecreaseDateTime":1.321661704489E9,"LastIncreaseDateTime":1.321663607695E9,"ReadCapacityUnits":5,"WriteCapacityUnits":10},"TableName":"Table1","TableStatus":"UPDATING"}}""")) must beLike {
      case UpdateTableResponse(x: TableDescription) => ok
    }
  }

  "PutItemRequest" should {
    "throw an assertion exception" in {
      PutItemRequest("Table1",
        Map("AttributeName1" -> AttributeValue(S, "AttributeValue1"),
          "AttributeName2" -> AttributeValue(N, "AttributeValue2")),
        ALL_NEW) must throwA[IllegalArgumentException]
    }
    "create correct json" >> {
      toJson(
        PutItemRequest("Table1",
          Map("AttributeName1" -> AttributeValue(S, "AttributeValue1"),
            "AttributeName2" -> AttributeValue(N, "AttributeValue2")),
          ALL_OLD,
          Some(Map("AttributeName3" -> AttributeExpectation(true, Some(AttributeValue(S, "AttributeValue"))))))) must_== parse("""{"TableName":"Table1","Item":{"AttributeName1":{"S":"AttributeValue1"},"AttributeName2":{"N":"AttributeValue2"}},"ReturnValues":"ALL_OLD","Expected":{"AttributeName3":{"Exists":true,"Value": {"S":"AttributeValue"}}}}""")
      toJson(
        PutItemRequest("Table1",
          Map("AttributeName1" -> AttributeValue(S, "AttributeValue1"),
            "AttributeName2" -> AttributeValue(N, "AttributeValue2")))) must_== parse("""{"TableName":"Table1","Item":{"AttributeName1":{"S":"AttributeValue1"},"AttributeName2":{"N":"AttributeValue2"}},"ReturnValues":"NONE"}""")
    }
  }

  "PutItemResponse should be created from json" in {
    fromJson[PutItemResponse](parse("""{"Attributes":{"AttributeName3":{"S":"AttributeValue3"},"AttributeName2":{"SS":["AttributeValue2"]},"AttributeName1":{"SS":["AttributeValue1"]}},"ConsumedCapacityUnits":1}""")) must beLike {
      case PutItemResponse(x: Map[_, _], 1) => ok
    }
  }

  "DeleteItemRequest" should {
    "throw an assertion exception" in {
      DeleteItemRequest("Table1",
        Key(AttributeValue(S, "AttributeValue1"), Some(AttributeValue(N, "AttributeValue2"))),
        ALL_NEW) must throwA[IllegalArgumentException]
    }
    "create correct json" >> {
      toJson(
        DeleteItemRequest("Table1",
          Key(AttributeValue(S, "AttributeValue1"), Some(AttributeValue(N, "AttributeValue2"))),
          ALL_OLD,
          Some(Map("AttributeName3" -> AttributeExpectation(true, Some(AttributeValue(S, "AttributeValue3"))))))) must_== parse("""{"TableName":"Table1","Key":{"HashKeyElement":{"S":"AttributeValue1"},"RangeKeyElement":{"N":"AttributeValue2"}},"ReturnValues":"ALL_OLD","Expected":{"AttributeName3":{"Exists":true, "Value":{"S":"AttributeValue3"}}}}""")
      toJson(
        DeleteItemRequest("Table1",
          Key(AttributeValue(S, "AttributeValue1"), Some(AttributeValue(N, "AttributeValue2"))))) must_== parse("""{"TableName":"Table1","Key":{"HashKeyElement":{"S":"AttributeValue1"},"RangeKeyElement":{"N":"AttributeValue2"}},"ReturnValues":"NONE"}""")
    }
  }

  "DeleteItemResponse should be created from json" in {
    fromJson[DeleteItemResponse](parse("""{"Attributes":{"AttributeName3":{"S":"AttributeValue3"},"AttributeName2":{"SS":["AttributeValue2"]},"AttributeName1":{"SS":["AttributeValue1"]}},"ConsumedCapacityUnits":1}""")) must beLike {
      case DeleteItemResponse(x: Map[_, _], 1) => ok
    }
  }

  "UpdateItemRequest should create correct json" in {
    toJson(
      UpdateItemRequest("Table1",
        Key(AttributeValue(S, "AttributeValue1"), Some(AttributeValue(N, "AttributeValue2"))),
        Map("AttributeName3" -> AttributeUpdate(AttributeValue(S, "AttributeValue3_New"))),
        UPDATED_NEW,
        Some(Map("AttributeName3" -> AttributeExpectation(true, Some(AttributeValue(S, "AttributeValue3_Current"))))))) must_== parse("""{"TableName":"Table1","Key":{"HashKeyElement":{"S":"AttributeValue1"},"RangeKeyElement":{"N":"AttributeValue2"}},"AttributeUpdates":{"AttributeName3":{"Value":{"S":"AttributeValue3_New"},"Action":"PUT"}},"ReturnValues":"UPDATED_NEW","Expected":{"AttributeName3":{"Exists":true,"Value":{"S":"AttributeValue3_Current"}}}}""")
  }

  "UpdateItemResponse should be created from json" in {
    fromJson[UpdateItemResponse](parse("""{"Attributes":{"AttributeName1":{"S":"AttributeValue1"},"AttributeName2":{"S":"AttributeValue2"},"AttributeName3":{"S":"AttributeValue3"}},"ConsumedCapacityUnits":1}""")) must beLike {
      case UpdateItemResponse(x: Map[_, _], 1) => ok
    }
  }

  "GetItemRequest should create correct json" in {
    toJson(
      GetItemRequest("Table1",
        Key(AttributeValue(S, "AttributeValue1"), Some(AttributeValue(N, "AttributeValue2"))),
        Some(List("AttributeName3", "AttributeName4")),
        false)) must_== parse("""{"TableName":"Table1","Key":{"HashKeyElement": {"S":"AttributeValue1"},"RangeKeyElement": {"N":"AttributeValue2"}},"ConsistentRead":false,"AttributesToGet":["AttributeName3","AttributeName4"]}""")
  }

  "GetItemResponse should be created from json" in {
    fromJson[GetItemResponse](parse("""{"Item":{"AttributeName3":{"S":"AttributeValue3"},"AttributeName4":{"N":"AttributeValue4"}},"ConsumedCapacityUnits": 0.5}""")) must beLike {
      case GetItemResponse(x: Map[_, _], 0.5) => ok
    }
  }

  "BatchWriteItemRequest should create correct json" in {
    toJson(BatchWriteItemRequest(Map(
      "Reply" -> Seq[BatchRequest](
        PutRequest(Map(
          "ReplyDateTime" -> AttributeValue(S, "2012-04-03T11:04:47.034Z"),
          "Id" -> AttributeValue(S, "Amazon DynamoDB#DynamoDB Thread 5"))),
        DeleteRequest(Key(AttributeValue(S, "Amazon DynamoDB#DynamoDB Thread 4"), Some(AttributeValue(S, "oops - accidental row"))))),
      "Thread" -> Seq(
        PutRequest(Map(
          "ForumName" -> AttributeValue(S, "Amazon DynamoDB"),
          "Subject" -> AttributeValue(S, "DynamoDB Thread 5"))))))) must_== parse("""{"RequestItems":{"Reply":[{"PutRequest":{"Item":{"ReplyDateTime":{"S":"2012-04-03T11:04:47.034Z"},"Id":{"S":"Amazon DynamoDB#DynamoDB Thread 5"}}}},{"DeleteRequest":{"Key":{"HashKeyElement":{"S":"Amazon DynamoDB#DynamoDB Thread 4"},"RangeKeyElement":{"S":"oops - accidental row"}}}}],"Thread":[{"PutRequest":{"Item":{"ForumName":{"S":"Amazon DynamoDB"},"Subject":{"S":"DynamoDB Thread 5"}}}}]}}""")

  }

  "BatchWriteItemResponse should be created from json" in {
    fromJson[BatchWriteItemResponse](parse("""{
	   "Responses":{
	      "Thread":{
	         "ConsumedCapacityUnits":1.0
	      },
	      "Reply":{
	         "ConsumedCapacityUnits":1.0
	      }
	   },
	   "UnprocessedItems":{
	      "Reply":[
	         {
	            "DeleteRequest":{
	               "Key":{
	                  "HashKeyElement":{
	                     "S":"Amazon DynamoDB#DynamoDB Thread 4"
	                  },
	                  "RangeKeyElement":{
	                     "S":"oops - accidental row"
	                  }
	               }
	            }
	         }
	      ]
	   }
    }""")) must beLike {
      case BatchWriteItemResponse(x: Map[_, _], y: Map[_, _]) if (x == Map("Thread" -> 1, "Reply" -> 1) && y == Map("Reply" -> Seq(DeleteRequest(Key(AttributeValue(S, "Amazon DynamoDB#DynamoDB Thread 4"), Some(AttributeValue(S, "oops - accidental row"))))))) => ok
    }
  }

  "BatchGetItemRequest should create correct json" in {
    toJson(BatchGetItemRequest(Map(
      "Table1" -> GetRequest(Seq(
        Key(AttributeValue(S, "KeyValue1"), Some(AttributeValue(N, "KeyValue2"))),
        Key(AttributeValue(S, "KeyValue3"), Some(AttributeValue(N, "KeyValue4"))),
        Key(AttributeValue(S, "KeyValue5"), Some(AttributeValue(N, "KeyValue6")))),
        Some(Seq("AttributeName1", "AttributeName2", "AttributeName3"))),
      "Table2" -> GetRequest(Seq(
        Key(AttributeValue(S, "KeyValue4")),
        Key(AttributeValue(S, "KeyValue5"))),
        Some(Seq("AttributeName4", "AttributeName5", "AttributeName6")))))) must_== parse("""{"RequestItems":
	    {"Table1": 
	        {"Keys": 
	            [{"HashKeyElement": {"S":"KeyValue1"}, "RangeKeyElement":{"N":"KeyValue2"}},
	            {"HashKeyElement": {"S":"KeyValue3"}, "RangeKeyElement":{"N":"KeyValue4"}},
	            {"HashKeyElement": {"S":"KeyValue5"}, "RangeKeyElement":{"N":"KeyValue6"}}],
	        "AttributesToGet":["AttributeName1", "AttributeName2", "AttributeName3"]},
		    "Table2": 
		        {"Keys": 
		            [{"HashKeyElement": {"S":"KeyValue4"}}, 
		            {"HashKeyElement": {"S":"KeyValue5"}}],
		        "AttributesToGet": ["AttributeName4", "AttributeName5", "AttributeName6"]
		        }
		    }
    	}""")
  }

  "BatchGetItemResponse should be created from json" in {
    fromJson[BatchGetItemResponse](parse("""{"Responses":
	    {"Table1":
	        {"Items":
	        [{"AttributeName1": {"S":"AttributeValue"},
	        "AttributeName2": {"N":"AttributeValue"},
	        "AttributeName3": {"SS":["AttributeValue", "AttributeValue", "AttributeValue"]}
	        },
	        {"AttributeName1": {"S": "AttributeValue"},
	        "AttributeName2": {"S": "AttributeValue"},
	        "AttributeName3": {"NS": ["AttributeValue", "AttributeValue", "AttributeValue"]}
	        }],
	    "ConsumedCapacityUnits":1},
	    "Table2": 
	        {"Items":
	        [{"AttributeName1": {"S":"AttributeValue"},
	        "AttributeName2": {"N":"AttributeValue"},
	        "AttributeName3": {"SS":["AttributeValue", "AttributeValue", "AttributeValue"]}
	        },
	        {"AttributeName1": {"S": "AttributeValue"},
	        "AttributeName2": {"S": "AttributeValue"},
	        "AttributeName3": {"NS": ["AttributeValue", "AttributeValue","AttributeValue"]}
	        }],
	    "ConsumedCapacityUnits":1}
	    },
	    "UnprocessedKeys":
	        {"Table3": 
	        {"Keys": 
	            [{"HashKeyElement": {"S":"KeyValue1"}, "RangeKeyElement":{"N":"KeyValue2"}},
	            {"HashKeyElement": {"S":"KeyValue3"}, "RangeKeyElement":{"N":"KeyValue4"}},
	            {"HashKeyElement": {"S":"KeyValue5"}, "RangeKeyElement":{"N":"KeyValue6"}}],
	        "AttributesToGet":["AttributeName1", "AttributeName2", "AttributeName3"]}
	        }
    	}""")) must beLike {
      case BatchGetItemResponse(x: Map[_, _], y: Map[_, _]) => ok
    }
  }
}