package fly.play.jira

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.api.test.FakeApplication

object JiraSpec extends Specification with Before {
  def f = FakeApplication(new java.io.File("./test/"))

  def before = play.api.Play.start(f)

  "Jira" should {
    "obtain a token" in {
      Jira.token.value.get must beLike {
        case Right(_) => ok
      }
    }
    "return an error" in {
      Jira.token("incorrect", "credentials").value.get must beLike {
        case Left(x: Error) if x.message contains "Invalid username or password" => ok
      }
    }
    var issueKey = ""
    "create an issue" in {
      Jira.createIssue(Issue(Bug, None, "TST", "Issue from automatic test", "This issue is created using the api-jira projects test suite. It will be deleted with the next test."))
        .value.get must beLike {
          case Right(i: Issue) => {
            issueKey = i.key.getOrElse("no key found")
            ok
          }
        }
    }

    "detele an issue" in {
      Jira.deleteIssue(issueKey).value.get must_== Right(Success)
    }

    "create a custom issue" in {
      Jira.createIssue(PlayProjectIssue(None, "Issue from automatic test", "This issue is created using the api-jira project test suite. It will be deleted with the next test.", Website.tests))
        .value.get must beLike {
          case Right(i: Issue) => {
            issueKey = i.key.getOrElse("no key found")
            ok
          }
        }
    }

    "delete that issue" in {
      Jira.deleteIssue(issueKey).value.get must_== Right(Success)
    }

  }
}