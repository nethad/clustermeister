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
package org.jppf.server.scheduler.bundle.providers;

import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.impl.NodeThreadsLoadBalancer;
import org.jppf.server.scheduler.bundle.nodethreads.NodeThreadsProfile;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerProvider;
import org.jppf.utils.TypedProperties;

/**
 * Provider implementation for the "nodethreads" algorithm.
 * @author Laurent Cohen
 */
public class NodeThreadsLoadBalancerProvider implements JPPFBundlerProvider
{

  /**
   * Create a bundler instance using the specified parameters profile.
   * @param profile no used in this implementation.
   * @return an instance of {@link NodeThreadsLoadBalancer}.
   * @see org.jppf.server.scheduler.bundle.spi.JPPFBundlerProvider#createBundler(org.jppf.server.scheduler.bundle.LoadBalancingProfile)
   */
  @Override
  public Bundler createBundler(final LoadBalancingProfile profile)
  {
    return new NodeThreadsLoadBalancer(profile);
  }

  /**
   * Create a bundler profile containing the parameters of the algorithm.
   * This method returns null, as the algorithm does not use any parameter.
   * @param configuration a set of properties defining the algorithm parameters.
   * @return an instance of {@link NodeThreadsProfile}.
   * @see org.jppf.server.scheduler.bundle.spi.JPPFBundlerProvider#createProfile(org.jppf.utils.TypedProperties)
   */
  @Override
  public LoadBalancingProfile createProfile(final TypedProperties configuration)
  {
    return new NodeThreadsProfile(configuration);
  }

  /**
   * Get the name of the algorithm defined by this provider.
   * @return the algorithm name as a string.
   * @see org.jppf.server.scheduler.bundle.spi.JPPFBundlerProvider#getAlgorithmName()
   */
  @Override
  public String getAlgorithmName()
  {
    return "nodethreads";
  }
}
