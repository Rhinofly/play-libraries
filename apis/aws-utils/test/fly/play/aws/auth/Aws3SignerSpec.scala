package fly.play.aws.auth

import org.specs2.mutable._
import fly.play.aws.Aws
import java.util.Date
import play.api.http.Writeable

object Aws3SignerSpec extends Specification {

  val fakeCredentials = SimpleAwsCredentials("fakeKeyId", "fakeSecret", Some("securityToken"))

  "Aws3Signer" should {

    val signer = Aws3Signer(fakeCredentials)

    "add the correct headers" in {

      val headers = signer.addHeaders(Map[String, Seq[String]](), "dynamodb.us-east-1.amazonaws.com", "Tue, 22 May 2012 21:13:19 UTC")

      headers must_== Map(
        "Host" -> Seq("dynamodb.us-east-1.amazonaws.com"),
        "X-Amz-Date" -> Seq("Tue, 22 May 2012 21:13:19 UTC"),
        "Date" -> Seq("Tue, 22 May 2012 21:13:19 UTC"),
        "X-Amz-Security-Token" -> Seq("securityToken"))

    }

    "create the correct canonical request and signed headers" in {

      val queryString = Map[String, Seq[String]]()

      val headers = Map(
        "Host" -> Seq("dynamodb.us-east-1.amazonaws.com"),
        "X-Amz-Date" -> Seq("Tue, 22 May 2012 21:13:19 UTC"),
        "Date" -> Seq("Tue, 22 May 2012 21:13:19 UTC"),
        "X-Amz-Security-Token" -> Seq("securityToken"))

      val (signedHeaders, cannonicalRequest) = signer.createCannonicalRequest("POST", None, queryString, headers, Some("test" getBytes "UTF-8"))

      "signed headers" in {
        signedHeaders must_== "host;x-amz-date;x-amz-security-token"
      }

      "cannonical request" in {
        cannonicalRequest must_==
          "POST\n" +
          "/\n" +
          "\n" +
          "host:dynamodb.us-east-1.amazonaws.com\n" +
          "x-amz-date:Tue, 22 May 2012 21:13:19 UTC\n" +
          "x-amz-security-token:securityToken\n" +
          "\n" +
          "test"
      }

    }

    def getBytes[T](value:T)(implicit wrt: Writeable[T]):Array[Byte] = wrt transform value

    "should be able to handle a zero length string" in {
      val queryString = Map[String, Seq[String]]()

      val headers = Map(
        "Host" -> Seq("dynamodb.us-east-1.amazonaws.com"),
        "X-Amz-Date" -> Seq("Tue, 22 May 2012 21:13:19 UTC"),
        "Date" -> Seq("Tue, 22 May 2012 21:13:19 UTC"),
        "X-Amz-Security-Token" -> Seq("securityToken"))

      val (signedHeaders, cannonicalRequest) = signer.createCannonicalRequest("POST", None, queryString, headers, Some(getBytes("")))

      "signed headers" in {
        signedHeaders must_== "host;x-amz-date;x-amz-security-token"
      }

      "cannonical request" in {
        cannonicalRequest must_==
          "POST\n" +
          "/\n" +
          "\n" +
          "host:dynamodb.us-east-1.amazonaws.com\n" +
          "x-amz-date:Tue, 22 May 2012 21:13:19 UTC\n" +
          "x-amz-security-token:securityToken\n" +
          "\n" +
          ""
      }
    }
  }
}