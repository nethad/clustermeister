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

package org.jppf.client.submission;

import org.jppf.client.*;


/**
 * 
 * @author Laurent Cohen
 */
public interface JobSubmission extends Runnable
{
  /**
   * {@inheritDoc}
   */
  @Override
  void run();

  /**
   * Get the unique id of this submission.
   * @return the id as a string.
   */
  String getId();

  /**
   * Get the connection to execute the job on.
   * @return a {@link AbstractJPPFClientConnection} instance.
   */
  AbstractJPPFClientConnection getConnection();

  /**
   * Get the flag indicating whether the job will be executed locally, at least partially.
   * @return <code>true</code> if the job will execute locally, <code>false</code> otherwise.
   */
  boolean isLocallyExecuting();

  /**
   * Get the job this submission is for.
   * @return a {@link JPPFJob} instance.
   */
  JPPFJob getJob();
}
