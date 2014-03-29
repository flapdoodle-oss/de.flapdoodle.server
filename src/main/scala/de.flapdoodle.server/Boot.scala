package de.flapdoodle.server

import akka.actor.ActorSystem

/**
 * EXAMPLE
 */
object Boot extends FlapdoodleServer(ActorSystem("Flapdoodle-Actor-System")) with Instrumented {

  // add gauge with scala metrics
  metrics.gauge("myGauge")("1")

  // add gauge with without instrumented prefix
  def gauge = 777

  Stats.metricRegistry.register("myGaugedd2", Stats.createGauge[Int](gauge))

}