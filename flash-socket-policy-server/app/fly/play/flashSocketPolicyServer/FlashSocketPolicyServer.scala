package fly.play.flashSocketPolicyServer

import java.io.InputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors

object FlashSocketPolicyServer {

  private[flashSocketPolicyServer] lazy val pool = Executors newFixedThreadPool 2
  private[flashSocketPolicyServer] lazy val serverSocket = new ServerSocket(843)

  private[flashSocketPolicyServer] lazy val socketServer = {

    Executors.newSingleThreadExecutor execute new Runnable {
      def run() =
        try {

          while (true) {
            val socket = serverSocket.accept()

            pool execute new Handler(socket)
          }

        } catch {
          case e: SocketException => // the socket server might be closed while waiting with accept()
        } finally {
          stop()
        }
    }
  }

  def start(): Unit = socketServer

  def stop(): Unit = {
    serverSocket.close()
    pool.shutdown()
  }

  private[flashSocketPolicyServer] val LIMIT = 100
  private[flashSocketPolicyServer] val POLICY_REQUEST = "<policy-file-request/>"
  private[flashSocketPolicyServer] val POLICY = {
    val port = Option(System getProperty "http.port") getOrElse "9000"
    """<?xml version=\"1.0\"?><cross-domain-policy><allow-access-from domain="*" to-ports="%s" /></cross-domain-policy>""" format port
  }

  def readPolicyRequest(inputStream: InputStream) = {
    //read from the stream until the 0 or -1 byte
    val bytes =
      Stream continually inputStream.read takeWhile (i => 0 != i && -1 != i)

    val policyRequestStream = (bytes map Character.toChars).flatten

    //in case we don't receive a 0 byte, we dont read too much 
    (policyRequestStream take LIMIT).mkString
  }

  class Handler(socket: Socket) extends Runnable {

    def run() =
      try {

        val policyRequest = readPolicyRequest(socket.getInputStream)
        
        if (policyRequest == POLICY_REQUEST) {
          val writer = new PrintWriter(socket.getOutputStream)
          writer write POLICY
          writer.flush()
        }

      } finally {
        socket.close
      }

  }

}
