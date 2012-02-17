/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.management;

import javax.management.MBeanServer;

/**
 * Interface for JMX remote servers associated to drivers and nodes.
 * @author Laurent Cohen
 */
public interface JMXServer
{
  /**
   * Start the MBean server and associated resources.
   * @param cl - the default classloader to be used by the JMX remote connector.
   * @throws Exception if an error occurs when starting the server or one of its components.
   */
  void start(ClassLoader cl) throws Exception;

  /**
   * Stop the MBean server and associated resources.
   * @throws Exception if an error occurs when stopping the server or one of its components.
   */
  void stop() throws Exception;

  /**
   * Get a reference to the MBean server.
   * @return an <code>MBeanServer</code> instance.
   */
  MBeanServer getServer();

  /**
   * Determine whether this JMX server is stopped.
   * @return <code>true</code> if this JMX server is stopped, <code>false</code> otherwise.
   */
  boolean isStopped();

  /**
   * Get a unique identifier for this management server. This id must be unique across JPPF nodes and servers.
   * @return the id as a string.
   */
  String getId();

  /**
   * Get the host interface on which the JMX server is listeneing for connections.
   * @return the host as a string.
   */
  String getManagementHost();

  /**
   * Get the port on which the connector is listening for connections from remote clients.
   * @return the port number as an int.
   */
  int getManagementPort();
}
