/*
 * Copyright 2012 The Clustermeister Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nethad.clustermeister.api;

/**
 * This class makes JPPF configuration system properties and values available 
 * as constants.
 *
 * @author daniel
 */
public class JPPFConstants {
    
    /**
     * Server broadcast and automatic discovery.
     */
    public static final String DISCOVERY_ENABLED = "jppf.discovery.enabled"; //default true
    
    /**
     * UDP broadcast group.
     */
    public static final String DISCOVERY_GROUP = "jppf.discovery.group";
    
    /**
     * Default value for {@link #DISCOVERY_GROUP}.
     * 
     * value = 230.0.0.1
     */
    public static final String DEFAULT_DISCOVERY_GROUP = "230.0.0.1";
    
    /**
     * UDP broadcast port.
     */
    public static final String DISCOVERY_PORT = "jppf.discovery.port";
    
    /**
     * Default value for {@link #DISCOVERY_PORT}.
     * 
     * value = 11111
     */
    public static final String DEFAULT_DISCOVERY_PORT = "11111";
    
    /**
     * UDP broadcast interval in milliseconds.
     */
    public static final String DISCOVERY_BROADCAST_INTERVAL = "jppf.discovery.broadcast.interval"; //default 5000
    
    /**
     * Discovery timeout in milliseconds.
     */
    public static final String DISCOVERY_TIMEOUT = "jppf.discovery.timeout"; //default 5000
    
    /**
     * JPPF server host address.
     */
    public static final String SERVER_HOST = "jppf.server.host";
    
    /**
     * Default value for {@link #SERVER_HOST}.
     * 
     * value = localhost
     */
    public static final String DEFAULT_SERVER_HOST = "localhost";
    
    /**
     * TCP port for the JPPF Driver (Server) to listen for client and node 
     * connections.
     */
    public static final String SERVER_PORT = "jppf.server.port";
    
    /**
     * Default value for {@link #SERVER_PORT}.
     * 
     * value = 11111
     */
    public static final int DEFAULT_SERVER_PORT = 11111;
    
    /**
     * Enable JMX management (and MBeans).
     */
    public static final String MANAGEMENT_ENABLED = "jppf.management.enabled"; //default true.
    
    /**
     * Host for JPPF Nodes and Drivers to listen to remote JMX connections.
     */
    public static final String MANAGEMENT_HOST = "jppf.management.host"; //computed by default.
    
    /**
     * TCP port for JPPF Nodes and Drivers to listen to remote JMX connections.
     */
    public static final String MANAGEMENT_PORT = "jppf.management.port";
    
    /**
     * Default value for {@link #MANAGEMENT_PORT}.
     * 
     * value = 11189
     */
    public static final int DEFAULT_MANAGEMENT_PORT = 11198;
    
    /**
     * TCP port for JPPF Management (JMX) RMI Registry.
     */
    public static final String MANAGEMENT_RMI_PORT = "jppf.management.rmi.port";
    
    /**
     * Default value for {@link #MANAGEMENT_RMI_PORT}.
     * 
     * value = 12198
     */
    public static final int DEFAULT_MANAGEMENT_RMI_PORT = 12198;
    
    /**
     * Automatic peer discovery.
     */
    public static final String PEER_DISCOVERY_ENABLED = "jppf.peer.discovery.enabled"; //default false
    
    /**
     * Space separated list of peer server names.
     */
    public static final String PEERS = "jppf.peers"; //default null
    
    /**
     * Peer server host.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #PEERS}.
     */
    public static final String PEER_SERVER_HOST_PATTERN = "jppf.peer.%s.server.host"; //default localhost
    
    /**
     * Peer port host.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #PEERS}.
     */
    public static final String PEER_SERVER_PORT_PATTERN = "jppf.peer.%s.server.port"; //default 11111
    
    /**
     * Load balancing algorithm name.
     * 
     * Each set of tasks sent to a node is called a "bundle", and the role of 
     * the load balancing (or task scheduling) algorithm is to optimize the 
     * performance by adjusting the number of task sent to each node.
     * 
     * The algorithm name can be one of those prefefined in JPPF, 
     * or a user-defined one. 
     * 
     * The predefined possible values for this property are: 
     * <ul>
     *  <li>
     *      manual: each bundle has a fixed number of tasks, meaning that each 
     *      will receive at most this number of tasks.
     *  </li>
     *  <li>
     *      autotuned: adaptive heuristic algorithm based on the Monte Carlo 
     *      algorithm.
     *  </li>
     *  <li>
     *      proportional: an adaptive deterministic algorithm based on the 
     *      contribution of each node to the overall mean task execution time.
     *  </li>
     *  <li>
     *      rl: adaptive algorithm based on an artificial intelligence 
     *      technique called "reinforcement learning".
     *  </li>
     *  <li>
     *      NodeThreads: each bundle will have at most n * m tasks, where n is 
     *      the number of threads in the node to which the bundle is sent is 
     *      sent, and m is a user-defined parameter.
     *  </li>
     * </ul>
     * 
     * Default is proportional (TODO: verify).
     */
    public static final String LOAD_BALANCING_ALGORITHM = "jppf.load.balancing.algorithm"; //default proportional
    
    /**
     * Load balancing parameters profile name.
     * 
     * Each algorithm uses its own set of parameters, which define together a 
     * strategy for the algorithm. A strategy has a name that serves to identify 
     * a group of parameters and their values, using the following pattern:
     * <br/><br/>
     * jppf.load.balancing.strategy = &lt;profile_name>
     * <br/>
     * strategy.&lt;profile_name>.&lt;parameter1> = &lt;value1>
     * <br/>
     * strategy.&lt;profile_name>.&lt;parameter2> = &lt;value2>
     * <br/>
     * ...
     */
    public static final String LOAD_BALANCING_STRATEGY = "jppf.load.balancing.strategy"; //default jppf
    
    /**
     * Delay in seconds before the first reconnection attempt. 
     */
    public static final String RECONNECT_INITIAL_DELAY = "reconnect.initial.delay"; //default 1
    
    /**
     * Delay in seconds after which reconnection attempts stop. 
     * 
     * Negative value = never stop.
     */
    public static final String RECONNECT_MAX_TIME = "reconnect.max.time"; //default 60
    
    /**
     * Frequency of reconnection attempts in seconds.
     */
    public static final String RECONNECT_INTERVAL = "reconnect.interval"; //default 1
    
    /**
     * Number of threads used for task executions.
     */
    public static final String PROCESSING_THREADS = "processing.threads"; //defaults to number of CPUs.
    
    /**
     * Fully qualified class_name of a class implementing the interface 
     * JPPFConfiguration.ConfigurationSource, enabling a configuration source 
     * from any origin, such as a URL, a distributed file system, a remote 
     * storage facility, a database, etc.
     */
    public static final String CONFIG_PLUGIN = "jppf.config.plugin";
    
    /**
     * JPPF UUID system property.
     */
    public static final String UUID = "jppf.uuid";
    
    /**
     * JVM options.
     */
    public static final String JVM_OPTIONS = "jppf.jvm.options";
    
    /**
     * Optional object stream builder.
     */
    public static final String OBJECT_STREAM_BUILDER = "jppf.object.stream.builder"; //default null
    
    /**
     * Optional alternate object input stream.
     */
    public static final String OBJECT_INPUT_STREAM_CLASS = "jppf.object.input.stream.class"; //default java.io.ObjectInputStream
    
    /**
     * Optional alternate object output stream.
     */
    public static final String OBJECT_OUTPUT_STREAM_CLASS = "jppf.object.output.stream.class"; //default java.io.ObjectOutputStream
    
    /**
     * Optional network data transformation.
     */
    public static final String DATA_TRANSFORM_CLASS = "jppf.data.transform.class"; //null
    
    /**
     * Number of threads performing network I/O.
     */
    public static final String TRANSITION_THREAD_POOL_SIZE = "transition.thread.pool.size"; //number of available CPUs
    
    /**
     * Enable a node to run in the same JVM as the driver.
     */
    public static final String LOCAL_NODE_ENABLED = "jppf.local.node.enabled"; //false
    
    /**
     * Enable recovery from hardware failures on the nodes.
     */
    public static final String RECOVERY_ENABLED = "jppf.recovery.enabled"; //true
    
    /**
     * Maximum number of pings to the node before the connection is considered
     * broken.
     */
    public static final String RECOVERY_MAXRETRIES = "jppf.recovery.max.retries"; //3
    
    /**
     * Maximum ping response time from the node.
     */
    public static final String RECOVERY_READ_TIMEOUT = "jppf.recovery.read.timeout"; //6000 (6s)
    
    /**
     * Port number for the detection of node failure.
     */
    public static final String RECOVERY_SERVER_PORT = "jppf.recovery.server.port";
    
    /**
     * Default value for {@link #RECOVERY_SERVER_PORT}.
     *     
     * value = 22222
     */
    public static final String DEFAULT_RECOVERY_SERVER_PORT = "jppf.recovery.server.port";
    
    /**
     * Interval between connection reaper runs.
     */
    public static final String RECOVERY_REAPER_RUN_INTERVAL = "jppf.recovery.reaper.run.interval"; //60000 (1 min)
   
    /**
     * Number of threads allocated to the reaper.
     */
    public static final String RECOVERY_REAPER_POOL_SIZE = "jppf.recovery.reaper.pool.size"; //number of available CPUs
    
    /**
     * Number of seconds a socket connection can remain idle before being
     * closed.
     */
    public static final String SOCKET_MAX_IDLE = "jppf.socket.max-idle"; //-1
    
    /**
     * Enable/disable network connection checks on write operations.
     */
    public static final String NIO_CONNECTION_CHECK = "jppf.nio.connection.check"; //true
    
    /**
     * IPv4 inclusion patterns for server discovery.
     */
    public static final String DISCOVERY_IPV4_INCLUDE = "jppf.discovery.ipv4.include"; //null
    
    /**
     * IPv4 exclusion patterns for server discovery.
     */
    public static final String DISCOVERY_IPV4_EXCLUDE = "jppf.discovery.ipv4.exclude"; //null
    
    /**
     * IPv6 inclusion patterns for server discovery.
     */
    public static final String DISCOVERY_IPV6_INCLUDE = "jppf.discovery.ipv6.include"; //null
    
    /**
     * IPv6 exclusion patterns for server discovery.
     */
    public static final String DISCOVERY_IPV6_EXCLUDE = "jppf.discovery.ipv6.exclude"; //null
    
    /**
     * Path to the security policy file, either local to the node or in the
     * server's file system.
     */
    public static final String POLICY_FILE = "jppf.policy.file"; //null
    
    /**
     * Enable the idle mode.
     */
    public static final String IDLE_MODE_ENABLED = "jppf.idle.mode.enabled"; //false
    
    /**
     * The time of keyboard and mouse inactivity before considering the node
     * idle, expressed in milliseconds.
     */
    public static final String IDLE_TIMEOUT = "jppf.idle.timeout"; //300000 (5 minutes)
    
    /**
     * How often the node will check for keyboard and mouse inactivity, in
     * milliseconds.
     */
    public static final String IDLE_POLL_INTERVAL = "jppf.idle.poll.interval"; //1000 (1 second)
    
    /**
     * Implementation of the idle detector factory.
     */
    public static final String IDLE_DETECTOR_FACTORY = "jppf.idle.detector.factory"; //null
    
    /**
     * Size of the class loader cache for the node.
     */
    public static final String CLASSLOADER_CACHE_SIZE = "jppf.classloader.cache.size"; //50
    
    /**
     * The classloader delegation strategy.
     */
    public static final String CLASSLOADER_DELEGATION = "jppf.classloader.delegation"; //parent
    
    /**
     * Space-separated list of driver names.
     */
    public static final String DRIVERS = "jppf.drivers"; //default-driver
    
    /**
     * Named driver's host or IP address.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #DRIVERS}.
     */
    public static final String DRIVER_SERVER_HOST_PATTERN = "%s.jppf.server.host"; //localhost
    
    /**
     * Named driver's port.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #DRIVERS}.
     */
    public static final String DRIVER_SERVER_PORT_PATTERN = "%s.jppf.server.port"; //11111
    
    /**
     * Named server's management server host.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #DRIVERS}.
     */
    public static final String DRIVER_MANAGEMENT_HOST_PATTERN = "%s.jppf.management.host"; //localhost
    
    /**
     * Named server's management remote connector port.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #DRIVERS}.
     */
    public static final String DRIVER_MANAGEMENT_PORT_PATTERN = "%s.jppf.management.port"; //11198
    
    /**
     * Enable remote management of named server.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #DRIVERS}.
     */
    public static final String DRIVER_MANAGEMENT_ENABLED_PATTERN = "%s.jppf.management.enabled"; //true
    
    /**
     * Named server priority.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #DRIVERS}.
     */
    public static final String DRIVER_PRIORITY_PATTERN = "%s.priority"; //0
    
    /**
     * Named server connection pool size.
     * 
     * This is a format string that can be used with 
     * {@link String#format(java.lang.String, java.lang.Object[])} to generate 
     * the final property for a specific peer.
     * 
     * Example: String.format(PATTERN, driverName), where 
     * driverName part of the list configured in {@link #DRIVERS}.
     */
    public static final String DRIVER_POOL_SIZE_PATTERN = "%s.jppf.pool.size"; //1
    
    /**
     * Maximum JPPF client initialization wait time.
     */
    public static final String CLIENT_MAX_INIT_TIME = "jppf.client.max.init.time"; //5000 (5 seconds)
    
    /**
     * Enable remote execution.
     */
    public static final String REMOTE_EXECUTION_ENABLED = "jppf.remote.execution.enabled"; //true
    
    /**
     * Enable local execution.
     */
    public static final String LOCAL_EXECUTION_ENABLED = "jppf.local.execution.enabled"; //false
    
    /**
     * Maximum threads to use for local execution.
     */
    public static final String LOCAL_EXECUTION_THREADS = "jppf.local.execution.threads"; //number of available CPUs
    
    /**
     * Connection pool size when discovery is enabled.
     */
    public static final String POOL_SIZE = "jppf.pool.size"; //1
    
    /**
     * Enable display of splash screen at startup.
     */
    public static final String UI_SPLASH = "jppf.ui.splash"; //true
    
    /**
     * How many completed tasks before notifying.
     */
    public static final String LOCAL_EXECUTION_ACCUMULATION_SIZE = "jppf.local.execution.accumulation.size"; //all tasks
    
    /**
     * How long before notifying.
     */
    public static final String LOCAL_EXECUTION_ACCUMULATION_TIME = "jppf.local.execution.accumulation.time"; //job completion time
    
    /**
     * Time unit to use.
     */
    public static final String LOCAL_EXECUTION_ACCUMULATION_UNIT = "jppf.local.execution.accumulation.unit"; //m = milliseconds 	
    
    /**
     * The path to the resource cache directory.
     */
    public static final String RESOURCE_CACHE_DIR = "jppf.resource.cache.dir";
}
