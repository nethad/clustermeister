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

import com.github.nethad.clustermeister.integration.AbstractScenario
import com.signalcollect.GraphBuilder
import com.signalcollect.configuration.LoggingLevel

object Scenario07 extends App {
  
  new Scenario07().execute
  
}

class Scenario07 extends AbstractScenario {
  
  override def runScenario() = {
    val graph = GraphBuilder.withNodeProvisioner(new ClustermeisterNodeProvisioner()).withLoggingLevel(LoggingLevel.Debug).build
    
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new PageRankVertex(3))
    graph.addEdge(new PageRankEdge(1, 2))
    graph.addEdge(new PageRankEdge(2, 1))
    graph.addEdge(new PageRankEdge(2, 3))
    graph.addEdge(new PageRankEdge(3, 2))
    
    val stats = graph.execute //(ExecutionConfiguration())
    //  val stats = graph.execute(ExecutionConfiguration().withExecutionMode(ExecutionMode.ContinuousAsynchronous))
    graph.awaitIdle
    //  val stats = graph.execute(ExecutionConfiguration().withExecutionMode(ExecutionMode.Synchronous))
    println(stats)
    graph.foreachVertex(println(_))
    graph.shutdown
  }

}
