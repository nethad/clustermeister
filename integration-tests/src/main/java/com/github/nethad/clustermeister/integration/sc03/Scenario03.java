/*
 * Copyright 2012 University of Zurich
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
package com.github.nethad.clustermeister.integration.sc03;

import akka.actor.ActorSystem;

import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.github.nethad.clustermeister.integration.AbstractScenario;
import com.github.nethad.clustermeister.integration.Assertions;
import java.util.*;
import akka.actor.*;
import akka.dispatch.Await;
import akka.dispatch.Future;
import static akka.pattern.Patterns.ask;
import akka.util.FiniteDuration;
import akka.actor.Address;
import akka.actor.ExtendedActorSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Option;

/**
 * @author pstutz
 */
public class Scenario03 extends AbstractScenario {

	private final Logger logger = LoggerFactory.getLogger(Scenario03.class);

	public static void main(String... args) throws InterruptedException {
		new Scenario03().execute();
	}

	@Override
	public void runScenario() throws Exception {
		logger.info("Scenario 03 started...");

		Clustermeister clustermeister = ClustermeisterFactory.create();
		try {
			logger.info("Clustermeister started.");
			logger.info("Getting nodes...");
			Collection<ExecutorNode> allNodes = clustermeister.getAllNodes();
			addToReport("node size", allNodes.size());
			logger.info(allNodes.size() + " nodes found.");

			logger.info("Starting actor system...");
			ActorSystem system = ActorSystem.create("RegistrarSystem",
					AkkaConfig.get());
			logger.info("Actor system started.");
			// ActorRef registrar = system.actorOf(
			// new Props().withCreator(new NodeProvisionerCreator()),
			// "Registrar");
			ActorRef registrar = system.actorOf(
					new Props().withCreator(new NodeProvisionerCreator()),
					"Registrar");
			logger.info("Registrar actor started.");

			Future<Object> nodesFuture = ask(registrar, "GET_NODES", 100000);
			String remoteAddress = getRemoteAddress(registrar, system);

			logger.info("Remote address: " + remoteAddress);
			
			allNodes.iterator()
					.next()
					.execute(
							new NodeControllerBootstrap(remoteAddress,
									AkkaConfig.get()));
			
			String result = (String) Await.result(nodesFuture,
					new FiniteDuration(20, "Seconds"));
			addToReport("Result: ", result);

			String expected = "AT_LEAST_1_NODE_FOUND";

			Assertions.assertEquals(expected, result,
					"Result is not as expected.");
		} catch (Exception ex) {
			logger.warn("Exception on result", ex);
			addToReport("Exception on result", ex);
		} finally {
			clustermeister.shutdown();
		}
	}

	public String getRemoteAddress(ActorRef actorRef, ActorSystem actorSystem) {
		Address dummyDestination = new Address("akka", "sys", "someHost", 42); // see
																				// http://groups.google.com/group/akka-user/browse_thread/thread/9448d8f628d38cc0
		Option<Address> akkaSystemAddress = ((ExtendedActorSystem) actorSystem)
				.provider().getExternalAddressFor(dummyDestination);
		String nodeProvisionerAddress = actorRef.path().toStringWithAddress(
				akkaSystemAddress.get());
		return nodeProvisionerAddress;
	}
}