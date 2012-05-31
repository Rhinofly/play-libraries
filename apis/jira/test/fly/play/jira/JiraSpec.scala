package fly.play.jira

import org.specs2.mutable.{ Specification, Before }
import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.api.test.FakeApplication
import java.security.MessageDigest
import org.apache.commons.codec.binary.Base64
import java.util.Date

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
    var issueKey1 = ""
    "create an issue" in {
      Jira.createIssue(Issue(Bug, None, "TST", "Issue from automatic test", "This issue is created using the api-jira projects test suite. It will be deleted with the next test."))
        .value.get must beLike {
          case Right(i: Issue) => {
            issueKey1 = i.key.getOrElse("")
            ok
          }
        }
    }

    "delete an issue" in {
      if (issueKey1.isEmpty) failure("No issue was created") else
        Jira.deleteIssue(issueKey1).value.get must_== Right(Success)
    }

    var issueKey2 = ""
    val summary = "Issue from automatic test"
    val description = new Date + "\nThis issue is created using the api-jira project test suite. It will be deleted with the next test."
    val hash = MessageDigest.getInstance("MD5").digest((summary + description).getBytes).map("%02X" format _).mkString

    "create a custom issue" in {
      Jira.createIssue(PlayProjectIssue(None, summary, description, Website.tests, Some(hash)))
        .value.get must beLike {
          case Right(i: Issue) => {
            issueKey2 = i.key.getOrElse("")
            ok
          }
        }
    }

    "find that issue" in {
      Jira.findIssues("project = PLAY AND Hash ~ " + hash + " AND Website = " + Website.tests.id + " ORDER BY priority DESC")
        .value.get must beLike {
          case Right(head :: Nil) if (head.key == Some(issueKey2)) => ok
        }
    }
    "find that issue as a specific type" in {
      Jira.findIssuesAs[PlayProjectIssue]("project = PLAY AND Hash ~ " + hash + " AND Website = " + Website.tests.id + " ORDER BY priority DESC")
        .value.get must beLike {
          case Right(head :: Nil) if (head.isInstanceOf[PlayProjectIssue] && head.key == Some(issueKey2)) => ok
        }
    }

    "add a comment" in {
      if (issueKey2.isEmpty) failure("No issue was created") else
      Jira.addComment(issueKey2, "Automatic comment").value.get must_== Right(Success) 
    }
    
    "delete that issue" in {
      if (issueKey2.isEmpty) failure("No issue was created") else
        Jira.deleteIssue(issueKey2).value.get must_== Right(Success)
    }
  }
}