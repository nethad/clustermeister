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
package com.github.nethad.clustermeister.integration.sc04;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Await;
import akka.dispatch.Future;
import akka.util.FiniteDuration;
import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.github.nethad.clustermeister.integration.AbstractScenario;
import com.github.nethad.clustermeister.integration.Assertions;
import com.github.nethad.clustermeister.integration.sc03.AkkaConfig;
import com.github.nethad.clustermeister.integration.sc03.NodeControllerBootstrap;
import com.github.nethad.clustermeister.integration.sc03.NodeProvisionerCreator;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Scenario that calculates pi. Calculation itself should run for 0.5 to 3 seconds, but class loading is slow (~90 seconds).
 * 
 * @author thomas
 */
public class Scenario04 extends AbstractScenario {
    
    private final Logger logger = LoggerFactory.getLogger(Scenario04.class);
    
    public static void main(String... args) throws InterruptedException {
        new Scenario04().execute();
    }

    @Override
    public void runScenario() throws Exception {
        
        new Thread(new Runnable() {

            @Override
            public void run() {
                logger.info("Scenario 04 started...");

                Clustermeister clustermeister = ClustermeisterFactory.create();
                try {
                    logger.info("Clustermeister started.");
                    logger.info("Getting nodes...");
                    Collection<ExecutorNode> allNodes = clustermeister.getAllNodes();
                    addToReport("node size", allNodes.size());
                    logger.info(allNodes.size() + " nodes found.");

                    logger.info("Execute Pi()");
                    ListenableFuture<String> result = allNodes.iterator().next().execute(new Pi());

                    logger.info("Waiting for result.");
                    String resultString = result.get(100, TimeUnit.SECONDS);
                    addToReport("Result: ", resultString);
                } catch (Exception ex) {
                    logger.warn("Exception on result", ex);
                    addToReport("Exception on result", ex);
                } finally {
                    clustermeister.shutdown();
                }
                
            }
        }).start();
    }
    
}
