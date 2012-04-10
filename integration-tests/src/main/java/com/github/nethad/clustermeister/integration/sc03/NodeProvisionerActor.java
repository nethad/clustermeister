package com.github.nethad.clustermeister.integration.sc03;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class NodeProvisionerActor extends UntypedActor {

	public LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	ActorRef nodeRequestor = null;
	ActorRef node = null;

	public void onReceive(Object message) throws Exception {
		if (message instanceof String) {
			log.info("Received String message: {}", message);
			if (message.equals("REGISTER")) {
				node = sender();
				notifyNodeRequestor();
			} else if (message.equals("GET_NODES")) {
				nodeRequestor = sender();
				notifyNodeRequestor();
			} else {
				unhandled(message);
			}
		} else {
			unhandled(message);
		}
	}

	void notifyNodeRequestor() {
		if (nodeRequestor != null && node != null) {
			nodeRequestor.tell("AT_LEAST_1_NODE_FOUND");
		}
	}
}

