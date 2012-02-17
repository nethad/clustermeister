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

import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class WaitingResultsState extends NodeServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingResultsState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitingResultsState(final NodeNioServer server)
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
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (context.getNodeMessage() == null) context.setNodeMessage(context.newMessage(), channel);
    if (context.readMessage(channel))
    {
      ServerJob bundleWrapper = context.getBundle();
      JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
      BundleWrapper newBundleWrapper = context.deserializeBundle();
      JPPFTaskBundle newBundle = (JPPFTaskBundle) newBundleWrapper.getJob();
      if (debugEnabled) log.debug("read bundle" + newBundle + " from node " + channel + " done");
      // if an exception prevented the node from executing the tasks
      if (newBundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM) != null)
      {
        if (debugEnabled)
        {
          Throwable t = (Throwable) newBundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM);
          log.debug("node " + channel + " returned exception parameter in the header for job '" + newBundle.getName() + "' : " + t);
        }
        newBundleWrapper.setTasks(bundleWrapper.getTasks());
        newBundle.setTaskCount(bundle.getTaskCount());
      }
      else
      {
        long elapsed = System.nanoTime() - bundle.getExecutionStartTime();
        statsManager.taskExecuted(newBundle.getTaskCount(), elapsed / 1000000L, newBundle.getNodeExecutionTime(), context.getNodeMessage().getLength());
        context.getBundler().feedback(newBundle.getTaskCount(), elapsed);
      }
      boolean requeue = (Boolean) newBundle.getParameter(BundleParameter.JOB_REQUEUE, false);
      jobManager.jobReturned(bundleWrapper, channel);
      if (requeue)
      {
        bundle.setParameter(BundleParameter.JOB_REQUEUE, true);
        bundle.getSLA().setSuspended(true);
        context.setBundle(null);
        context.setJobCanceled(false);
        context.resubmitBundle(bundleWrapper);
      }
      else
      {
        TaskCompletionListener listener = bundle.getCompletionListener();
        // notify the client thread about the end of a bundle
        if (listener != null) listener.taskCompleted(context.isJobCanceled() ? bundleWrapper : newBundleWrapper);
        context.setJobCanceled(false);
      }
      Bundler bundler = context.getBundler();
      JPPFSystemInformation systemInfo = (JPPFSystemInformation) bundle.getParameter(BundleParameter.NODE_SYSTEM_INFO_PARAM);
      if ((systemInfo != null) && (bundler instanceof NodeAwareness)) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
      // there is nothing left to do, so this instance will wait for a task bundle
      // make sure the context is reset so as not to resubmit the last bundle executed by the node.
      context.setNodeMessage(null, channel);
      context.setBundle(null);
      server.addIdleChannel(channel);
      return TO_IDLE;
    }
    return TO_WAITING;
  }
}
