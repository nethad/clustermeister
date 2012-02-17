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

package org.jppf.server.nio.acceptor;

import org.jppf.server.*;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.NioState;

/**
 * Common abstract superclass for all states of a client that sends and receives jobs.
 * @author Laurent Cohen
 */
abstract class AcceptorServerState extends NioState<AcceptorTransition>
{
  /**
   * The server that handles this state.
   */
  protected AcceptorNioServer server = null;
  /**
   * The driver stats manager.
   */
  protected JPPFDriverStatsManager statsManager = null;
  /**
   * The job manager.
   */
  protected JPPFJobManager jobManager = null;
  /**
   * Reference to the driver.
   */
  protected JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public AcceptorServerState(final AcceptorNioServer server)
  {
    this.server = server;
    statsManager = driver.getStatsManager();
    jobManager = driver.getJobManager();
  }
}
