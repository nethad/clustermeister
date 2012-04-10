package com.github.nethad.clustermeister.integration.sc03;

import akka.actor.Actor;
import akka.japi.Creator;

public class NodeProvisionerCreator implements Creator<Actor> {
	public Actor create() {
		return new NodeProvisionerActor();
	}
}
