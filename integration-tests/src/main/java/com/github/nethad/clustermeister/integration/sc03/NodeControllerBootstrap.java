package com.github.nethad.clustermeister.integration.sc03;

import java.io.Serializable;
import java.util.concurrent.Callable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.typesafe.config.Config;

public class NodeControllerBootstrap implements Callable<String>, Serializable {

	private static final long serialVersionUID = 1L;

	protected String nodeProvisionerAddress = null;
	protected Config akkaConfig = null;

	public NodeControllerBootstrap(String nodeProvisionerAddress,
			Config akkaConfig) {
		this.nodeProvisionerAddress = nodeProvisionerAddress;
		this.akkaConfig = akkaConfig;
	}

	@SuppressWarnings("unused")
	@Override
	public String call() throws Exception {
		ActorSystem system = ActorSystem.create("NodeControllerSystem", akkaConfig);
		ActorRef nodeController = system.actorOf(
				new Props().withCreator(new NodeControllerCreator(
						nodeProvisionerAddress)), "Registrar");
		return "NodeController actor has been started.";
	}
}
