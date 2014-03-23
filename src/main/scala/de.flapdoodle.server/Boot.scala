package de.flapdoodle.server

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

object Boot extends FlapdoodleServer(ActorSystem("Flapdoodle-Actor-System"))