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
package com.github.nethad.clustermeister.integration.sc06;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Await;
import static akka.pattern.Patterns.ask;
import akka.util.FiniteDuration;
import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.github.nethad.clustermeister.integration.AbstractScenario;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pstutz
 */
public class Scenario06 extends AbstractScenario {

	private final Logger logger = LoggerFactory.getLogger(Scenario06.class);

	public static void main(String... args) throws InterruptedException {
		new Scenario06().execute();
	}

	@Override
	public void runScenario() {
 		logger.info("Scenario 06 started...");

		Clustermeister clustermeister = ClustermeisterFactory.create();
		try {
			logger.info("Clustermeister started.");
			logger.info("Getting nodes...");
			Collection<ExecutorNode> allNodes = clustermeister.getAllNodes();
			addToReport("node size", allNodes.size());
			logger.info(allNodes.size() + " nodes found.");

			ListenableFuture<?> proxyAddressFuture = allNodes.iterator().next()
					.execute(new ProxyBootstrap(AkkaConfig.get()));

			String proxyAddress = (String) proxyAddressFuture.get();

			ActorSystem system = ActorSystem.create("MainSystem",
					AkkaConfig.get());

			ActorRef proxyActor = system.actorFor(proxyAddress);

			Node proxiedNode = AkkaProxy.newStringProxy(proxyActor);

			Integer cores1 = proxiedNode.numberOfCores();

//			Integer expected1 = Runtime.getRuntime().availableProcessors();

//			System.out.println("First result: " + cores1);

//			Assertions.assertEquals(expected1, cores1,
//					"Result is not as expected.");
            addToReport("first result", cores1);

			akka.dispatch.Future<Object> coresFuture = ask(proxyActor,
					ExampleRequests.simpleNumberOfCores(), 100000);
			Integer cores2 = (Integer) Await.result(coresFuture,
					new FiniteDuration(100, "Seconds"));

			System.out.println("Second result: " + cores2);
            addToReport("second result", cores2);

//			Integer expected2 = Runtime.getRuntime().availableProcessors();

//			Assertions.assertEquals(expected2, cores2,
//					"Result is not as expected.");

		} catch (Exception ex) {
			logger.warn("Exception on result", ex);
			addToReport("Exception on result", ex);
		} finally {
			clustermeister.shutdown();
		}
	}

}