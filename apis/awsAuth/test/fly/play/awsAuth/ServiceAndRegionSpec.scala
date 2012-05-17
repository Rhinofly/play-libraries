package fly.play.awsAuth

import org.specs2.mutable._

object ServiceAndRegionSpec extends Specification {
  "ServiceAndRegion" should {
    "extract the correct signatures" in {
      ServiceAndRegion("dynamodb.us-east-1.amazonaws.com") must_== ServiceAndRegion("dynamodb", "us-east-1")
      ServiceAndRegion("dynamodb.us-gov.amazonaws.com") must_== ServiceAndRegion("dynamodb", "us-gov-west-1")
    }
  }
}