package de.flapdoodle.server

import scala.concurrent.duration._
import akka.pattern.ask
import spray.can._
import spray.can.server.{Stats => SprayStats}
import spray.routing.HttpService
import org.json4s._
import spray.httpx.Json4sSupport
import scala.concurrent.ExecutionContext.Implicits.global

object MyFormats extends Json4sSupport{
  override implicit def json4sFormats: Formats = DefaultFormats
  implicit val statsMarshaller = json4sMarshaller[SprayStats]
  implicit val jvalueMarshaller = json4sMarshaller[JValue]
}
trait AdminHttpServerRoutes extends HttpService {
  self: AdminHttpServerActor =>

  import MyFormats.statsMarshaller
  import MyFormats.jvalueMarshaller

  val adminRoutes =
    path("admin" / "stats") {
      complete {
        (httpListener ? Http.GetStats)(1.second).mapTo[SprayStats]
      }
    } ~ path("admin" / "metrics") {
      complete {
        Stats.getGauges ++ Stats.getRuntimeStats
      }
    }

}
