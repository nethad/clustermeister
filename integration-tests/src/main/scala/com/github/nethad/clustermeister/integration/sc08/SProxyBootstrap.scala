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

package com.github.nethad.clustermeister.integration.sc06

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import java.util.concurrent.Callable
import akka.actor.Address
import akka.actor.Props
import com.typesafe.config.Config

class SProxyBootstrap(config: Config) extends Callable[String] with Serializable {
  
  def call(): String = {
        val system: ActorSystem = ActorSystem.create("NodeSystem", config)
        val proxy: ActorRef = system.actorOf(new Props().withCreator(new SProxyCreator), "ProxyActor")
		getRemoteAddress(proxy, system)
  }
  
  def getRemoteAddress(actorRef: ActorRef, actorSystem: ActorSystem): String = {
        val dummyDestination = new Address("akka", "sys", "someHost", 42)
		val akkaSystemAddress = actorSystem.asInstanceOf[ExtendedActorSystem].provider.getExternalAddressFor(dummyDestination)
		val nodeProvisionerAddress = actorRef.path.toStringWithAddress(
				akkaSystemAddress.get)
		nodeProvisionerAddress
  }
  
}
