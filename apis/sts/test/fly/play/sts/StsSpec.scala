package fly.play.sts

import org.specs2.mutable.{ Specification, Before }
import play.api.test._
import play.api.test.Helpers._
import fly.play.aws.auth.AwsCredentials
import fly.play.aws.Aws
import java.io.File

object StsSpec extends Specification with Before {

  def before = play.api.Play.start(FakeApplication(path = new File("./test")))

  "Sts" should {
    "be able to obtain session credentials" in {

      Sts.sessionToken(AwsCredentials.fromConfiguration).await(10000).get match {
        case Left(error) => failure(error.toString)
        case Right(credentials) => success
      }
    }
  }

  "SessionCredentials" should {
    "obtain session credentials" in {
      val awsCredentials = AwsSessionCredentials(AwsCredentials.fromConfiguration)
      awsCredentials must beLike {
        case AwsCredentials(accessKeyId, secretKey, Some(sessionToken), Some(expiration)) => {
          accessKeyId must not be empty
          secretKey must not be empty
          sessionToken must not be empty
          expiration must not be empty

        }
      }

      val AwsCredentials(accessKeyId, secretKey, Some(sessionToken), Some(expiration)) = awsCredentials

      "and get them from cache if requested a second time" in {
        AwsSessionCredentials(AwsCredentials.fromConfiguration) must beLike {
          case AwsCredentials(newAccessKeyId, newSecretKey, Some(newSessionToken), Some(newExpiration)) => {
            newAccessKeyId must_== accessKeyId
            newSecretKey must_== secretKey
            newSessionToken must_== sessionToken
            newExpiration must_== expiration

          }
        }
      }
    }
  }

  "AwsXmlCredentials" should {
    "extract the correct information" in {
      val xml = <GetSessionTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
                  <GetSessionTokenResult>
                    <Credentials>
                      <SessionToken>sessionToken</SessionToken>
                      <SecretAccessKey>secretAccessKey</SecretAccessKey>
                      <Expiration>2012-10-20T23:12:01.999Z</Expiration>
                      <AccessKeyId>accessKeyId</AccessKeyId>
                    </Credentials>
                  </GetSessionTokenResult>
                  <ResponseMetadata>
                    <RequestId>requestId</RequestId>
                  </ResponseMetadata>
                </GetSessionTokenResponse>

      AwsXmlCredentials(xml) must beLike {
        case AwsCredentials("accessKeyId", "secretAccessKey", Some("sessionToken"), Some(date)) =>
          date must_== Aws.dates.iso8601DateFormat.parse("2012-10-20T23:12:01.999Z")
        case mismatch:AwsCredentials => failure("Incorrect signature: " + AwsCredentials.unapply(mismatch).toString)
          
      }
    }
  }
}