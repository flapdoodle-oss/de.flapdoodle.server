package de.flapdoodle.server.stats

import de.flapdoodle.server.Instrumented
import java.lang.management.ManagementFactory
import org.json4s.JsonAST.JObject
import scala.util.Try
import java.lang.reflect.Method


/**
 * JVM Stats
 * -> taken from https://github.com/twitter/twitter-server
 */
protected[server] object JvmStats extends Instrumented{
  import scala.collection.JavaConverters._

  def register() = {

    val mem = ManagementFactory.getMemoryMXBean()

    def heap = mem.getHeapMemoryUsage()
    metrics.gauge("heap.committed") { heap.getCommitted() }
    metrics.gauge("heap.max") { heap.getMax() }
    metrics.gauge("heap.used") { heap.getUsed() }

    def nonHeap = mem.getNonHeapMemoryUsage()
    metrics.gauge("nonheap.committed") { nonHeap.getCommitted() }
    metrics.gauge("nonheap.max") { nonHeap.getMax() }
    metrics.gauge("nonheap.used") { nonHeap.getUsed() }

    val threads = ManagementFactory.getThreadMXBean()
    metrics.gauge("thread.daemon_count") { threads.getDaemonThreadCount().toLong }
    metrics.gauge("thread.count") { threads.getThreadCount().toLong }
    metrics.gauge("thread.peak_count") { threads.getPeakThreadCount().toLong }

    val runtime = ManagementFactory.getRuntimeMXBean()
    metrics.gauge("start_time") { runtime.getStartTime() }
    metrics.gauge("uptime") { runtime.getUptime() }

    val os = ManagementFactory.getOperatingSystemMXBean()
    metrics.gauge("num_cpus") { os.getAvailableProcessors().toLong }
    os match {
      case unix: com.sun.management.UnixOperatingSystemMXBean =>
         metrics.gauge("fd_count") { unix.getOpenFileDescriptorCount }
         metrics.gauge("fd_limit") { unix.getMaxFileDescriptorCount }
      case _ =>
    }

    val compilation = ManagementFactory.getCompilationMXBean()
    metrics.gauge("compilation.time_msec") { compilation.getTotalCompilationTime() }

    val classes = ManagementFactory.getClassLoadingMXBean()
    metrics.gauge("classes.total_loaded") { classes.getTotalLoadedClassCount() }
    metrics.gauge("classes.total_unloaded") { classes.getUnloadedClassCount() }
    metrics.gauge("classes.current_loaded") { classes.getLoadedClassCount().toLong }


    val memPool = ManagementFactory.getMemoryPoolMXBeans.asScala
    memPool foreach {
      pool =>
      val poolName: String = pool.getName().replaceAll("\\s","_")

      if (pool.getCollectionUsage != null) {
        def usage = pool.getCollectionUsage // this is a snapshot, we can't reuse the value
        metrics.gauge(s"mem.postGC.${poolName}.used") { usage.getUsed }
        metrics.gauge(s"mem.postGC.${poolName}.max") { usage.getMax }
      }
      if (pool.getUsage != null) {
        def usage = pool.getUsage // this is a snapshot, we can't reuse the value
        metrics.gauge(s"mem.current.${poolName}.used") { usage.getUsed }
        metrics.gauge(s"mem.current.${poolName}.max") { usage.getMax }
      }
    }
    metrics.gauge("mem.postGC.used") {
      memPool flatMap(p => Option(p.getCollectionUsage)) map(_.getUsed) sum
    }
    metrics.gauge("mem.current.used") {
      memPool flatMap(p => Option(p.getUsage)) map(_.getUsed) sum
    }

    // `BufferPoolMXBean` and `ManagementFactory.getPlatfromMXBeans` are introduced in Java 1.7.
    // Use reflection to add these gauges so we can still compile with 1.6
    for {
      bufferPoolMXBean <- Try[Class[_]] {
        ClassLoader.getSystemClassLoader.loadClass("java.lang.management.BufferPoolMXBean")
      }
      getPlatformMXBeans <- classOf[ManagementFactory].getMethods.find { m =>
        m.getName == "getPlatformMXBeans" && m.getParameterTypes.length == 1
      }
      pool <- getPlatformMXBeans.invoke(null /* static method */, bufferPoolMXBean)
        .asInstanceOf[java.util.List[_]].asScala
    } {
      val name = bufferPoolMXBean.getMethod("getName").invoke(pool).asInstanceOf[String].replaceAll("\\s","_")

      val getCount: Method = bufferPoolMXBean.getMethod("getCount")
      metrics.gauge(s"buffer.${name}.count") { getCount.invoke(pool).asInstanceOf[Long] }

      val getMemoryUsed: Method = bufferPoolMXBean.getMethod("getMemoryUsed")
      metrics.gauge(s"buffer.${name}.used") { getMemoryUsed.invoke(pool).asInstanceOf[Long] }

      val getTotalCapacity: Method = bufferPoolMXBean.getMethod("getTotalCapacity")
      metrics.gauge(s"buffer.${name}.max") { getTotalCapacity.invoke(pool).asInstanceOf[Long] }
    }

    val gcPool = ManagementFactory.getGarbageCollectorMXBeans.asScala
    gcPool foreach { gc =>
      val name = gc.getName.replaceAll("\\s","_")
      metrics.gauge(s"gc.${name}.cycles") { gc.getCollectionCount }
      metrics.gauge(s"gc.${name}.msec") { gc.getCollectionTime }
    }

    // note, these could be -1 if the collector doesn't have support for it.
    metrics.gauge(s"gc.cycles") { gcPool map(_.getCollectionCount) filter(_ > 0) sum }
    metrics.gauge(s"gc.msec") { gcPool map(_.getCollectionTime) filter(_ > 0) sum }
  }
}

