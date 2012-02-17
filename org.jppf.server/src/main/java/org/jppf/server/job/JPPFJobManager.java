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

package org.jppf.server.job;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.job.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.*;

/**
 * Instances of this class manage and monitor the jobs throughout their processing within the JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFJobManager implements QueueListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFJobManager.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of jobs to the nodes they are executing on.
   */
  private final Map<String, List<ChannelJobPair>> jobMap = new HashMap<String, List<ChannelJobPair>>();
  /**
   * Mapping of job ids to the corresponding <code>JPPFTaskBundle</code>.
   */
  private final Map<String, ServerJob> bundleMap = new HashMap<String, ServerJob>();
  /**
   * Processes the event queue asynchronously.
   */
  private final ExecutorService executor;
  /**
   * The list of registered listeners.
   */
  private final List<JobListener> eventListeners = new ArrayList<JobListener>();

  /**
   * Default constructor.
   */
  public JPPFJobManager()
  {
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("JobManager"));
  }

  /**
   * Get all the nodes to which a all or part of a job is dispatched.
   * @param jobUuid the id of the job.
   * @return a list of <code>SelectableChannel</code> instances.
   */
  public synchronized List<ChannelJobPair> getNodesForJob(final String jobUuid)
  {
    if (jobUuid == null) return Collections.emptyList();
    List<ChannelJobPair> list = jobMap.get(jobUuid);
    return list == null ? Collections.<ChannelJobPair>emptyList() : Collections.unmodifiableList(list);
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return a set of ids as strings.
   */
  public synchronized Set<String> getAllJobIds()
  {
    return Collections.unmodifiableSet(jobMap.keySet());
  }

  /**
   * Get the queued bundle wrapper for the specified job.
   * @param jobUuid the id of the job to look for.
   * @return a <code>BundleWrapper</code> instance, or null if the job is not queued anymore.
   */
  public synchronized ServerJob getBundleForJob(final String jobUuid)
  {
    return bundleMap.get(jobUuid);
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param bundleWrapper the dispatched job.
   * @param channel the node to which the job is dispatched.
   */
  public synchronized void jobDispatched(final ServerJob bundleWrapper, final ChannelWrapper channel)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    String jobUuid = bundle.getUuid();
    List<ChannelJobPair> list = jobMap.get(jobUuid);
    if (list == null)
    {
      list = new ArrayList<ChannelJobPair>();
      jobMap.put(jobUuid, list);
    }
    list.add(new ChannelJobPair(channel, bundleWrapper));
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : added node " + channel);
    submitEvent(JobEventType.JOB_DISPATCHED, bundle, channel);
  }

  /**
   * Called when all or part of a job has returned from a node.
   * @param bundleWrapper the returned job.
   * @param channel the node to which the job is dispatched.
   */
  public synchronized void jobReturned(final ServerJob bundleWrapper, final ChannelWrapper channel)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    String jobUuid = bundle.getUuid();
    List<ChannelJobPair> list = jobMap.get(jobUuid);
    if (list == null)
    {
      log.info("attempt to remove node " + channel + " but JobManager shows no node for jobId = " + bundle.getName());
      return;
    }
    list.remove(new ChannelJobPair(channel, bundleWrapper));
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : removed node " + channel);
    submitEvent(JobEventType.JOB_RETURNED, bundle, channel);
  }

  /**
   * Called when a job is added to the server queue.
   * @param bundleWrapper the queued job.
   */
  public synchronized void jobQueued(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    String jobUuid = bundle.getUuid();
    bundleMap.put(jobUuid, bundleWrapper);
    jobMap.put(jobUuid, new ArrayList<ChannelJobPair>());
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' queued");
    submitEvent(JobEventType.JOB_QUEUED, bundle, null);
    JPPFDriver.getInstance().getStatsUpdater().jobQueued(bundle.getTaskCount());
  }

  /**
   * Called when a job is complete and returned to the client.
   * @param bundleWrapper the completed job.
   */
  public synchronized void jobEnded(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    long time = System.currentTimeMillis() - (Long) bundle.getParameter(BundleParameter.JOB_RECEIVED_TIME);
    String jobUuid = bundle.getUuid();
    jobMap.remove(jobUuid);
    bundleMap.remove(jobUuid);
    JPPFDriver.getInstance();
    ((JPPFPriorityQueue) JPPFDriver.getQueue()).clearSchedules(jobUuid);
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' ended");
    submitEvent(JobEventType.JOB_ENDED, bundle, null);
    JPPFDriver.getInstance().getStatsUpdater().jobEnded(time);
  }

  /**
   * Called when a job is added to the server queue.
   * @param bundleWrapper the queued job.
   */
  public synchronized void jobUpdated(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' updated");
    submitEvent(JobEventType.JOB_UPDATED, bundle, null);
  }

  /**
   * Called when a queue event occurs.
   * @param event a queue event.
   * @see org.jppf.server.queue.QueueListener#newBundle(org.jppf.server.queue.QueueEvent)
   */
  @Override
  public void newBundle(final QueueEvent event)
  {
    if (!event.isRequeued()) jobQueued(event.getBundleWrapper());
    else jobUpdated(event.getBundleWrapper());
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param bundle the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final JPPFTaskBundle bundle, final ChannelWrapper channel)
  {
    executor.submit(new JobEventTask(this, eventType, bundle, channel));
  }

  /**
   * Close this job manager and release its resources.
   */
  public synchronized void close()
  {
    executor.shutdownNow();
    jobMap.clear();
    bundleMap.clear();
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addJobListener(final JobListener listener)
  {
    synchronized(eventListeners)
    {
      eventListeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeJobListener(final JobListener listener)
  {
    synchronized(eventListeners)
    {
      eventListeners.remove(listener);
    }
  }

  /**
   * Fire job listener event.
   * @param event the event to be fired.
   */
  public void fireJobEvent(final JobNotification event)
  {
    if(event == null) throw new IllegalArgumentException("event is null");

    synchronized(eventListeners)
    {
      switch (event.getEventType())
      {
        case JOB_QUEUED:
          for (JobListener listener: eventListeners) listener.jobQueued(event);
          break;

        case JOB_ENDED:
          for (JobListener listener: eventListeners) listener.jobEnded(event);
          break;

        case JOB_UPDATED:
          for (JobListener listener: eventListeners) listener.jobUpdated(event);
          break;

        case JOB_DISPATCHED:
          for (JobListener listener: eventListeners) listener.jobDispatched(event);
          break;

        case JOB_RETURNED:
          for (JobListener listener: eventListeners) listener.jobReturned(event);
          break;

        default:
          throw new IllegalStateException("Unsupported event type: " + event.getEventType());
      }
    }
  }
}
