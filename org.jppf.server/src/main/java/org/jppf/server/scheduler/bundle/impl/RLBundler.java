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

package org.jppf.server.scheduler.bundle.impl;

import org.jppf.server.JPPFDriver;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.rl.AbstractRLBundler;
import org.slf4j.*;

/**
 * Bundler based on a reinforcement learning algorithm.
 * @author Laurent Cohen
 */
public class RLBundler extends AbstractRLBundler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RLBundler.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Creates a new instance with the specified parameters.
   * @param profile the parameters of the algorithm, grouped as a performance analysis profile.
   */
  public RLBundler(final LoadBalancingProfile profile)
  {
    super(profile);
  }

  /**
   * Make a copy of this bundler
   * @return a <code>Bundler</code> instance.
   * @see org.jppf.server.scheduler.bundle.Bundler#copy()
   */
  @Override
  public Bundler copy()
  {
    return new RLBundler(profile);
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
   */
  @Override
  protected int maxSize()
  {
    return JPPFDriver.getQueue().getMaxBundleSize();
  }
}
