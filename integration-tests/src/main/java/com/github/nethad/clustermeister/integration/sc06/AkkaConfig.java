package com.github.nethad.clustermeister.integration.sc06;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class AkkaConfig {

	public static Config get() {
		return ConfigFactory.parseString(akkaConfig).withFallback(ConfigFactory.load());
	}

	static String akkaConfig = "akka {\n" + 
			"  #logConfigOnStart=on\n" + 
			"  loglevel = DEBUG\n" + 
			"  actor {\n" + 
			"    #serialize-messages = on\n" + 
			"    provider = \"akka.remote.RemoteActorRefProvider\"\n" + 
			"    \n" + 
			"  	pinned-dispatcher {\n" + 
			"	  type = PinnedDispatcher\n" + 
			"	  executor = \"thread-pool-executor\"\n" + 
			"  	}\n" + 
			"\n" + 
			"    serializers {\n" + 
			"      java = \"akka.serialization.JavaSerializer\"\n" + 
			"    }\n" + 
			"\n" + 
			"    deployment {\n" + 
			"\n" + 
			"      default {\n" + 
			"\n" + 
			"        # if this is set to a valid remote address, the named actor will be deployed\n" + 
			"        # at that node e.g. \"akka://sys@host:port\"\n" + 
			"        remote = \"\"\n" + 
			"\n" + 
			"        target {\n" + 
			"\n" + 
			"          # A list of hostnames and ports for instantiating the children of a\n" + 
			"          # non-direct router\n" + 
			"          #   The format should be on \"akka://sys@host:port\", where:\n" + 
			"          #    - sys is the remote actor system name\n" + 
			"          #    - hostname can be either hostname or IP address the remote actor\n" + 
			"          #      should connect to\n" + 
			"          #    - port should be the port for the remote server on the other node\n" + 
			"          # The number of actor instances to be spawned is still taken from the\n" + 
			"          # nr-of-instances setting as for local routers; the instances will be\n" + 
			"          # distributed round-robin among the given nodes.\n" + 
			"          nodes = []\n" + 
			"\n" + 
			"        }\n" + 
			"      }\n" + 
			"    }\n" + 
			"  }\n" + 
			"\n" + 
			"  remote {\n" + 
			"\n" + 
			"    # Which implementation of akka.remote.RemoteTransport to use\n" + 
			"    # default is a TCP-based remote transport based on Netty\n" + 
			"    transport = \"akka.remote.netty.NettyRemoteTransport\"\n" + 
			"\n" + 
			"    # Enable untrusted mode for full security of server managed actors, allows\n" + 
			"    # untrusted clients to connect.\n" + 
			"    untrusted-mode = off\n" + 
			"\n" + 
			"    # Timeout for ACK of cluster operations, lik checking actor out etc.\n" + 
			"    remote-daemon-ack-timeout = 30s\n" + 
			"\n" + 
			"    # If this is \"on\", Akka will log all inbound messages at DEBUG level, if off then they are not logged\n" + 
			"    log-received-messages = on\n" + 
			"\n" + 
			"    # If this is \"on\", Akka will log all outbound messages at DEBUG level, if off then they are not logged\n" + 
			"    log-sent-messages = on\n" + 
			"\n" + 
			"    # Each property is annotated with (I) or (O) or (I&O), where I stands for “inbound” and O for “outbound” connections.\n" + 
			"    # The NettyRemoteTransport always starts the server role to allow inbound connections, and it starts\n" + 
			"    # active client connections whenever sending to a destination which is not yet connected; if configured\n" + 
			"    # it reuses inbound connections for replies, which is called a passive client connection (i.e. from server\n" + 
			"    # to client).\n" + 
			"    netty {\n" + 
			"\n" + 
			"      # (O) In case of increased latency / overflow how long\n" + 
			"      # should we wait (blocking the sender) until we deem the send to be cancelled?\n" + 
			"      # 0 means \"never backoff\", any positive number will indicate time to block at most.\n" + 
			"      backoff-timeout = 0ms\n" + 
			"\n" + 
			"      # (I&O) Generate your own with '$AKKA_HOME/scripts/generate_config_with_secure_cookie.sh'\n" + 
			"      #     or using 'akka.util.Crypt.generateSecureCookie'\n" + 
			"      secure-cookie = \"\"\n" + 
			"\n" + 
			"      # (I) Should the remote server require that it peers share the same secure-cookie\n" + 
			"      # (defined in the 'remote' section)?\n" + 
			"      require-cookie = off\n" + 
			"\n" + 
			"      # (I) Reuse inbound connections for outbound messages\n" + 
			"      use-passive-connections = on\n" + 
			"\n" + 
			"      # (I) The hostname or ip to bind the remoting to,\n" + 
			"      # InetAddress.getLocalHost.getHostAddress is used if empty\n" + 
			"      hostname = \"\"\n" + 
			"\n" + 
			"      # (I) The default remote server port clients should connect to.\n" + 
			"      # Default is 2552 (AKKA), use 0 if you want a random available port\n" + 
			"      port = 0\n" + 
			"\n" + 
			"      # (O) The address of a local network interface (IP Address) to bind to when creating\n" + 
			"      # outbound connections. Set to \"\" or \"auto\" for automatic selection of local address.\n" + 
			"      outbound-local-address = \"auto\"\n" + 
			"\n" + 
			"      # (I&O) Increase this if you want to be able to send messages with large payloads\n" + 
			"      message-frame-size = 1 MiB\n" + 
			"\n" + 
			"      # (O) Timeout duration\n" + 
			"      connection-timeout = 120s\n" + 
			"\n" + 
			"      # (I) Sets the size of the connection backlog\n" + 
			"      backlog = 4096\n" + 
			"\n" + 
			"      # (I) Length in akka.time-unit how long core threads will be kept alive if idling\n" + 
			"      execution-pool-keepalive = 60s\n" + 
			"\n" + 
			"      # (I) Size of the core pool of the remote execution unit\n" + 
			"      execution-pool-size = 4\n" + 
			"\n" + 
			"      # (I) Maximum channel size, 0 for off\n" + 
			"      max-channel-memory-size = 0b\n" + 
			"\n" + 
			"      # (I) Maximum total size of all channels, 0 for off\n" + 
			"      max-total-memory-size = 0b\n" + 
			"\n" + 
			"      # (O) Time between reconnect attempts for active clients\n" + 
			"      reconnect-delay = 5s\n" + 
			"\n" + 
			"      # (O) Read inactivity period (lowest resolution is seconds)\n" + 
			"      # after which active client connection is shutdown;\n" + 
			"      # will be re-established in case of new communication requests.\n" + 
			"      # A value of 0 will turn this feature off\n" + 
			"      read-timeout = 0s\n" + 
			"\n" + 
			"      # (O) Write inactivity period (lowest resolution is seconds)\n" + 
			"      # after which a heartbeat is sent across the wire.\n" + 
			"      # A value of 0 will turn this feature off\n" + 
			"      write-timeout = 10s\n" + 
			"\n" + 
			"      # (O) Inactivity period of both reads and writes (lowest resolution is seconds)\n" + 
			"      # after which active client connection is shutdown;\n" + 
			"      # will be re-established in case of new communication requests\n" + 
			"      # A value of 0 will turn this feature off\n" + 
			"      all-timeout = 0s\n" + 
			"\n" + 
			"      # (O) Maximum time window that a client should try to reconnect for\n" + 
			"      reconnection-time-window = 600s\n" + 
			"    }\n" + 
			"\n" + 
			"    # The dispatcher used for the system actor \"network-event-sender\"\n" + 
			"    network-event-sender-dispatcher {\n" + 
			"      executor = thread-pool-executor\n" + 
			"      type = PinnedDispatcher\n" + 
			"    }\n" + 
			"  }\n" + 
			"}";

}
