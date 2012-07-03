package fly.play.dynamoDb.models

import play.api.libs.json.{Format, JsValue, JsObject, Reads, JsString, JsArray, Writes}
import play.api.libs.json.Json.toJson

sealed trait AttributeType {
  def value: String
}

object AttributeType {
  implicit object AttributeTypetatusFormat extends Format[AttributeType] {
    def reads(json: JsValue) = AttributeType(json.as[String])
    def writes(a: AttributeType): JsValue = toJson(a.value)
  }

  def apply(value: String) = value match {
    case "S" => S
    case "N" => N
    case "NS" => NS
    case "SS" => SS
    case _ => throw new Exception("Could not create AttributeType from '" + value + "'")
  }
}

abstract class SimpleAttributeType(val value: String) extends AttributeType
abstract class SeqAttributeType(val value: String) extends AttributeType

case object S extends SimpleAttributeType("S")
case object N extends SimpleAttributeType("N")
case object NS extends SeqAttributeType("NS")
case object SS extends SeqAttributeType("SS")

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

case class AttributeExpectation(exists: Boolean, value: Option[AttributeValue] = None) {
  require((exists && value.isDefined) || (!exists && value.isEmpty), "If exists is false, value should be None. If exists is true, value should be Some")
}

object AttributeExpectation extends ((Boolean, Option[AttributeValue]) => AttributeExpectation) {
  implicit object AttributeExpectationWrites extends Writes[AttributeExpectation] with JsonUtils {
    def writes(a: AttributeExpectation): JsValue = JsObject(Seq(
      "Exists" -> toJson(a.exists)) ++
      optional("Value" -> a.value))
  }
}

sealed abstract class AttributeUpdateAction(val value: String)

object AttributeUpdateAction {
  implicit object AttributeUpdateActionWrites extends Writes[AttributeUpdateAction] {
    def writes(a: AttributeUpdateAction): JsValue = toJson(a.value)
  }
}

case object PUT extends AttributeUpdateAction("PUT")
case object DELETE extends AttributeUpdateAction("DELETE")
case object ADD extends AttributeUpdateAction("ADD")

case class AttributeUpdate(value: AttributeValue, action: AttributeUpdateAction = PUT) {
  require(action != DELETE || (value match {
    case SeqAttributeValue(_, value) if value.size > 0 => true
    case _ => false
  }), "Delete action only works when providing a seq type attribute that is not empty")

  require(action != ADD || (value match {
    case SimpleAttributeValue(S, _) => false
    case _ => true
  }), "Add action does not work for simple string type attributes")
}

object AttributeUpdate extends ((AttributeValue, AttributeUpdateAction) => AttributeUpdate) {
  implicit object AttributeUpdateWrites extends Writes[AttributeUpdate] {
    def writes(a: AttributeUpdate): JsValue = JsObject(Seq(
      "Value" -> toJson(a.value),
      "Action" -> toJson(a.action)))
  }
}