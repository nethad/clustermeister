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
package org.jppf.server.protocol;

import java.io.Serializable;
import java.util.*;

import org.jppf.node.protocol.*;
import org.jppf.utils.*;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 */
public class JPPFTaskBundle implements Serializable, Comparable<JPPFTaskBundle>, JPPFDistributedJob
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Type safe enumeration for the values of the bundle state.
   */
  public enum State
  {
    /**
     * Means the bundle is used for handshake with the server (initial bundle).
     */
    INITIAL_BUNDLE,
    /**
     * Means the bundle is used normally, to transport executable tasks.
     */
    EXECUTION_BUNDLE
  }

  /**
   * The unique identifier for this task bundle.
   */
  private String bundleUuid = null;
  /**
   * The unique identifier for the request (the job) this task bundle is a part of.
   */
  private String jobUuid = null;
  /**
   * The user-defined display name for this job.
   */
  private String name = null;
  /**
   * The unique identifier for the submitting application.
   */
  private TraversalList<String> uuidPath = new TraversalList<String>();
  /**
   * The number of tasks in this bundle.
   */
  private int taskCount = 0;
  /**
   * The initial number of tasks in this bundle.
   */
  private int initialTaskCount = 0;
  /**
   * The shared data provider for this task bundle.
   */
  private transient byte[] dataProvider = null;
  /**
   * The tasks to be executed by the node.
   */
  private transient List<byte[]> tasks = null;
  /**
   * The time at which this wrapper was added to the queue.
   */
  private transient long queueEntryTime = 0L;
  /**
   * The task completion listener to notify, once the execution of this task has completed.
   */
  private transient TaskCompletionListener completionListener = null;
  /**
   * The time it took a node to execute this task.
   */
  private long nodeExecutionTime = 0L;
  /**
   * The time at which the bundle is taken out of the queue fir sending to a node.
   */
  private long executionStartTime = 0L;
  /**
   * The build number of the current version of JPPF.
   */
  private int buildNumber = 0;
  /**
   * The state of this bundle, to indicate whether it is used for handshake with
   * the server or for transporting tasks to execute.
   */
  private State state = State.EXECUTION_BUNDLE;
  /**
   * Map holding the parameters of the request.
   */
  private final Map<Object, Object> parameters = new HashMap<Object, Object>();
  /**
   * The service level agreement between the job and the server.
   */
  private JobSLA jobSLA = new JPPFJobSLA();
  /**
   * The user-defined metadata associated with this job.
   */
  private JobMetadata jobMetadata = new JPPFJobMetadata();

  /**
   * Initialize this task bundle and set its build number.
   */
  public JPPFTaskBundle()
  {
    buildNumber = VersionUtils.getBuildNumber();
  }

  /**
   * Get the unique identifier for this task bundle.
   * @return the uuid as a string.
   */
  public String getBundleUuid()
  {
    return bundleUuid;
  }

  /**
   * Set the unique identifier for this task bundle.
   * @param uuid the uuid as a string.
   */
  public void setBundleUuid(final String uuid)
  {
    this.bundleUuid = uuid;
  }

  /**
   * Get the unique identifier for the request this task is a part of.
   * @return the request uuid as a string.
   */
  public String getRequestUuid()
  {
    return jobUuid;
  }

  /**
   * Set the unique identifier for the request this task is a part of.
   * @param requestUuid the request uuid as a string.
   */
  public void setRequestUuid(final String requestUuid)
  {
    this.jobUuid = requestUuid;
  }

  /**
   * Get shared data provider for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public byte[] getDataProvider()
  {
    return dataProvider;
  }

  /**
   * Set shared data provider for this task.
   * @param dataProvider a <code>DataProvider</code> instance.
   */
  public void setDataProvider(final byte[] dataProvider)
  {
    this.dataProvider = dataProvider;
  }

  /**
   * Get the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @return the uuid path as a list of string elements.
   */
  public TraversalList<String> getUuidPath()
  {
    return uuidPath;
  }

  /**
   * Set the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @param uuidPath the uuid path as a list of string elements.
   */
  public void setUuidPath(final TraversalList<String> uuidPath)
  {
    this.uuidPath = uuidPath;
  }

  /**
   * Get the time at which this wrapper was added to the queue.
   * @return the time in milliseconds as a long value.
   */
  public long getQueueEntryTime()
  {
    return queueEntryTime;
  }

  /**
   * Set the time at which this wrapper was added to the queue.
   * @param queueEntryTime the time in milliseconds as a long value.
   */
  public void setQueueEntryTime(final long queueEntryTime)
  {
    this.queueEntryTime = queueEntryTime;
  }

  /**
   * Get the time it took a node to execute this task.
   * @return the time in milliseconds as a long value.
   */
  public long getNodeExecutionTime()
  {
    return nodeExecutionTime;
  }

  /**
   * Set the time it took a node to execute this task.
   * @param nodeExecutionTime the time in milliseconds as a long value.
   */
  public void setNodeExecutionTime(final long nodeExecutionTime)
  {
    this.nodeExecutionTime = nodeExecutionTime;
  }

  /**
   * Get the tasks to be executed by the node.
   * @return the tasks as a <code>List</code> of arrays of bytes.
   */
  public List<byte[]> getTasks()
  {
    return tasks;
  }

  /**
   * Set the tasks to be executed by the node.
   * @param tasks the tasks as a <code>List</code> of arrays of bytes.
   */
  public void setTasks(final List<byte[]> tasks)
  {
    this.tasks = tasks;
  }

  /**
   * Get the number of tasks in this bundle.
   * @return the number of tasks as an int.
   */
  public int getTaskCount()
  {
    return taskCount;
  }

  /**
   * Set the number of tasks in this bundle.
   * @param taskCount the number of tasks as an int.
   */
  public void setTaskCount(final int taskCount)
  {
    this.taskCount = taskCount;
    if (initialTaskCount <= 0) initialTaskCount = taskCount;
  }

  /**
   * Get the task completion listener to notify, once the execution of this task has completed.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public TaskCompletionListener getCompletionListener()
  {
    return completionListener;
  }

  /**
   * Set the task completion listener to notify, once the execution of this task has completed.
   * @param listener a <code>TaskCompletionListener</code> instance.
   */
  public void setCompletionListener(final TaskCompletionListener listener)
  {
    this.completionListener = listener;
  }

  /**
   * Compare two task bundles, based on their respective priorities.<br>
   * <b>Note:</b> <i>this class has a natural ordering that is inconsistent with equals.</i>
   * @param bundle the bundle compare this one to.
   * @return a positive int if this bundle is greater, 0 if both are equal,
   * or a negative int if this bundle is less than the other.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final JPPFTaskBundle bundle)
  {
    if (bundle == null) return 1;
    int otherPriority = bundle.getSLA().getPriority();
    if (jobSLA.getPriority() < otherPriority) return -1;
    if (jobSLA.getPriority() > otherPriority) return 1;
    return 0;
  }

  /**
   * Get the build number under which this task bundle was created.
   * @return the build number as an int value.
   */
  public int getBuildNumber()
  {
    return buildNumber;
  }

  /**
   * Make a copy of this bundle.
   * @return a new <code>JPPFTaskBundle</code> instance.
   */
  public JPPFTaskBundle copy()
  {
    JPPFTaskBundle bundle = new JPPFTaskBundle();
    bundle.setBundleUuid(bundleUuid);
    bundle.setUuidPath(uuidPath);
    bundle.setRequestUuid(jobUuid);
    bundle.setUuid(jobUuid);
    bundle.setName(name);
    bundle.setTaskCount(taskCount);
    bundle.setDataProvider(dataProvider);
    synchronized(bundle.getParametersMap())
    {
      for (Map.Entry<Object, Object> entry: parameters.entrySet()) bundle.setParameter(entry.getKey(), entry.getValue());
    }
    bundle.setQueueEntryTime(queueEntryTime);
    bundle.setCompletionListener(completionListener);
    bundle.setSLA(jobSLA);
    //bundle.setParameter(BundleParameter.JOB_METADATA, getJobMetadata());

    return bundle;
  }

  /**
   * Make a copy of this bundle containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>JPPFTaskBundle</code> instance.
   */
  public JPPFTaskBundle copy(final int nbTasks)
  {
    JPPFTaskBundle bundle = copy();
    bundle.setTaskCount(nbTasks);
    taskCount -= nbTasks;
    return bundle;
  }

  /**
   * Get the state of this bundle.
   * @return a <code>State</code> type safe enumeration value.
   */
  public State getState()
  {
    return state;
  }

  /**
   * Set the state of this bundle.
   * @param state a <code>State</code> type safe enumeration value.
   */
  public void setState(final State state)
  {
    this.state = state;
  }

  /**
   * Get the time at which the bundle is taken out of the queue for sending to a node.
   * @return the time as a long value.
   */
  public long getExecutionStartTime()
  {
    return executionStartTime;
  }

  /**
   * Set the time at which the bundle is taken out of the queue for sending to a node.
   * @param executionStartTime the time as a long value.
   */
  public void setExecutionStartTime(final long executionStartTime)
  {
    this.executionStartTime = executionStartTime;
  }

  /**
   * Get the initial task count of this bundle.
   * @return the task count as an int.
   */
  public int getInitialTaskCount()
  {
    return initialTaskCount;
  }

  /**
   * Set a parameter of this request.
   * @param name the name of the parameter to set.
   * @param value the value of the parameter to set.
   */
  public void setParameter(final Object name, final Object value)
  {
    synchronized(parameters)
    {
      parameters.put(name, value);
    }
  }

  /**
   * Get the value of a parameter of this request.
   * @param name the name of the parameter to get.
   * @return the value of the parameter, or null if the parameter is not set.
   */
  public Object getParameter(final Object name)
  {
    //if (parameters == null) return null;
    return parameters.get(name);
  }

  /**
   * Get the value of a parameter of this request.
   * @param name the name of the parameter to get.
   * @param defaultValue the default value to return if the parameter is not set.
   * @return the value of the parameter, or <code>defaultValue</code> if the parameter is not set.
   */
  public Object getParameter(final Object name, final Object defaultValue)
  {
    Object res = parameters.get(name);
    return res == null ? defaultValue : res;
  }

  /**
   * Remove a parameter from this request.
   * @param name the name of the parameter to remove.
   * @return the value of the parameter to remove, or null if the parameter is not set.
   */
  public Object removeParameter(final Object name)
  {
    return parameters.remove(name);
  }

  /**
   * Get the map holding the parameters of the request.
   * @return a map of string keys to object values.
   */
  public Map<Object, Object> getParametersMap()
  {
    return parameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JobSLA getSLA()
  {
    return jobSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of <code>JPPFJobSLA</code>.
   */
  public void setSLA(final JobSLA jobSLA)
  {
    this.jobSLA = jobSLA;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder("[");
    sb.append("jobId=").append(getName());
    sb.append(", jobUuid=").append(getUuid());
    sb.append(", initialTaskCount=").append(initialTaskCount);
    sb.append(", taskCount=").append(taskCount);
    sb.append(", requeue=").append(parameters == null ? null : getParameter(BundleParameter.JOB_REQUEUE));
    sb.append(']');
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   * @deprecated use {@link #getName() getName()} instead.
   */
  @Override
  public String getId()
  {
    return getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName()
  {
    return name;
  }

  /**
   * Set the user-defined display name for the job.
   * @param name the display name as a string.
   */
  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JobMetadata getMetadata()
  {
    return jobMetadata;
  }

  /**
   * Set this bundle's metadata.
   * @param jobMetadata a {@link JPPFJobMetadata} instance.
   */
  public void setMetadata(final JobMetadata jobMetadata)
  {
    this.jobMetadata = jobMetadata;
  }

  /**
   * {@inheritDoc}
   * @deprecated use {@link #getUuid()} instead.
   */
  @Override
  public String getJobUuid()
  {
    return getUuid();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUuid()
  {
    return jobUuid;
  }

  /**
   * Set the uuid of the initial job.
   * @param jobUuid the uuid as a string.
   */
  public void setUuid(final String jobUuid)
  {
    this.jobUuid = jobUuid;
  }
}
