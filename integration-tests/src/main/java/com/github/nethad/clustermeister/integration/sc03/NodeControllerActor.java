package com.github.nethad.clustermeister.integration.sc03;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class NodeControllerActor extends UntypedActor {

	NodeControllerActor(String nodeProvisionerAddress) {
		this.nodeProvisionerAddress = nodeProvisionerAddress;
	}

	protected String nodeProvisionerAddress = null;

	public void preStart() {
		super.preStart();
		ActorRef nodeProvisioner = context().actorFor(nodeProvisionerAddress);
		nodeProvisioner.tell("REGISTER");
	}

	public LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public void onReceive(Object message) throws Exception {
		log.debug("Received message" + message);
		unhandled(message);
	}
}
