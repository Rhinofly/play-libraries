package fly.play.dynamoDb.models

import play.api.libs.json.{Format, JsValue, JsObject, Reads, JsString, JsArray}
import play.api.libs.json.Json.toJson

case class Attribute(name: String, tpe: AttributeType) {
  require(name.length > 0, "The given name should have length")
  require(name.length < 256, "The given name has a length that exceeds 255")
}

object Attribute extends ((String, AttributeType) => Attribute) {
  implicit object AttributeFormat extends Format[Attribute] {
    def writes(a: Attribute): JsValue = JsObject(Seq(
      "AttributeName" -> toJson(a.name),
      "AttributeType" -> toJson(a.tpe)))

    def reads(json: JsValue) = Attribute(
      (json \ "AttributeName").as[String],
      (json \ "AttributeType").as[AttributeType])
  }
}


sealed abstract class AttributeValue {
  type ValueType

  def tpe: AttributeType
  def value: ValueType
}

object AttributeValue {

  implicit object AttributeValueFormat extends Format[AttributeValue] {

    implicit def mapReads[K, V](implicit fmtk: Reads[K], fmtv: Reads[V]): Reads[collection.immutable.Map[K, V]] = new Reads[collection.immutable.Map[K, V]] {
      def reads(json: JsValue) = json match {
        case JsObject(m) => m.map { case (k, v) => toJson(k).as[K] -> v.as[V] }.toMap
        case _ => throw new RuntimeException("Map expected")
      }
    }

    def reads(json: JsValue) = {
      val attributeFormat = json.as[Map[AttributeType, JsValue]]
      attributeFormat.head match {
        case (key: SimpleAttributeType, value: JsString) => AttributeValue(key, value.as[String])
        case (key: SeqAttributeType, value: JsArray) => AttributeValue(key, value.as[Seq[String]])
        case _ => throw new Exception("Invalid key - value combination: " + json)
      }
    }

    def writes(a: AttributeValue): JsValue = {

      val value = a match {
        case SimpleAttributeValue(_, value) => toJson(value)
        case SeqAttributeValue(_, value) => toJson(value)
      }

      JsObject(Seq(a.tpe.value -> value))
    }
  }

  def apply(tpe: SimpleAttributeType, value: String): AttributeValue = SimpleAttributeValue(tpe, value)
  def apply(tpe: SeqAttributeType, value: Seq[String]): AttributeValue = SeqAttributeValue(tpe, value)

  def unapply(a: AttributeValue): Option[(AttributeType, Any)] = if (a == null) None else Some((a.tpe, a.value))
}

case class SimpleAttributeValue(tpe: SimpleAttributeType, value: String) extends AttributeValue { type ValueType = String }
case class SeqAttributeValue(tpe: SeqAttributeType, value: Seq[String]) extends AttributeValue { type ValueType = Seq[String] }


case class Key(hashKeyElement: AttributeValue, rangeKeyElement: Option[AttributeValue] = None)

object Key extends ((AttributeValue, Option[AttributeValue]) => Key) {
  implicit object KeyFormat extends Format[Key] with JsonUtils {
    def writes(k: Key): JsValue = JsObject(Seq(
      "HashKeyElement" -> toJson(k.hashKeyElement)) ++
      optional("RangeKeyElement" -> k.rangeKeyElement))

    def reads(json: JsValue) = Key(
      (json \ "HashKeyElement").as[AttributeValue],
      (json \ "RangeKeyElement").as[Option[AttributeValue]])
  }
}
