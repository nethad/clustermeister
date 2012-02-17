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

package org.jppf.server.nio.client;

import java.nio.channels.SelectionKey;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class ClientNioServer extends NioServer<ClientState, ClientTransition>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Reference to the driver.
   */
  private static JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Initialize this server with a specified port number.
   * @param port the port this socket server is listening to.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public ClientNioServer(final int port) throws Exception
  {
    this(new int[] { port });
  }

  /**
   * Initialize this server with the specified port numbers.
   * @param ports the ports this socket server is listening to.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public ClientNioServer(final int[] ports) throws Exception
  {
    super(ports, NioServer.CLIENT_SERVER, false);
    this.selectTimeout = NioServer.DEFAULT_SELECT_TIMEOUT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected NioServerFactory<ClientState, ClientTransition> createFactory()
  {
    return new ClientServerFactory(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void postAccept(final ChannelWrapper channel)
  {
    try
    {
      transitionManager.transitionChannel(channel, ClientTransition.TO_WAITING_HANDSHAKE);
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      closeClient(channel);
    }
    driver.getStatsManager().newClientConnection();
  }

  /**
   * Define a context for a newly created channel.
   * @return an <code>NioContext</code> instance.
   * @see org.jppf.server.nio.NioServer#createNioContext()
   */
  @Override
  public NioContext createNioContext()
  {
    return new ClientContext();
  }

  /**
   * Get the IO operations a connection is initially interested in.
   * @return a bit-wise combination of the interests, taken from
   * {@link java.nio.channels.SelectionKey SelectionKey} constants definitions.
   * @see org.jppf.server.nio.NioServer#getInitialInterest()
   */
  @Override
  public int getInitialInterest()
  {
    return SelectionKey.OP_READ;
  }

  /**
   * Close a connection to a node.
   * @param channel a <code>SocketChannel</code> that encapsulates the connection.
   */
  public static void closeClient(final ChannelWrapper<?> channel)
  {
    if (JPPFDriver.JPPF_DEBUG) driver.getInitializer().getServerDebug().removeChannel(channel, CLIENT_SERVER);
    try
    {
      channel.close();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
    try
    {
      driver.getStatsManager().clientConnectionClosed();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }
}
