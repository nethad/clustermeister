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

package org.jppf.management;

import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.*;

import javax.management.remote.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXMPServer extends AbstractJMXServer
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXMPServer.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this JMX server.
   */
  public JMXMPServer()
  {
    this(new JPPFUuid().toString());
  }

  /**
   * Initialize this JMX server with the specified uuid.
   * @param id the unique id of the driver or node holding this jmx server.
   */
  public JMXMPServer(final String id)
  {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start(final ClassLoader cl) throws Exception
  {
    if (debugEnabled) log.debug("starting remote connector server");
    ClassLoader tmp = Thread.currentThread().getContextClassLoader();
    lock.lock();
    try
    {
      Thread.currentThread().setContextClassLoader(cl);
      server = ManagementFactory.getPlatformMBeanServer();
      TypedProperties props = JPPFConfiguration.getProperties();
      managementHost = NetworkUtils.getManagementHost();
      managementPort = props.getInt("jppf.management.port", 11198);
      boolean found = false;
      JMXServiceURL url = null;
      while (!found)
      {
        try
        {
          InetAddress addr = InetAddress.getByName(managementHost);
          url = new JMXServiceURL("service:jmx:jmxmp://" + managementHost + ':' + managementPort);
          Map<String, Object> env = new HashMap<String, Object>();
          env.put("jmx.remote.default.class.loader", cl);
          env.put("jmx.remote.protocol.provider.class.loader", cl);
          connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, server);
          connectorServer.start();
          found = true;
        }
        catch(Exception e)
        {
          String s = e.getMessage();
          if ((e instanceof BindException) || ((s != null) && (s.toLowerCase().contains("bind"))))
          {
            if (managementPort >= 65530) managementPort = 1024;
            managementPort++;
          }
          else throw e;
        }
      }
      //props.setProperty("jppf.management.port", Integer.toString(port));
      //if (debugEnabled) log.debug("starting connector server with port = " + port);
      stopped = false;
      if (debugEnabled) log.debug("JMXConnectorServer started at URL " + url);
    }
    finally
    {
      lock.unlock();
      Thread.currentThread().setContextClassLoader(tmp);
    }
  }
}
