package fly.play.dynamoDb.models

import play.api.libs.json.{JsValue, Format, Reads, Writes}
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


sealed abstract class TableStatus(val value: String)

object TableStatus {
  implicit object TableStatusReads extends Reads[TableStatus] {
    def reads(json: JsValue) = json.as[String] match {
      case "CREATING" => CREATING
      case "ACTIVE" => ACTIVE
      case "DELETING" => DELETING
      case "UPDATING" => UPDATING
      case _ => throw new Exception("Could not create TableStatus from '" + json + "'")
    }
  }
}

case object CREATING extends TableStatus("CREATING")
case object ACTIVE extends TableStatus("ACTIVE")
case object DELETING extends TableStatus("DELETING")
case object UPDATING extends TableStatus("UPDATING")


sealed abstract class ReturnValuesType(val value: String)

object ReturnValuesType {
  implicit object ReturnValuesTypeWrites extends Writes[ReturnValuesType] {
    def writes(r: ReturnValuesType): JsValue = toJson(r.value)
  }
}

case object NONE extends ReturnValuesType("NONE")
case object ALL_OLD extends ReturnValuesType("ALL_OLD")
case object ALL_NEW extends ReturnValuesType("ALL_NEW")
case object UPDATED_OLD extends ReturnValuesType("UPDATED_OLD")
case object UPDATED_NEW extends ReturnValuesType("UPDATED_NEW")


sealed abstract class AttributeUpdateAction(val value: String)

object AttributeUpdateAction {
  implicit object AttributeUpdateActionWrites extends Writes[AttributeUpdateAction] {
    def writes(a: AttributeUpdateAction): JsValue = toJson(a.value)
  }
}

case object PUT extends AttributeUpdateAction("PUT")
case object DELETE extends AttributeUpdateAction("DELETE")
case object ADD extends AttributeUpdateAction("ADD")


sealed abstract class ComparisonOperator(val value: String)

object ComparisonOperator {
  implicit object ComparisonOperatorWrites extends Writes[ComparisonOperator] {
    def writes(a: ComparisonOperator): JsValue = toJson(a.value)
  }
}

case object EQ extends ComparisonOperator("EQ")
case object LE extends ComparisonOperator("LE")
case object LT extends ComparisonOperator("LT")
case object GE extends ComparisonOperator("GE")
case object GT extends ComparisonOperator("GT")
case object BEGINS_WITH extends ComparisonOperator("BEGINS_WITH")
case object BETWEEN extends ComparisonOperator("BETWEEN")
case object NE extends ComparisonOperator("NE")
case object NOT_NULL extends ComparisonOperator("NOT_NULL")
case object NULL extends ComparisonOperator("NULL")
case object CONTAINS extends ComparisonOperator("CONTAINS")
case object NOT_CONTAINS extends ComparisonOperator("NOT_CONTAINS")
case object IN extends ComparisonOperator("IN")