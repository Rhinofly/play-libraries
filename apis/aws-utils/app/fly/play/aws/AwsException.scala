package fly.play.aws

case class AwsException(status: Int, code: String, message: String, extraInfo:Option[String])
  extends Exception("Status: " + status + ", code: " + code + ", message: " + message + extraInfo.map(", extra info:\n" + _).getOrElse(""))