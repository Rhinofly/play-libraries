package fly.play.libraryUtils

import org.specs2.mutable.{Specification, Before}
import play.api.test.FakeApplication
import play.api.Play.current
import play.api.PlayException
import java.io.File

object PlayConfigurationSpec extends Specification with Before {
	def before = play.api.Play.start(FakeApplication(new File("./test")))
  
	"PlayConfiguration(test.existingKey)" should {
	  "return 'Fake value'" in {
	    PlayConfiguration("test.existingKey") must_== "Fake value"
	  }
	}
	"PlayConfiguration(test.nonExistingKey)" should {
		"throw a PlayException" in {
			PlayConfiguration("test.nonExistingKey") must throwA[PlayException]
		}
	}
}