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

package com.github.nethad.clustermeister.integration.sc07

/**
 * Creator in separate class to prevent excessive closure-capture of the TorqueNodeProvisioner class (Error[java.io.NotSerializableException TorqueNodeProvisioner])
 */
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.dispatch.Await
import akka.japi.Creator
import akka.util.Timeout
import akka.util.duration._
import com.github.nethad.clustermeister.api.Clustermeister
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory
import collection.JavaConversions._
import akka.pattern.ask
import com.typesafe.config.Config
import java.util.concurrent.Callable
import com.signalcollect.nodeprovisioning.torque._
import com.signalcollect.configuration.AkkaConfig
import com.signalcollect.implementations.messaging.AkkaProxy
import com.signalcollect.nodeprovisioning._

case class NodeControllerCreator(jobId: Any, nodeProvisionerAddress: String) extends Creator[NodeControllerActor] {
  def create: NodeControllerActor = new NodeControllerActor(jobId, nodeProvisionerAddress)
}

/**
 * Creator in separate class to prevent excessive closure-capture of the TorqueNodeProvisioner class (Error[java.io.NotSerializableException TorqueNodeProvisioner])
 */
case class NodeProvisionerCreator(numberOfNodes: Int) extends Creator[NodeProvisionerActor] {
  def create: NodeProvisionerActor = new NodeProvisionerActor(numberOfNodes)
}

class ClustermeisterNodeProvisioner extends NodeProvisioner {

  var cm: Option[Clustermeister] = None

  def getNodes: List[Node] = {
    try {
      cm = Some(ClustermeisterFactory.create)
      println("Start Clustermeister")
      if (cm.isDefined) {
        val numberOfNodes = cm.get.getAllNodes.size
        val system: ActorSystem = ActorSystem("NodeProvisioner", AkkaConfig.get)
        val nodeProvisionerCreator = NodeProvisionerCreator(numberOfNodes)
        val nodeProvisioner = system.actorOf(Props().withCreator(nodeProvisionerCreator.create), name = "NodeProvisioner")
        val nodeProvisionerAddress = AkkaHelper.getRemoteAddress(nodeProvisioner, system)
        implicit val timeout = new Timeout(1800 seconds)
        for (node <- cm.get.getAllNodes) {
          val actorNameFuture = node.execute(NodeControllerBootstrap(node.getID, nodeProvisionerAddress, AkkaConfig.get))
          println("Started node controller: " + actorNameFuture.get)
        }
        val nodesFuture = nodeProvisioner ? "GetNodes"
        val result = Await.result(nodesFuture, timeout.duration)
        val nodes: List[Node] = result.asInstanceOf[List[ActorRef]] map (AkkaProxy.newInstance[Node](_))
        nodes
      } else {
        throw new Exception("Clustermeister could not be initialized.")
      }
    } finally {
      if (cm.isDefined) {
        cm.get.shutdown
      }
    }
  }

}

case class NodeControllerBootstrap(nodeId: String, nodeProvisionerAddress: String, akkaConfig: Config) extends Callable[String] {
  def call: String = {
    val system = ActorSystem("SignalCollect", akkaConfig)
    val nodeControllerCreator = NodeControllerCreator(nodeId, nodeProvisionerAddress)
    val nodeController = system.actorOf(Props().withCreator(nodeControllerCreator.create), name = "NodeController" + nodeId)
//    system.awaitTermination
    "Done"
  }
}
