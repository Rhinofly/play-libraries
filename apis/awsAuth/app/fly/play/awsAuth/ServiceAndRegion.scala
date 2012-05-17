package fly.play.awsAuth

case class ServiceAndRegion(service: String, region: String)

object ServiceAndRegion extends ((String, String) => ServiceAndRegion) {
  val Pattern = """(.*?)\.(.*?)\.amazonaws\.com""".r

  def apply(host: String): ServiceAndRegion = host match {
    case Pattern(service, "us-gov") => apply(service, "us-gov-west-1")
    case Pattern(service, region) => apply(service, region)
    case _ => apply("us-east-1", "us-east-1")
  }
}