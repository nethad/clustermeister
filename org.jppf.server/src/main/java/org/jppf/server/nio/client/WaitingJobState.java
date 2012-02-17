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

import static org.jppf.server.nio.client.ClientTransition.*;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class WaitingJobState extends ClientServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingJobState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitingJobState(final ClientNioServer server)
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
  public ClientTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    ClientContext context = (ClientContext) channel.getContext();
    if (context.getClientMessage() == null) context.setClientMessage(context.newMessage());
    if (context.readMessage(channel))
    {
      ServerJob bundleWrapper = context.deserializeBundle();
      JPPFTaskBundle header = (JPPFTaskBundle) bundleWrapper.getJob();
      int count = header.getTaskCount();
      if (debugEnabled) log.debug("read bundle" + header + " from client " + channel + " done: received " + count + " tasks");
      if (header.getParameter(BundleParameter.JOB_RECEIVED_TIME) == null)
        header.setParameter(BundleParameter.JOB_RECEIVED_TIME, System.currentTimeMillis());

      header.getUuidPath().incPosition();
      header.getUuidPath().add(driver.getUuid());
      if (debugEnabled) log.debug("uuid path=" + header.getUuidPath().getList());
      header.setCompletionListener(new CompletionListener(channel));
      context.setPendingTasksCount(header.getTaskCount());
      context.setInitialBundleWrapper(bundleWrapper);
      context.setCurrentJobId(header.getUuid());
      JPPFDriver.getQueue().addBundle(bundleWrapper);

      // there is nothing left to do, so this instance will wait for a task bundle
      // make sure the context is reset so as not to resubmit the last bundle executed by the node.
      context.setClientMessage(null);
      context.setBundle(null);
      return TO_SENDING_RESULTS;
    }
    return TO_WAITING_JOB;
  }
}
