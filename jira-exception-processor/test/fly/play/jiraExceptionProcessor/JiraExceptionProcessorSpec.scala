package fly.play.jiraExceptionProcessor

import org.specs2.mutable.{ Specification, Before }
import play.api.test._

object JiraExceptionProcessorSpec extends Specification with Before {
  def f = FakeApplication(new java.io.File("./test/"))

  def before = play.api.Play.start(f)

  "Jira" should {
    "find the 'Hash' custom field name" in {
      val hashCustomField = Jira.hashCustomField
      (hashCustomField must not be empty) and
        (hashCustomField must startWith("customfield"))
    }
    "find the component id" in {
      val componentId = Jira.componentId
      componentId must not be empty
    }
    "find the project id" in {
      val projectId = Jira.projectId
      projectId must not be empty
    }
  }

  "JiraExceptionProcessor" should {
    "report an error or add a comment" in {

      val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Map("testheader" -> Seq("headervalue"))), "body")

      JiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test"))

      success
    }

    "send an email in case of an error while reporting" in {
      val e = Error(0, Seq("Dit is een test om te kijken of er een mailtje verstuurd wordt wanneer er iets mis gaat met de automatische error reporting", "[fake stack trace]"))
      JiraExceptionProcessor.sendEmail(e)
      success
    }
  }
}