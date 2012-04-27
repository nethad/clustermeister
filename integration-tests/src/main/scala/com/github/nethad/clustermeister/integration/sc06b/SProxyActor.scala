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

package com.github.nethad.clustermeister.integration.sc06b

import akka.actor.UntypedActor
import akka.event.Logging
import akka.event.LoggingAdapter
import org.xml.sax.helpers.NewInstance

class SProxyActor extends UntypedActor with SNode {
  
    val log: LoggingAdapter = Logging.getLogger(getContext.system, this)
    
  override def onReceive(message: Any) = message match {
    case Request(command, returnResult) => 
      log.info("Received message "+message)
      val res = result
      if (returnResult) {
        sender.tell(res)
      }
    case _ =>
      log.info("Received unknown message.")
  }
  
  override def result: String = {
    val sb = new StringBuilder("result:\n")
    
    val classes = Array("com.github.nethad.clustermeister.integration.sc07.PageRankVertex",
"com.github.nethad.clustermeister.integration.sc07.PageRank",
"com.github.nethad.clustermeister.integration.sc06b.ClassLoadingExampleClassScenario07b")
    for(clazz <- classes) {
      val clazz2 = Class.forName(clazz)
      sb.append(clazz2.getName()).append(" ").append(clazz2.getConstructors().length).append("\n")
      if (clazz2.getName().contains("ClassLoading")) {
        sb.append(clazz2.getMethod("hello").invoke(clazz2.newInstance()))
      }
    }
    sb.toString
  }

//	def onReceive(message: Object) = {
//		log.info("Received message" + message)
//            
//    if (message.isInstanceOf[Request[?]]) {
//      println("Yes")
//    }
//      
//		if (message instanceof Request<?>) {
//			@SuppressWarnings("unchecked")
//			Request<Node> request = (Request<Node>) message;
//			Object result = request.command().apply(this);
//			if (request.returnResult()) {
//				sender().tell(result);
//			}
//		}
//		unhandled(message);
//	}
//
//	@Override
//	public Integer numberOfCores() {
//		return Runtime.getRuntime().availableProcessors();
//	}


}
