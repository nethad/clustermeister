package com.github.nethad.clustermeister.integration.sc06;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

//import static akka.actor.Actors.*;

public class ProxyActor extends UntypedActor implements Node {

	public LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public void onReceive(Object message) throws Exception {
		log.info("Received message" + message);
            
		if (message instanceof Request<?>) {
			@SuppressWarnings("unchecked")
			Request<Node> request = (Request<Node>) message;
			Object result = request.command().apply(this);
			if (request.returnResult()) {
				sender().tell(result);
			}
		}
		unhandled(message);
	}

	@Override
	public Integer numberOfCores() {
		return Runtime.getRuntime().availableProcessors();
	}
}
