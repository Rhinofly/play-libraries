package fly.play.aws.auth

import org.specs2.mutable._

object Aws4SignerSpec extends Specification {

  val fakeCredentials = SimpleAwsCredentials("fakeKeyId", "fakeSecret")

  "Aws4Signer" should {

    val signer = Aws4Signer(fakeCredentials, Some("sts"))

    "add the correct headers" in {

      val headers = signer.addHeaders(Map[String, Seq[String]](), "sts.amazonaws.com", "20120519T004356Z", None)

      headers must_== Map(
        "Host" -> Seq("sts.amazonaws.com"),
        "X-Amz-Date" -> Seq("20120519T004356Z"))

    }

    "create the correct canonical request and signed headers" in {

      val queryString = Map("Version" -> Seq("2011-06-15"),
        "Action" -> Seq("GetSessionToken"),
        "DurationSeconds" -> Seq("3600"))

      val headers = Map(
        "Host" -> Seq("sts.amazonaws.com"),
        "X-Amz-Date" -> Seq("20120519T004356Z"))

      val (signedHeaders, cannonicalRequest) = signer.createCannonicalRequest("GET", None, queryString, headers, None)

      "signed headers" in {
    	  signedHeaders must_== "host;x-amz-date"
      }

      "cannonical request" in {
    	  cannonicalRequest must_==
    			  "GET\n" +
    					  "/\n" +
    					  "Action=GetSessionToken&DurationSeconds=3600&Version=2011-06-15\n" +
    					  "host:sts.amazonaws.com\n" +
    					  "x-amz-date:20120519T004356Z\n" +
    					  "\n" +
    					  "host;x-amz-date\n" +
    					  "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
      }
    }
    
    "create the correct string to sign" in {
      
      val cannonicalRequest =
        "GET\n" +
        "/\n" +
        "Action=GetSessionToken&DurationSeconds=3600&Version=2011-06-15\n" +
        "host:sts.amazonaws.com\n" +
        "x-amz-date:20120519T004356Z\n" +
        "\n" +
        "host;x-amz-date\n" +
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
      
      val stringToSign = signer.createStringToSign("20120519T004356Z", cannonicalRequest, "20120519/us-east-1/sts/aws4_request")
      
      stringToSign must_== 
        "AWS4-HMAC-SHA256\n" +
        "20120519T004356Z\n" +
        "20120519/us-east-1/sts/aws4_request\n" +
        "ec19857897328f82cfb526a6bae44824ad717e58272c3a018545b658ceba425d"
    }
  }
}