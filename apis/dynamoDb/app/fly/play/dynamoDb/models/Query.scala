package fly.play.dynamoDb.models

import play.api.libs.json.{Writes, JsObject, JsValue}
import play.api.libs.json.Json.toJson

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

case class Condition(comparisonOperator: ComparisonOperator, attributeValueList: Option[Seq[AttributeValue]]) {
  comparisonOperator match {
    case EQ | LE | LT | GE | GT | NE | CONTAINS | NOT_CONTAINS => require(attributeValueList match {
      case Some(Seq(SimpleAttributeValue(_, _))) => true
      case _ => false
    }, "The operator " + comparisonOperator + " requires that the attribute value list has a size of 1 and that it contains 1 simple attribute value")
    case BEGINS_WITH => require(attributeValueList match {
    	case Some(Seq(SimpleAttributeValue(S, _))) => true
    	case _ => false
    }, "The operator BEGINS_WITH requires that the attribute value list has a size of 1 and that it contains a string attribute value")
    case BETWEEN => require(attributeValueList match {
      case Some(Seq(AttributeValue(S, _), AttributeValue(S, _))) => true
      case Some(Seq(AttributeValue(N, _), AttributeValue(N, _))) => true
      case _ => false
    }, "The operator BETWEEN requires that the attribute value list has a size of 2 and that it contains two number or two string values")
    case NOT_NULL | NULL => require(attributeValueList match {
      case None => true
      case _ => false
    }, "The operator " + comparisonOperator + " requires that no attribute value list is specified")
    case IN => require(attributeValueList match {
      case Some(x) if (x.size > 0 && x.forall(_.isInstanceOf[SimpleAttributeValue])) => true
      case _ => false
    }, "The operator IN requires that the attribute value list has a size of at least 1 and that it contains simple value attributes")
  }
}

object Condition extends ((ComparisonOperator, Option[Seq[AttributeValue]]) => Condition) {
  implicit object ConditionWrites extends Writes[Condition] with JsonUtils {
    def writes(c: Condition): JsValue = JsObject(Seq(
      "ComparisonOperator" -> toJson(c.comparisonOperator)) ++
      optional("AttributeValueList" -> c.attributeValueList))
  }
}