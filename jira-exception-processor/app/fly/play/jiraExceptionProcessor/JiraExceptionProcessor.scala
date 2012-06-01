package fly.play.jiraExceptionProcessor

import play.api.mvc.RequestHeader
import java.io.StringWriter
import java.io.PrintWriter
import fly.play.libraryUtils.PlayConfiguration
import play.api.mvc.Session
import play.api.Play.current
import java.security.MessageDigest
import fly.play.jira.Jira
import fly.play.jira.Error
import fly.play.jira.Success
import fly.play.ses.Ses
import fly.play.ses.Email
import fly.play.ses.EmailAddress
import fly.play.ses.Recipient
import javax.mail.Message

object JiraExceptionProcessor {
  lazy val website = PlayConfiguration("jira.play.website")

  def getStackTraceString(ex: Throwable): String = {
    val s = new StringWriter
    val p = new PrintWriter(s)
    ex.printStackTrace(p)
    s.toString
  }

  def getRequestString(request: RequestHeader): String = {

    "uri: " + request.uri + "\n" +
      "path: " + request.path + "\n" +
      "method: " + request.method + "\n" +
      "headers: \n" +
      request.headers.toMap.toList.map((keyValueSeq _).tupled).mkString("\n") + "\n" +
      "session: \n" +
      request.session.data.toList.map((keyValue _).tupled).mkString("\n") + "\n" +
      "flash: \n" +
      request.flash.data.toList.map((keyValue _).tupled).mkString("\n")

  }

  def keyValue(key: String, value: String): String = "   " + key + ": " + value
  def keyValueSeq(key: String, value: Seq[String]): String = keyValue(key, value.mkString(", "))

  def reportError(request: RequestHeader, ex: Throwable): Unit = {

    val result: Either[Error, Success] = try {
      val summary = ex.getMessage
      val description = getStackTraceString(ex)
      val hash = createHash(description)
      val comment = getRequestString(request)

      Jira.findIssuesAs[PlayProjectIssue]("project = PLAY AND Hash ~ " + hash + " AND Website = " + website + " ORDER BY priority DESC")
        .flatMap {
          //we found an issue, add the comment
          case Right(x) if (!x.isEmpty) => Jira.addComment(x.head.key.get, comment)
          case Right(_) => Jira.createIssue(PlayProjectIssue(None, summary, description, website, hash)).flatMap {
            case Right(playProjectIssue) => Jira.addComment(playProjectIssue.key.get, comment)
          }

        }.value.get

    } catch {
      case e => Left(Error(0, e.getMessage, Some(getStackTraceString(e))))
    }

    result match {
      case Left(error) => sendEmail(error)
      case Right(success) => /* error reported */
    }
  }

  def sendEmail(error: Error) = {
    val message = error.code + "\n" +
      error.message + "\n\n" +
      error.data.getOrElse("")

    Ses.sendEmail(Email(
      subject = "Failed to report error",
      from = EmailAddress(PlayConfiguration("mail.from.name"), PlayConfiguration("mail.from.address")),
      replyTo = None,
      recipients = List(Recipient(Message.RecipientType.TO, EmailAddress("Play", "play+error@rhinofly.nl"))),
      text = message,
      htmlText = message.replace("\n", "<br />"),
      attachments = Seq.empty))
  }

  def createHash(str: String): String =
    MessageDigest.getInstance("MD5").digest(str.getBytes).map("%02X" format _).mkString
}

