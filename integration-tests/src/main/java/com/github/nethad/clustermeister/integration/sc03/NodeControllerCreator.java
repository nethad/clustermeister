package com.github.nethad.clustermeister.integration.sc03;

import akka.actor.Actor;
import akka.japi.Creator;

public class NodeControllerCreator implements Creator<Actor> {

	NodeControllerCreator(String nodeProvisionerAddress) {
		this.nodeProvisionerAddress = nodeProvisionerAddress;
	}

	String nodeProvisionerAddress = null;

	public Actor create() {
		return new NodeControllerActor(nodeProvisionerAddress);
	}
}
