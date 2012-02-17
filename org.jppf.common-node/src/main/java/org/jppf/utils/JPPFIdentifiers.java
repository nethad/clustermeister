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

package org.jppf.utils;

import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public final class JPPFIdentifiers
{
  /**
   * Identifier for an unidentified channel.
   */
  public static final int UNKNOWN = 0;
  /**
   * Identifier for the job data channel of a client.
   */
  public static final int CLIENT_JOB_DATA_CHANNEL = 0xFFFA;
  /**
   * Identifier for the class loader channel of a client.
   */
  public static final int CLIENT_CLASSLOADER_CHANNEL = 0xFFFB;
  /**
   * Identifier for the job data channel of a node.
   */
  public static final int NODE_JOB_DATA_CHANNEL = 0xFFFC;
  /**
   * Identifier for the class loader channel of a node.
   */
  public static final int NODE_CLASSLOADER_CHANNEL = 0xFFFD;
  /**
   * Mapping of ids to readable names.
   */
  private static Map<Integer, String> idMap = new HashMap<Integer, String>();

  static
  {
    idMap.put(CLIENT_JOB_DATA_CHANNEL, "CLIENT_JOB_DATA_CHANNEL");
    idMap.put(CLIENT_CLASSLOADER_CHANNEL, "CLIENT_CLASSLOADER_CHANNEL");
    idMap.put(NODE_JOB_DATA_CHANNEL, "NODE_JOB_DATA_CHANNEL");
    idMap.put(NODE_CLASSLOADER_CHANNEL, "NODE_CLASSLOADER_CHANNEL");
  }

  /**
   * Get an identifier as a string.
   * @param id the id to lookup.
   * @return a readable string for the id.
   */
  public static String asString(final int id)
  {
    String s = idMap.get(id);
    return s == null ? "UNKNOWN" : s;
  }
}
