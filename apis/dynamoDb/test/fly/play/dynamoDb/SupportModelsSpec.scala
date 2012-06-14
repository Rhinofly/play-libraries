package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import fly.play.sts.AwsSessionCredentials
import fly.play.aws.auth.AwsCredentials
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue
import java.util.Date

object SupportModelsSpec extends Specification {

  "ProvisionedThroughput" in {
    "should create correct json" in {
      toJson(ProvisionedThroughput()) must_== parse("""{"ReadCapacityUnits":10,"WriteCapacityUnits":5}""")
      toJson(ProvisionedThroughput(10, 5)) must_== parse("""{"ReadCapacityUnits":5,"WriteCapacityUnits":10}""")
    }
    "should be created from json" in {
      fromJson[ProvisionedThroughput](parse("""{"ReadCapacityUnits":5,"WriteCapacityUnits":10}""")) must beLike {
        case ProvisionedThroughput(10, 5, None, None) => ok
      }
      fromJson[ProvisionedThroughput](parse("""{"LastIncreaseDateTime": 1.309988345372E9, "LastDecreaseDateTime": 1.209988345372E9, "ReadCapacityUnits":10,"WriteCapacityUnits":10}""")) must beLike {
        case ProvisionedThroughput(10, 10, Some(x: Date), Some(y: Date)) if (x.getTime == (1.309988345372E9).toLong && y.getTime == (1.209988345372E9).toLong) => ok
      }
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
    "should be created from json" in {
      fromJson[Attribute](parse("""{"AttributeName":"AttributeName1","AttributeType":"S"}""")) must beLike {
        case Attribute("AttributeName1", S) => ok
      }
    }
  }

  "KeySchema" in {
    "should create correct json" in {
      toJson(KeySchema(Attribute("AttributeName1", S))) must_== parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"}}""")
      toJson(KeySchema(Attribute("AttributeName1", S), Some(Attribute("AttributeName2", N)))) must_== parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}}""")
    }
    "should be created from json" in {
      fromJson[KeySchema](parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"}}""")) must beLike {
        case KeySchema(x: Attribute, None) => ok
      }
      fromJson[KeySchema](parse("""{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}}""")) must beLike {
        case KeySchema(x: Attribute, Some(y: Attribute)) => ok
      }
    }
  }

  "TableDescription" in {
    "should be created from json" in {
      fromJson[TableDescription](parse("""{"CreationDateTime":1.310506263362E9,"KeySchema":{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}},"ProvisionedThroughput":{"ReadCapacityUnits":5, "WriteCapacityUnits":10 },"TableName":"Table1","TableStatus":"CREATING"}""")) must beLike {
        case TableDescription("Table1", CREATING, Some(d), Some(k: KeySchema), p: ProvisionedThroughput) if (d.getTime == (1.310506263362E9).toLong) => ok
      }
    }
  }

  "Table" in {
    "should be created from json" in {
      fromJson[Table](parse("""{"CreationDateTime":1309988345.372,"ItemCount":1,"KeySchema":{"HashKeyElement":{"AttributeName":"AttributeName1","AttributeType":"S"},"RangeKeyElement":{"AttributeName":"AttributeName2","AttributeType":"N"}},"ProvisionedThroughput":{"LastIncreaseDateTime": 1.309988345372E9, "LastDecreaseDateTime": 1.309988345372E9, "ReadCapacityUnits":10,"WriteCapacityUnits":10},"TableName":"Table1","TableSizeBytes":1,"TableStatus":"ACTIVE"}""")) must beLike {
        case Table("Table1", ACTIVE, Some(d), Some(1), Some(1), Some(y: KeySchema), z: ProvisionedThroughput) if (d.getTime == (1309988345.372).toLong) => ok
      }
    }
  }
}