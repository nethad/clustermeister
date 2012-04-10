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
package com.github.nethad.clustermeister.integration.sc01;

import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.github.nethad.clustermeister.integration.AbstractScenario;
import com.github.nethad.clustermeister.integration.Assertions;
import com.github.nethad.clustermeister.integration.ReturnStringCallable;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Scenario01 extends AbstractScenario {
    
    private final Logger logger = LoggerFactory.getLogger(Scenario01.class);
    
    public static void main(String... args) throws InterruptedException {
        new Scenario01().execute();
    }

    @Override
    public void runScenario() throws Exception {
        logger.info("Run scenario.");

        Clustermeister clustermeister = ClustermeisterFactory.create();
        try {
            logger.info("Start Clustermeister.");
            Collection<ExecutorNode> allNodes = clustermeister.getAllNodes();
            addToReport("node size", allNodes.size());
//            logger.info("nodes size = {}", allNodes.size());
            Assertions.assertEquals(1, allNodes.size(), "Number of nodes not as expected");
            String expectedString = "it works!";
            if (allNodes.size() > 0) {
                ListenableFuture<String> result = allNodes.iterator().next().execute(new ReturnStringCallable(expectedString));
                try {
                    String resultString = result.get();
                    addToReport("result string", resultString);
//                    Assertions.assertEquals(expectedString, resultString, "Result string is not as expected.");
                } catch (ExecutionException ex) {
                    logger.warn("Exception on result", ex);
                    addToReport("exception on result", ex);
                }
            }
        } finally {
            clustermeister.shutdown();
        }
    }
    
}
