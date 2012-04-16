package com.github.nethad.clustermeister.integration.sc06;

import akka.actor.Actor;
import akka.japi.Creator;

public class ProxyCreator implements Creator<Actor> {

	public Actor create() {
		return new ProxyActor();
	}
}
