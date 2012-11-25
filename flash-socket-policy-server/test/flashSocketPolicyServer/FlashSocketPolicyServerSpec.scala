package fly.play.flashSocketPolicyServer
import org.specs2.Specification
import play.api.libs.ws.WS
import java.net.Socket
import org.specs2.specification.Around
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

object FlashSocketPolicyServerSpec extends Specification with Mockito {

  def is =
    "FlashSocketPolicyServer should listen to port 843" !
      {
        server {
          val socket = new Socket("localhost", 843)

          socket.isConnected === true
        }

      } ^
      "Method readPolicyRequest should be able to extract the policy from the inputstream" !
      {
        val policyRequestBytes = FlashSocketPolicyServer.POLICY_REQUEST.getBytes

        val result = FlashSocketPolicyServer readPolicyRequest new ByteArrayInputStream(policyRequestBytes)

        result === FlashSocketPolicyServer.POLICY_REQUEST
      } ^
      "Handler should write the policy file if we supply the correct request" !
      {
        val policyRequestBytes = FlashSocketPolicyServer.POLICY_REQUEST.getBytes

        testHandler(policyRequestBytes) === FlashSocketPolicyServer.POLICY
      } ^
      "Handler should write the policy file if we supply the correct request with 0 byte termination" !
      {
        val policyRequestBytes =
          FlashSocketPolicyServer.POLICY_REQUEST.getBytes ++
            Array(0.toByte) ++
            "test".getBytes

        testHandler(policyRequestBytes) === FlashSocketPolicyServer.POLICY
      } ^
      "Handler should write nothing and call close on the socket for an invalid request" !
      {
        val policyRequestBytes = "wrong request".getBytes

        val (result, socket) = testHandlerWithSocket(policyRequestBytes)

        there was one(socket).close() and result === ""
      } ^ end

  def testHandlerWithSocket(input: Array[Byte]): (String, Socket) = {
    val socket = mock[Socket]

    val bao = new ByteArrayOutputStream
    val bai = new ByteArrayInputStream(input)

    socket.getOutputStream() returns bao
    socket.getInputStream() returns bai

    val handler = new FlashSocketPolicyServer.Handler(socket)

    handler.run

    (new String(bao.toByteArray), socket)
  }

  def testHandler(input: Array[Byte]) = testHandlerWithSocket(input)._1

  object server extends Around {
    def around[T <% Result](t: => T) = {
      FlashSocketPolicyServer.start()

      val result = t

      FlashSocketPolicyServer.stop()

      result
    }
  }

}
