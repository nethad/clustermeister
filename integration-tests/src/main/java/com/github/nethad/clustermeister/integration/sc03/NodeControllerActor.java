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
		System.out.println("Retrieving registrar actor reference ...");
		ActorRef nodeProvisioner = context().actorFor(nodeProvisionerAddress);
		System.out.println("Done: " + nodeProvisioner.toString());
		System.out.println("Sending message to registrar ...");
		nodeProvisioner.tell("REGISTER");
		System.out.println("Done.");
	}

	public LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public void onReceive(Object message) throws Exception {
		log.debug("Received message" + message);
		unhandled(message);
	}
}
