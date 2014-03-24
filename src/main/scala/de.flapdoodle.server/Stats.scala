package de.flapdoodle.server

object Stats {
  val metricRegistry = new com.codahale.metrics.MetricRegistry()
  metricRegistry.registerAll(new com.codahale.metrics.JvmAttributeGaugeSet)
}
