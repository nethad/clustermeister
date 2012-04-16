package com.github.nethad.clustermeister.integration.sc06;

import java.io.Serializable;
import java.util.concurrent.Callable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Address;
import akka.actor.ExtendedActorSystem;
import scala.Option;
import com.typesafe.config.Config;

public class ProxyBootstrap implements Callable<String>, Serializable {

	private static final long serialVersionUID = 1L;

	protected Config akkaConfig = null;

	public ProxyBootstrap(Config akkaConfig) {
		this.akkaConfig = akkaConfig;
	}

	@Override
	public String call() throws Exception {
		ActorSystem system = ActorSystem.create("NodeSystem", akkaConfig);
		ActorRef proxy = system.actorOf(
				new Props().withCreator(new ProxyCreator()),
				"ProxyActor");
		return getRemoteAddress(proxy, system);
	}

	public String getRemoteAddress(ActorRef actorRef, ActorSystem actorSystem) {
		Address dummyDestination = new Address("akka", "sys", "someHost", 42); // see
																				// http://groups.google.com/group/akka-user/browse_thread/thread/9448d8f628d38cc0
		Option<Address> akkaSystemAddress = ((ExtendedActorSystem) actorSystem)
				.provider().getExternalAddressFor(dummyDestination);
		String nodeProvisionerAddress = actorRef.path().toStringWithAddress(
				akkaSystemAddress.get());
		return nodeProvisionerAddress;
	}
}
