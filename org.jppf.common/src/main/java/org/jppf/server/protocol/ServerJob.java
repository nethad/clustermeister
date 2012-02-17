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

import java.util.List;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * 
 * @author Laurent Cohen
 */
public interface ServerJob
{
  /**
   * Get the underlying task bundle.
   * @return a <code>JPPFTaskBundle</code> instance.
   */
  JPPFDistributedJob getJob();

  /**
   * Get the location of the data provider.
   * @return a <code>JPPFTaskBundle</code> instance.
   */
  DataLocation getDataProvider();

  /**
   * Get the list of locations of the tasks.
   * @return a list of <code>DataLocation</code> instances.
   */
  List<DataLocation> getTasks();
}
