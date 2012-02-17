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

package org.jppf.client;

import java.util.*;

/**
 * Instances of this class manage a list of client connections with the same priority.
 */
class ClientPool
{
  /**
   * The priority associated with this pool.
   */
  private final int priority;
  /**
   * Index of the last used connection in this pool.
   */
  private int lastUsedIndex = 0;
  /**
   * List of <code>JPPFClientConnection</code> instances with the same priority.
   */
  private final List<JPPFClientConnection> clientList = new ArrayList<JPPFClientConnection>();

  /**
   * Initialize this pool with the specified priority.
   * @param priority the priority assigned to the connections n the pool.
   */
  public ClientPool(final int priority) {
    this.priority = priority;
  }

  /**
   * Get the next client connection.
   * @return a <code>JPPFClientConnection</code> instances.
   */
  public JPPFClientConnection nextClient()
  {
    if (clientList.isEmpty()) return null;
    lastUsedIndex = ++lastUsedIndex % clientList.size();
    return clientList.get(getLastUsedIndex());
  }

  /**
   * Determine whether this pool is empty.
   * @return <code>true</code> if this pool is empty, <code>false</code> otherwise.
   */
  public boolean isEmpty() {
    return clientList.isEmpty();
  }

  /**
   * Get the current size of this pool.
   * @return the size as an int.
   */
  public int size()
  {
    return clientList.size();
  }

  /**
   * Add a driver connection to this pool.
   * @param client the connection too add.
   * @return true if the underlying list of connections changed as a result of calling this method.
   */
  public boolean add(final JPPFClientConnection client) {
    return clientList.add(client);
  }

  /**
   * Remove a driver connection from this pool.
   * @param client the connection too remove.
   * @return true if the underlying list of connections changed as a result of calling this method.
   */
  public boolean remove(final JPPFClientConnection client) {
    if(clientList.remove(client)) {
      if(lastUsedIndex >= clientList.size() && lastUsedIndex > 0) lastUsedIndex--;
      return true;
    } else
      return false;
  }

  /**
   * Get the priority associated with this pool.
   * @return the priority as an int.
   */
  public int getPriority()
  {
    return priority;
  }

  /**
   * Get the index of the last used connection in this pool.
   * @return the last used index as an int.
   */
  public int getLastUsedIndex()
  {
    return lastUsedIndex;
  }
}
