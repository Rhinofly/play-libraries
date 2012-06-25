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
        case Right(Some(PutItemResponse(x: Map[_, _], _))) if (x == Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value1"))) => ok
      }
    }

    "update the item conditionally" in {
      DynamoDb(PutItemRequest("TestTable1", Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value3")), ALL_OLD, Some(Map("attribute1" -> AttributeExpectation(true, Some(AttributeValue(S, "value2"))))))).value.get must beLike {
        case Right(Some(PutItemResponse(x: Map[_, _], _))) if (x == Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value2"))) => ok
      }
    }

    "return a conditional error" in {
      DynamoDb(PutItemRequest("TestTable1", Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value4")), ALL_OLD, Some(Map("attribute1" -> AttributeExpectation(true, Some(AttributeValue(S, "value2"))))))).value.get must beLike {
        case Left(x: ConditionalCheckFailedException) => ok
      }
    }
  }

  "update item" should {
    "update an item" in {
      DynamoDb(UpdateItemRequest("TestTable1", Key(AttributeValue(S, "elem1")), Map("attribute1" -> AttributeUpdate(AttributeValue(S, "value4")), "attribute2" -> AttributeUpdate(AttributeValue(N, "3"), ADD)), ALL_NEW)).value.get must beLike {
        case Right(Some(UpdateItemResponse(x: Map[_, _], _))) if (x == Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value4"), "attribute2" -> AttributeValue(N, "3"))) => ok
      }
    }
  }

  "get item" should {
    "retrieve an item" in {
      DynamoDb(GetItemRequest("TestTable1", Key(AttributeValue(S, "elem1")), Some(List("attribute1")), true)).value.get must beLike {
        case Right(GetItemResponse(x: Map[_, _], _)) if (x == Map("attribute1" -> AttributeValue(S, "value4"))) => ok
      }
    }
  }

  "batch write item" should {
    "put and delete items" in {
      DynamoDb(PutItemRequest("TestTable1", Map("id" -> AttributeValue(S, "elem2"), "attribute1" -> AttributeValue(S, "value1")), NONE)).value.get must beLike {
        case Right(None) => ok
      }

      DynamoDb(BatchWriteItemRequest(Map("TestTable1" -> Seq(
        PutRequest(Map("id" -> AttributeValue(S, "elem3"), "attribute1" -> AttributeValue(SS, Seq("value1", "value2")))),
        DeleteRequest(Key(AttributeValue(S, "elem2"))))))).value.get must beLike {
        case Right(BatchWriteItemResponse(x: Map[_, _], y: Map[_, _])) => ok
      }
    }
  }

  "batch get item" should {
    "retrieve items" in {
      DynamoDb(BatchGetItemRequest(Map("TestTable1" -> GetRequest(
        Seq(Key(AttributeValue(S, "elem1")), Key(AttributeValue(S, "elem3"))),
        Some(Seq("id", "attribute1")))))).value.get must beLike {
        //Might fail because the ordering is not determined in a batch get
        case Right(BatchGetItemResponse(x: Map[_, _], y: Map[_, _])) if (x == Map("TestTable1" -> TableItems(Seq(
          Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value4")),
          Map("id" -> AttributeValue(S, "elem3"), "attribute1" -> AttributeValue(SS, Seq("value1", "value2")))), 1))) => ok
      }
    }
  }

  "query" should {
    "retrieve items" in {
      DynamoDb(QueryRequest("TestTable2", AttributeValue(S, "elem1"))).value.get must beLike {
        case Right(QueryResponse(3, Seq(item1: Map[_, _], item2: Map[_, _], item3: Map[_, _]), None, _)) if (
          item1 == Map("id" -> AttributeValue(S, "elem1"), "range" -> AttributeValue(N, "1")) &&
          item2 == Map("id" -> AttributeValue(S, "elem1"), "range" -> AttributeValue(N, "2")) &&
          item3 == Map("id" -> AttributeValue(S, "elem1"), "range" -> AttributeValue(N, "3"))) => ok
      }
    }
  }

  "scan" should {
    "retrieve items" >> {
      DynamoDb(ScanRequest("TestTable2", None, Some(Map("range" -> Condition(GT, Some(Seq(AttributeValue(N, "1")))))), None, true)).value.get must beLike {
        case Right(ScanResponse(2, None, None, _, 3)) => ok
      }
      DynamoDb(ScanRequest("TestTable2", None, Some(Map("range" -> Condition(GT, Some(Seq(AttributeValue(N, "1")))))))).value.get must beLike {
        case Right(ScanResponse(2, Some(Seq(item1: Map[_, _], item2: Map[_, _])), None, _, 3)) => ok
      }
    }
  }

  "delete item" should {
    "delete an item" in {
      DynamoDb(DeleteItemRequest("TestTable1", Key(AttributeValue(S, "elem1")), ALL_OLD)).value.get must beLike {
        case Right(Some(DeleteItemResponse(x: Map[_, _], _))) if (x == Map("id" -> AttributeValue(S, "elem1"), "attribute1" -> AttributeValue(S, "value4"), "attribute2" -> AttributeValue(N, "3"))) => ok
      }
    }
  }

}