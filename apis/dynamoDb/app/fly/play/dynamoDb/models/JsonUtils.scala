package fly.play.dynamoDb.models

import play.api.libs.json.{Writes, JsValue}
import play.api.libs.json.Json.toJson

trait JsonUtils {
  def optional[T](key:(String, Option[T]))(implicit wrt: Writes[T]):Option[(String, JsValue)] =
    key._2 map (value => key._1 -> toJson(value))
}