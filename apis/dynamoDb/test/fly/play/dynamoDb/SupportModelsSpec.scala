package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue
import java.util.Date

import models._

object SupportModelsSpec extends Specification {

  "ProvisionedThroughput" should {
    "create correct json" in {
      toJson(ProvisionedThroughput()) must_== parse("""{"ReadCapacityUnits":10,"WriteCapacityUnits":5}""")
      toJson(ProvisionedThroughput(10, 5)) must_== parse("""{"ReadCapacityUnits":5,"WriteCapacityUnits":10}""")
    }
    "be created from json" in {
      fromJson[ProvisionedThroughput](parse("""{"ReadCapacityUnits":5,"WriteCapacityUnits":10}""")) must beLike {
        case ProvisionedThroughput(10, 5, None, None) => ok
      }
      fromJson[ProvisionedThroughput](parse("""{"LastIncreaseDateTime": 1.309988345372E9, "LastDecreaseDateTime": 1.209988345372E9, "ReadCapacityUnits":10,"WriteCapacityUnits":10}""")) must beLike {
        case ProvisionedThroughput(10, 10, Some(x: Date), Some(y: Date)) if (x.getTime == (1.309988345372E9).toLong && y.getTime == (1.209988345372E9).toLong) => ok
      }
    }
  }

  "Attribute" should {
    "throw an assertion exception" >> {
      Attribute("a" * 256, S) must throwA[IllegalArgumentException]
      Attribute("", S) must throwA[IllegalArgumentException]
    }
    "create correct json" in {
      toJson(Attribute("AttributeName1", S)) must_== parse("""{"AttributeName":"AttributeName1","AttributeType":"S"}""")
    }
    "be created from json" in {
      fromJson[Attribute](parse("""{"AttributeName":"AttributeName1","AttributeType":"S"}""")) must beLike {
        case Attribute("AttributeName1", S) => ok
      }
    }
  }

  "KeySchema" should {
    "create correct json" in {
      toJson(KeySchema(Attribute("AttributeName1", S))) must_== parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"}}""")
      toJson(KeySchema(Attribute("AttributeName1", S), Some(Attribute("AttributeName2", N)))) must_== parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}}""")
    }
    "be created from json" in {
      fromJson[KeySchema](parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"}}""")) must beLike {
        case KeySchema(x: Attribute, None) => ok
      }
      fromJson[KeySchema](parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}}""")) must beLike {
        case KeySchema(x: Attribute, Some(y: Attribute)) => ok
      }
    }
  }

  "TableDescription" should {
    "be created from json" in {
      fromJson[TableDescription](parse("""{"CreationDateTime":1.310506263362E9,"KeySchema":{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}},"ProvisionedThroughput":{"ReadCapacityUnits":5, "WriteCapacityUnits":10 },"TableName":"Table1","TableStatus":"CREATING"}""")) must beLike {
        case TableDescription("Table1", CREATING, Some(d), Some(k: KeySchema), p: ProvisionedThroughput) if (d.getTime == (1.310506263362E9).toLong) => ok
      }
    }
  }

  "Table" should {
    "be created from json" in {
      fromJson[Table](parse("""{"CreationDateTime":1309988345.372,"ItemCount":1,"KeySchema":{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}},"ProvisionedThroughput":{"LastIncreaseDateTime": 1.309988345372E9, "LastDecreaseDateTime": 1.309988345372E9, "ReadCapacityUnits":10,"WriteCapacityUnits":10},"TableName":"Table1","TableSizeBytes":1,"TableStatus":"ACTIVE"}""")) must beLike {
        case Table("Table1", ACTIVE, Some(d), Some(1), Some(1), Some(y: KeySchema), z: ProvisionedThroughput) if (d.getTime == (1309988345.372).toLong) => ok
      }
    }
  }

  "AttributeValue" should {
    "create correct json" >> {
      toJson(AttributeValue(S, "AttributeValue1")) must_== parse("""{"S":"AttributeValue1"}""")
      toJson(AttributeValue(SS, Seq("AttributeValue1", "AttributeValue2"))) must_== parse("""{"SS":["AttributeValue1", "AttributeValue2"]}""")
    }

    "be created from json" >> {
      fromJson[AttributeValue](parse("""{"S":"AttributeValue1"}""")) must beLike {
        case SimpleAttributeValue(tpe, value) => ok
      }
      fromJson[AttributeValue](parse("""{"SS":["AttributeValue1", "AttributeValue2"]}""")) must beLike {
        case SeqAttributeValue(tpe, value) => ok
      }
    }
  }

  "AttributeExpectation" should {
    "throw an assertion exception" >> {
      AttributeExpectation(false, Some(AttributeValue(S, "Yellow"))) must throwA[IllegalArgumentException]
      AttributeExpectation(true, None) must throwA[IllegalArgumentException]
    }

    "create correct json" >> {
      toJson(AttributeExpectation(true, Some(AttributeValue(S, "Yellow")))) must_== parse("""{"Exists":true,"Value":{"S":"Yellow"}}""")
      toJson(AttributeExpectation(false)) must_== parse("""{"Exists":false}""")
    }
  }

  "Key" should {
    "create correct json" >> {
      toJson(Key(AttributeValue(S, "AttributeValue1"))) must_== parse("""{"HashKeyElement":{"S":"AttributeValue1"}}""")
      toJson(Key(AttributeValue(S, "AttributeValue1"), Some(AttributeValue(N, "AttributeValue2")))) must_== parse("""{"HashKeyElement":{"S":"AttributeValue1"},"RangeKeyElement":{"N":"AttributeValue2"}}""")
    }
  }

  "AttributeUpdate" should {
    "throw an assertion exception" >> {
      AttributeUpdate(AttributeValue(SS, Seq.empty), DELETE) must throwA[IllegalArgumentException]
      AttributeUpdate(AttributeValue(S, ""), ADD) must throwA[IllegalArgumentException]
    }
    "create correct json" in {
      toJson(AttributeUpdate(AttributeValue(N, "10"), ADD)) must_== parse("""{"Value":{"N":"10"},"Action":"ADD"}""")
    }
  }

  "PutRequest" should {
    "create correct json" in {
      toJson[BatchRequest](BatchPutRequest(Map("ReplyDateTime" -> AttributeValue(S, "2012-04-03T11:04:47.034Z"), "Id" -> AttributeValue(S, "Amazon DynamoDB#DynamoDB Thread 5")))) must_== parse("""{"PutRequest":{"Item":{"ReplyDateTime":{"S":"2012-04-03T11:04:47.034Z"},"Id":{"S":"Amazon DynamoDB#DynamoDB Thread 5"}}}}""")
    }
  }

  "DeleteRequest" should {
    "create correct json" in {
      toJson[BatchRequest](BatchDeleteRequest(Key(AttributeValue(S, "Amazon DynamoDB#DynamoDB Thread 4"), Some(AttributeValue(S, "oops - accidental row"))))) must_== parse("""{"DeleteRequest":{"Key":{"HashKeyElement":{"S":"Amazon DynamoDB#DynamoDB Thread 4"},"RangeKeyElement":{"S":"oops - accidental row"}}}}""")
    }
  }

  "BatchRequest" should {
    "be created from json" >> {
      fromJson[BatchRequest](parse("""{
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
         }""")) must beLike {
        case BatchDeleteRequest(Key(SimpleAttributeValue(S, "Amazon DynamoDB#DynamoDB Thread 4"), Some(SimpleAttributeValue(S, "oops - accidental row")))) => ok
      }
      fromJson[BatchRequest](parse("""{
        "PutRequest":{
          "Item":{
            "ReplyDateTime":{
              "S":"2012-04-03T11:04:47.034Z"
            },
            "Id":{
              "S":"Amazon DynamoDB#DynamoDB Thread 5"
            }
          }
        }
      }""")) must beLike {
        case BatchPutRequest(x: Map[_, _]) if (x == Map("ReplyDateTime" -> AttributeValue(S, "2012-04-03T11:04:47.034Z"), "Id" -> AttributeValue(S, "Amazon DynamoDB#DynamoDB Thread 5"))) => ok
      }
    }
  }

  "GetRequest" should {
    "create correct json" in {
      toJson(BatchGetRequest(Seq(
        Key(AttributeValue(S, "KeyValue1"), Some(AttributeValue(N, "KeyValue2"))),
        Key(AttributeValue(S, "KeyValue3"), Some(AttributeValue(N, "KeyValue4"))),
        Key(AttributeValue(S, "KeyValue5"), Some(AttributeValue(N, "KeyValue6")))),
        Some(Seq("AttributeName1", "AttributeName2", "AttributeName3")))) must_== parse("""{"Keys": 
            [{"HashKeyElement": {"S":"KeyValue1"}, "RangeKeyElement":{"N":"KeyValue2"}},
            {"HashKeyElement": {"S":"KeyValue3"}, "RangeKeyElement":{"N":"KeyValue4"}},
            {"HashKeyElement": {"S":"KeyValue5"}, "RangeKeyElement":{"N":"KeyValue6"}}],
        "AttributesToGet":["AttributeName1", "AttributeName2", "AttributeName3"]}""")
    }
    "be created from json" in {
      fromJson[BatchGetRequest](parse("""{"Keys": 
            [{"HashKeyElement": {"S":"KeyValue1"}, "RangeKeyElement":{"N":"KeyValue2"}},
            {"HashKeyElement": {"S":"KeyValue3"}, "RangeKeyElement":{"N":"KeyValue4"}},
            {"HashKeyElement": {"S":"KeyValue5"}, "RangeKeyElement":{"N":"KeyValue6"}}],
        "AttributesToGet":["AttributeName1", "AttributeName2", "AttributeName3"]}""")) must beLike {
        case BatchGetRequest(Seq(
          Key(SimpleAttributeValue(S, "KeyValue1"), Some(SimpleAttributeValue(N, "KeyValue2"))),
          Key(SimpleAttributeValue(S, "KeyValue3"), Some(SimpleAttributeValue(N, "KeyValue4"))),
          Key(SimpleAttributeValue(S, "KeyValue5"), Some(SimpleAttributeValue(N, "KeyValue6")))),
          Some(Seq("AttributeName1", "AttributeName2", "AttributeName3"))) => ok
      }
    }
  }

  "TableItems" should {
    "be created from json" in {
      fromJson[TableItems](parse("""{"Items":
        [{"AttributeName1": {"S":"AttributeValue"},
        "AttributeName2": {"N":"AttributeValue"},
        "AttributeName3": {"SS":["AttributeValue", "AttributeValue", "AttributeValue"]}
        },
        {"AttributeName1": {"S": "AttributeValue"},
        "AttributeName2": {"S": "AttributeValue"},
        "AttributeName3": {"NS": ["AttributeValue", "AttributeValue", "AttributeValue"]}
        }],
    	"ConsumedCapacityUnits":1}""")) must beLike {
        case TableItems(Seq(x, y), 1) if (x == Map(
          "AttributeName1" -> AttributeValue(S, "AttributeValue"),
          "AttributeName2" -> AttributeValue(N, "AttributeValue"),
          "AttributeName3" -> AttributeValue(SS, Seq("AttributeValue", "AttributeValue", "AttributeValue"))) && y == Map(
            "AttributeName1" -> AttributeValue(S, "AttributeValue"),
            "AttributeName2" -> AttributeValue(S, "AttributeValue"),
            "AttributeName3" -> AttributeValue(NS, Seq("AttributeValue", "AttributeValue", "AttributeValue")))) => ok
      }
    }
  }

  "Condition" should {
    "throw an assertion exception" >> {
      Condition(EQ, Some(Seq(AttributeValue(SS, Seq("a", "b"))))) must throwA[IllegalArgumentException]
      Condition(EQ, Some(Seq(AttributeValue(S, "a"), AttributeValue(S, "b")))) must throwA[IllegalArgumentException]
      Condition(BEGINS_WITH, Some(Seq(AttributeValue(N, "1")))) must throwA[IllegalArgumentException]
      Condition(BETWEEN, Some(Seq(AttributeValue(N, "1")))) must throwA[IllegalArgumentException]
      Condition(BETWEEN, Some(Seq(AttributeValue(N, "1"), AttributeValue(S, "1")))) must throwA[IllegalArgumentException]
      Condition(GT, None) must throwA[IllegalArgumentException]
      Condition(NOT_NULL, Some(Seq())) must throwA[IllegalArgumentException]
      Condition(IN, None) must throwA[IllegalArgumentException]
      Condition(IN, Some(Seq(AttributeValue(SS, Seq("a", "b"))))) must throwA[IllegalArgumentException]
    }
    "create correct Json" >> {
      toJson(Condition(GT, Some(Seq(AttributeValue(N, "AttributeValue2"))))) must_== parse("""{"ComparisonOperator":"GT", "AttributeValueList":[{"N":"AttributeValue2"}]}""")
      toJson(Condition(BETWEEN, Some(Seq(AttributeValue(S, "AttributeValue1"), AttributeValue(S, "AttributeValue2"))))) must_== parse("""{"ComparisonOperator":"BETWEEN", "AttributeValueList":[{"S":"AttributeValue1"},{"S":"AttributeValue2"}]}""")
    }
  }
}