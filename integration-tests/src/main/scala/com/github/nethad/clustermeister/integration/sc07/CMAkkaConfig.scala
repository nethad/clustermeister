package com.github.nethad.clustermeister.integration.sc07

import com.typesafe.config._
import java.io.InputStreamReader
import java.io.BufferedInputStream
import scala.io.Source

object CMAkkaConfig {
  lazy val configStr = Source.fromInputStream(getClass().getResourceAsStream("/config-akka-cm.conf")).getLines.reduceLeft(_ + "\n" + _);
  
  println(configStr)
  
  lazy val get = ConfigFactory.parseString(configStr).withFallback(ConfigFactory.load).resolve
}