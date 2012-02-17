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

package org.jppf.server.peer;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.discovery.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class discover peer drivers over the network.
 * @author Laurent Cohen
 */
public class PeerDiscoveryThread extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PeerNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Contains the set of retrieved connection information objects.
   */
  private final Set<JPPFConnectionInformation> infoSet = new HashSet<JPPFConnectionInformation>();
  /**
   * Count of distinct retrieved connection information objects.
   */
  private final AtomicInteger count = new AtomicInteger(0);
  /**
   * Connection information for this JPPF driver.
   */
  private final JPPFConnectionInformation localInfo;

  /**
   * Defines a callback for objects wishing to be notified of discovery events.
   */
  private final ConnectionHandler connectionHandler;
  /**
   * Holds a set of filters to include or exclude sets of IP addresses in the discovery process.
   */
  private final IPFilter ipFilter;

  /**
   * Default constructor.
   * @param connectionHandler handler for adding new connection
   * @param ipFilter for accepted IP addresses
   * @param localInfo Connection information for this JPPF driver.
   */
  public PeerDiscoveryThread(final ConnectionHandler connectionHandler, final IPFilter ipFilter, final JPPFConnectionInformation localInfo)
  {
    if(localInfo == null) throw new IllegalArgumentException("localInfo is null");
    if(connectionHandler == null) throw new IllegalArgumentException("connectionHandler is null");

    this.connectionHandler = connectionHandler;
    this.ipFilter = ipFilter;
    this.localInfo = localInfo;
  }

  /**
   * Lookup server configurations from UDP multicasts.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    JPPFMulticastReceiver receiver = null;
    try
    {
      receiver = new JPPFMulticastReceiver(ipFilter);
      while (!isStopped())
      {
        JPPFConnectionInformation info = receiver.receive();
        if ((info != null) && !hasConnectionInformation(info))
        {
          if (debugEnabled) log.debug("Found peer connection information: " + info);
          addConnectionInformation(info);
          onNewConnection("Peer-" + count.incrementAndGet(), info);
        }
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
    finally
    {
      if(receiver != null) receiver.setStopped(true);
    }
  }

  /**
   * Add a newly found connection.
   * @param name for the connection
   * @param info the peer's connection information.
   */
  protected synchronized void onNewConnection(final String name, final JPPFConnectionInformation info)
  {
    connectionHandler.onNewConnection(name, info);
  }

  /**
   * Determine whether a connection information object is already discovered.
   * @param info the connection information to lookup.
   * @return true if the connection information is in the map, false otherwise.
   */
  protected boolean hasConnectionInformation(final JPPFConnectionInformation info)
  {
    return infoSet.contains(info) || info.equals(localInfo) || isSelf(info);
  }

  /**
   * Add the specified connection information to discovered map.
   * @param info a {@link JPPFConnectionInformation} instance.
   */
  public synchronized void addConnectionInformation(final JPPFConnectionInformation info)
  {
    infoSet.add(info);
  }

  /**
   * Remove a disconnected connection.
   * @param info connection info of the peer to remove
   * @return whether connection was successfully removed
   */
  public synchronized boolean removeConnectionInformation(final JPPFConnectionInformation info)
  {
    return infoSet.remove(info);
  }

  /**
   * Determine whether the specified connection information refers to this driver.
   * This situation may arise if the host has multiple network interfaces, each with its own IP address.
   * Making this distinction is important to prevent a driver from connecting to itself.
   * @param info the peer's connection information.
   * @return true if the host/port combination in the connection information can be resolved
   * as the configuration for this driver.
   */
  private boolean isSelf(final JPPFConnectionInformation info)
  {
    List<InetAddress> ipv4Addresses = NetworkUtils.getIPV4Addresses();
    for (InetAddress addr: ipv4Addresses)
    {
      String ip = addr.getHostAddress();
      if (info.host.equals(ip) && Arrays.equals(info.serverPorts, localInfo.serverPorts))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Defines a callback for objects wishing to be notified of discovery events.
   */
  public interface ConnectionHandler
  {
    /**
     * Called when a new connection is discovered.
     * @param name the name assigned to the connection.
     * @param info the information required to connect to the driver.
     */
    void onNewConnection(final String name, final JPPFConnectionInformation info);
  }
}
