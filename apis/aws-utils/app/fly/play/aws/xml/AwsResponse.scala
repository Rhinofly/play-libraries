package fly.play.aws.xml

import play.api.libs.ws.Response

object AwsResponse {
  def apply[T](converter: (Int, Response) => T)(response: Response): Either[AwsError, T] =
    response.status match {
      case status if status >= 200 && status < 300 => Right(converter(status, response))
      case status => Left(
          if (response.body.isEmpty) AwsError(status, "unknown error", "no body", None) else AwsError(response.status, response.xml))
    }
}