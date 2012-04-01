package fly.play.utils
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import play.api.mvc.Results.Unauthorized

/**
 * Usage:
 * 
 * object Authenticated extends WrappedAction[String](
 *   { request =>
 *     request.session.get("username") match {
 *       case Some(username) => Right(username)
 *       case None => Left(Unauthorized(views.html.defaultpages.unauthorized()))
 *     }
 *   })
 */
class WrappedAction[B](
    val value:RequestHeader => Either[Result, B]) {
  
  def apply[A](action: B => Action[A]): Action[(Action[A], A)] = {

    val customValueBodyParser = BodyParser { request =>
      value(request) match { 
        case Right(value) => {
        	val innerAction = action(value)
        	innerAction.parser(request).mapDone { body =>
        	body.right.map(innerBody => (innerAction, innerBody))
        	}
        }
        case Left(result) => Done(Left(result), Input.Empty)
      }
    }

    Action(customValueBodyParser) { request =>
      val (innerAction, innerBody) = request.body
      innerAction(request.map(_ => innerBody))
    }
  }
}

object WrappedAction {
  def apply[A, B](action: B => Action[A])(value:RequestHeader => Either[Result, B]): Action[(Action[A], A)] = 
    new WrappedAction[B](value)(action)
  
}
