package de.flapdoodle.server

import spray.routing.HttpService
import spray.http.MediaTypes._
import spray.can.server.Stats
import spray.can.Http.GetStats
import akka.pattern.ask

import org.json4s.{DefaultFormats, Formats}
import akka.util.Timeout
import scala.concurrent.duration._
import spray.httpx.Json4sSupport

trait AdminHttpServerRoutes extends HttpService with Json4sSupport {
  self: AdminHttpServerActor =>
  implicit def json4sFormats: Formats = DefaultFormats

  implicit val timeout = Timeout(5 seconds)

  implicit val statsMarshaller = json4sMarshaller[Stats]

  val adminRoutes =
    path("admin") {
      get {
        respondWithMediaType(`application/json`) {
          // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            httpListener.ask(GetStats).mapTo[Stats]
          }
        }
      }
    }
}
