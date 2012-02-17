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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.net.ConnectException;

import org.jppf.server.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * This class represents the state of sending the initial hand-shaking data to a newly connected node.
 * @author Laurent Cohen
 */
class SendInitialBundleState extends NodeServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendInitialBundleState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public SendInitialBundleState(final NodeNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    //if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));
    if (channel.isReadable() && !(channel instanceof LocalNodeChannel)) throw new ConnectException("node " + channel + " has been disconnected");

    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (context.getNodeMessage() == null)
    {
      if (debugEnabled) log.debug("serializing initial bundle for " + channel);
      context.serializeBundle(channel);
    }
    if (context.writeMessage(channel))
    {
      if (debugEnabled) log.debug("sent entire initial bundle for " + channel);
      context.setNodeMessage(null, channel);
      context.setBundle(null);
      return TO_WAIT_INITIAL;
    }
    if (debugEnabled) log.debug("part yet to send for " + channel);
    return TO_SEND_INITIAL;
  }
}
