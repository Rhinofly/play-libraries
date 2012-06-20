package fly.play.dynamoDb

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import fly.play.sts.AwsSessionCredentials
import fly.play.aws.auth.AwsCredentials
import play.api.libs.json.Json.{ toJson, parse, fromJson }
import play.api.libs.json.JsValue
import java.util.Date
import org.specs2.specification.Example

object DynamoDbItemSpec extends Specification with Before {

  sequential

  def f = FakeApplication(new java.io.File("./test/"))
  def before = play.api.Play.start(f)

  "put item" should {

    "create an item" in {
      DynamoDb(PutItemRequest("TestTable1", Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value1")), NONE, Some(Map("attribute1" -> AttributeExpectation(false))))).value.get must beLike {
        case Right(None) => ok
      }
    }
    
    "update the item" in {
    	DynamoDb(PutItemRequest("TestTable1", Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value2")), ALL_OLD)).value.get must beLike {
    	case Right(Some(PutItemResponse(x: Map[_, _], _))) if (x == Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value1"))) =>  ok
    	}
    }

    "update the item conditionally" in {
    	DynamoDb(PutItemRequest("TestTable1", Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value3")), ALL_OLD, Some(Map("attribute1" -> AttributeExpectation(true, Some(AttributeValue(S, "value2"))))))).value.get must beLike {
    	case Right(Some(PutItemResponse(x: Map[_, _], _))) if (x == Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value2"))) => ok
    	}
    }
    
    "return a conditional error" in {
    	DynamoDb(PutItemRequest("TestTable1", Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value4")), ALL_OLD, Some(Map("attribute1" -> AttributeExpectation(true, Some(AttributeValue(S, "value2"))))))).value.get must beLike {
    	case Left(x:ConditionalCheckFailedException) => ok
    	}
    }
  }
  
  "delete item" should {
    "delete an item" in {
      DynamoDb(DeleteItemRequest("TestTable1", Key(AttributeValue(S, "elem1")), ALL_OLD)).value.get must beLike {
        case Right(Some(DeleteItemResponse(x:Map[_, _], _))) if (x == Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value3"))) => ok
      }
    }
  }
}