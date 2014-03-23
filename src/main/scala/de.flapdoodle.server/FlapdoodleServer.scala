package de.flapdoodle.server

import akka.actor.ActorSystem

class FlapdoodleServer(val system: ActorSystem = FlapdoodleServer.defaultSystem) extends App {

  system.actorOf(AdminHttpServerActor.props, "AdminHttpServer")

  sys addShutdownHook {
    system.shutdown()
  }
}

object FlapdoodleServer {
  lazy val defaultSystem = ActorSystem("FlapdoodleServer")
}