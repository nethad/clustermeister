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

import akka.actor.UntypedActor
import akka.event.Logging
import akka.event.LoggingAdapter

class SProxyActor extends UntypedActor with Node {
  
    val log: LoggingAdapter = Logging.getLogger(getContext.system, this)
    
  override def onReceive(message: Any) = message match {
    case Request(command, returnResult) => 
      log.info("Received message "+message)
      val result = command.apply(this)
      if (returnResult) {
        sender.tell(result)
      }
    case _ =>
      log.info("Received unknown message.")
  }
  
  override def numberOfCores = Runtime.getRuntime.availableProcessors

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
