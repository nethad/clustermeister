package com.github.nethad.clustermeister.integration.sc06b
import com.github.nethad.clustermeister.integration.sc06.Node

case class Request[ProxiedClass](command: ProxiedClass => Any, returnResult: Boolean = false)

object ExampleRequests {
  def simpleNumberOfCores = Request[Node]((_.numberOfCores()), true)
}