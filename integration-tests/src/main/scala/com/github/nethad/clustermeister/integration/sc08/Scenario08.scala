/*
 * Copyright 2012 The Clustermeister Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nethad.clustermeister.integration.sc08

import akka.actor.ActorSystem
import com.github.nethad.clustermeister.api.Clustermeister
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory
import com.github.nethad.clustermeister.integration.AbstractScenario
import com.signalcollect.configuration.AkkaConfig
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import com.github.nethad.clustermeister.api.ExecutorNode
import scala.collection.JavaConversions._
import com.google.common.util.concurrent.ListenableFuture
import akka.actor.ActorRef



class Scenario08 extends AbstractScenario {
  
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  
  override def runScenario() = {
    	val clustermeister: Clustermeister = ClustermeisterFactory.create()
		try {
			logger.info("Clustermeister started.")
			logger.info("Getting nodes...")
			val allNodes: Collection[ExecutorNode] = clustermeister.getAllNodes
			addToReport("node size", allNodes.size)
			logger.info(allNodes.size() + " nodes found.")

			val proxyAddressFuture: ListenableFuture[String] = allNodes.iterator.next.execute(new SProxyBootstrap(AkkaConfig.get))
			val proxyAddress = proxyAddressFuture.get
			val system: ActorSystem = ActorSystem.create("MainSystem",
					AkkaConfig.get)

			val proxyActor: ActorRef = system.actorFor(proxyAddress)

			val proxiedNode: SNode = AkkaProxy.newStringProxy(proxyActor)

			val result = proxiedNode.result
			println("result "+result)
            addToReport("result", result)

		} catch {
          case ex: Exception =>
			logger.warn("Exception on result", ex)
			addToReport("Exception on result", ex)
		} finally {
			clustermeister.shutdown
		}
  }
  
}
