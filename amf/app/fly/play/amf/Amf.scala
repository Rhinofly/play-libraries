package fly.play.amf
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers
import play.api.mvc.Controller
import play.api.mvc.Results
import play.api.mvc.Result
import play.api.Play
import play.api.libs.iteratee.Iteratee
import java.io.PipedOutputStream
import java.io.PipedInputStream
import java.io.ByteArrayOutputStream
import com.exadel.flamingo.flex.messaging.amf.io.AMF3Deserializer
import com.exadel.flamingo.flex.messaging.amf.io.AMF3Serializer
import java.io.ByteArrayInputStream

trait Amf extends BodyParsers { self:Controller =>
	def amfParser[A]: BodyParser[A] = parse.when(
	  _.contentType.exists(_ == "application/x-amf"),
      tolerantAmfParser,
      request => Play.maybeApplication.map(_.global.onBadRequest(request, "Expecting application/x-amf body")).getOrElse(Results.BadRequest)
	)
	
	def tolerantAmfParser[A]: BodyParser[A] = BodyParser("amf") { request =>
	  
	  
	  Iteratee.consume[Array[Byte]]().mapDone { x =>
		  val derializer = new AMF3Deserializer(new ByteArrayInputStream(x))
		  Right(derializer.readObject().asInstanceOf[A])
	  }
	 
    }
	
	def Amf(data:Any):Result = {
		val b = new ByteArrayOutputStream();
		
		val serializer = new AMF3Serializer(b);
		serializer.writeObject(data);
		Ok(b.toByteArray).as("application/x-amf")
	}
}
