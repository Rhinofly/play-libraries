package fly.play.aws.auth

import org.specs2.mutable.Specification
import java.util.Date
import org.specs2.mutable.Before
import play.api.test.FakeApplication

object AwsCredentialsSpec extends Specification  with Before {

  def f = FakeApplication(new java.io.File("./test/"))
  
  def before = play.api.Play.start(f)
  
	"AwsCredentials" should {
	  "retrieve from configuration" in {
	    AwsCredentials.fromConfiguration must_== AwsCredentials("testKey", "testSecret", None, None)
	  }
	  "implement apply" in {
	    AwsCredentials("key", "secret")
	    AwsCredentials("key", "secret", Some("token"), Some(new Date))
	    ok
	  }
	  "implement unapply" >> {
		  val AwsCredentials(a, b, c, d) = AwsCredentials("key", "secret")
		  a must_== "key"
		  b must_== "secret"
		  c must_== None
		  d must_== None
		  
		  val date = new Date
		  
		  val AwsCredentials(e, f, Some(g), Some(h)) = AwsCredentials("key", "secret", Some("token"), Some(date))
		  e must_== "key"
		  f must_== "secret"
		  g must_== "token"
		  h must_== date
	  }
	  
	  def checkImplicit()(implicit c:AwsCredentials) = c
	  
	  "provide an implicit value" in {
	    checkImplicit must not beNull
	  }
	  
	  "override the implicit" in {
		  checkImplicit()(AwsCredentials("test", "test")) must_== AwsCredentials("test", "test")
	  }
	}
}