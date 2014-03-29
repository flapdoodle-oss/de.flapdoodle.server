package de.flapdoodle.server

import scala.concurrent.duration._
import akka.pattern.ask
import spray.can._
import spray.can.server.{Stats => SprayStats}
import spray.routing.HttpService
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import scala.concurrent.ExecutionContext.Implicits.global

trait AdminHttpServerRoutes extends HttpService with Json4sSupport {
  self: AdminHttpServerActor =>


  override implicit def json4sFormats: Formats = DefaultFormats

  implicit val statsMarshaller = json4sMarshaller[SprayStats]

  val adminRoutes =
    path("admin" / "stats") {
      complete {
        (httpListener ? Http.GetStats)(1.second).mapTo[SprayStats].map{
          // this map should not be necessary...
          value => value
        }
      }
    } ~ path("admin" / "metrics") {
      complete {
        Stats.getGauges ++ Stats.getRuntimeStats
      }
    }

}
