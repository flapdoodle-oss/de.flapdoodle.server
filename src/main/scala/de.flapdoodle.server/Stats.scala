package de.flapdoodle.server

import java.lang.management.{ManagementFactory, RuntimeMXBean}
import scala.collection.JavaConverters._

import org.json4s.JsonDSL._
import org.json4s._
import com.codahale.metrics.Gauge

object Stats {
  val runtime: RuntimeMXBean = ManagementFactory.getRuntimeMXBean
  val metricRegistry = new com.codahale.metrics.MetricRegistry()

  def getRuntimeStats : JValue = ("runtime" ->
    ("name" -> runtime.getName) ~
      ("vendor" -> s"${runtime.getVmVendor} ${runtime.getVmName} ${runtime.getVmVersion} (${runtime.getSpecVersion})") ~
      ("uptimeInMs" -> runtime.getUptime()) ~
      ("startTime" -> runtime.getStartTime()) ~
      ("inputArguments" -> runtime.getInputArguments().asScala) ~
      ("systemProps" -> runtime.getSystemProperties().asScala))

  def getGauges = {
    metricRegistry.getGauges.asScala.foldLeft[JValue](JNothing) {
      (result, value) =>
        result.++(
          (value._1 -> value._2.getValue().toString()) ~
            ("type" -> classOf[Gauge[value._2.type]].getSimpleName())
        )
    }
  }

  def createGauge[T](value: => T): com.codahale.metrics.Gauge[T] = {
    new com.codahale.metrics.Gauge[T]() {
      override def getValue() = value
    }
  }
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = Stats.metricRegistry
}
