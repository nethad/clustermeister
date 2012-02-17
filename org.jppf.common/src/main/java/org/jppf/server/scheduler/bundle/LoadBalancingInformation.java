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

package org.jppf.server.scheduler.bundle;

import java.io.Serializable;
import java.util.List;

import org.jppf.utils.TypedProperties;

/**
 * Information on the load-balancing algorithm currently setup in the driver.
 * @author Laurent Cohen
 */
public class LoadBalancingInformation implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The name of the algorithm.
   */
  private String algorithm = null;
  /**
   * The algorithm's parameters.
   */
  private TypedProperties parameters = null;
  /**
   * The names of all available algorithms.
   */
  private List<String> algorithmNames = null;

  /**
   * Default constructor.
   */
  public LoadBalancingInformation()
  {
  }

  /**
   * Initialize this load balancing information with the specified algorithm and parameters.
   * @param algorithm the name of the algorithm.
   * @param parameters the algorithm's parameters.
   * @param algorithmNames the names of all available algorithms.
   */
  public LoadBalancingInformation(final String algorithm, final TypedProperties parameters, final List<String> algorithmNames)
  {
    this.algorithm = algorithm;
    this.parameters = parameters;
    this.algorithmNames = algorithmNames;
  }

  /**
   * Get the name of the algorithm.
   * @return the algorithm name as a string. 
   */
  public String getAlgorithm()
  {
    return algorithm;
  }

  /**
   * Get the algorithm's parameters.
   * @return the parameters as a set of (name, value) pairs.
   */
  public TypedProperties getParameters()
  {
    return parameters;
  }

  /**
   * The names of all available algorithms.
   * @return a list of all available alogrithm names.
   */
  public List<String> getAlgorithmNames()
  {
    return algorithmNames;
  }
}
