package de.flapdoodle.server

import scala.concurrent.duration._
import akka.pattern.ask
import spray.can._
import spray.can.server.Stats
import spray.routing.HttpService
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import scala.concurrent.ExecutionContext.Implicits.global
import com.codahale.metrics.Metric

trait AdminHttpServerRoutes extends HttpService with Json4sSupport {
  self: AdminHttpServerActor =>
  override implicit def json4sFormats: Formats = DefaultFormats
  implicit val statsMarshaller = json4sMarshaller[Stats]
  implicit val metricsMarshaller = json4sMarshaller[Metric]

  val adminRoutes =
    path("admin" / "stats") {
      complete {
        (httpListener ? Http.GetStats)(1.second).mapTo[Stats]
      }
    }

}
