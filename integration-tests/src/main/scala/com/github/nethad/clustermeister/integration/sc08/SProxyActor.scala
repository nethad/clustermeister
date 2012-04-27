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
import akka.actor.Actor

class SProxyActor extends Actor with SNode {
  
//  val log: LoggingAdapter = Logging.getLogger(getContext.system, this)
  
  def receive = {
    case com.signalcollect.implementations.messaging.Request(command, reply) =>
      try {
    	  println("it's a request")
    	  println("Received command: " + command)
    	  val clazz = Class.forName("com.github.nethad.clustermeister.integration.sc08.SignalMessage")
    	  println(clazz.getName())
    	  val sm = SignalMessage(0, 1, "")
    	  sm.sourceId
    	  val result = command(this)
    	  if (reply) {
    		  if (result == null) { // Netty does not like null messages: org.jboss.netty.channel.socket.nio.NioWorker - WARNING: Unexpected exception in the selector loop. - java.lang.NullPointerException 
    			  sender ! None
    			  println("done.")
    		  } else {
    			  sender ! result
    			  println("done.")
    		  }
    	  }
      } catch {
        case any =>
          sender ! "Exception thrown: "+any
      }
  }

//  override def onReceive(message: Any) = {
//    log.info("request")
//    println("request")
//    val r = com.signalcollect.implementations.messaging.Request(null, true)
//    r.command
//    r.returnResult
//    
//    log.info("command")
//    println("command")
//    val c = com.signalcollect.implementations.messaging.Command("", "", Array())
//    c.arguments
//    c.className
//    c.methodDescription
    
    
    
//	log.info("before class for name")
//    println("before class for name")
//    val clazz = Class.forName("com.github.nethad.clustermeister.integration.sc08.SignalMessage")
//    val clazz = Class.forName("com.signalcollect.implementations.messaging.Request")
//    val clazz2 = Class.forName("com.signalcollect.implementations.messaging.Command")
//    val clazz3 = Class.forName("com.signalcollect.implementations.messaging.AkkaProxy")
//    log.info("class for name, name " + clazz3.getName())
//    println("class for name, name " + clazz3.getName())
//    log.info("done")
//    println("done")
//    sender.tell("Done.")
//  }
  
  override def result = "Hello world!"

}
