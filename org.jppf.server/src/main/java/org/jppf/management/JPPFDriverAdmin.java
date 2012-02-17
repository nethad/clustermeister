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

import java.util.*;

import org.jppf.server.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate the administration functionalities for a JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFDriverAdmin implements JPPFDriverAdminMBean
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFDriverAdmin.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Base name used for localization lookups.
   */
  private static final String I18N_BASE = "org.jppf.server.i18n.server_messages";
  /**
   * Reference to the driver.
   */
  private transient JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * The latest load-balancing information set via JMX.
   */
  private LoadBalancingInformation currentLoadBalancingInformation;
  /**
   * Synchronization lock.
   */
  private Object loadBalancingInformationLock = new Object();

  /**
   * Request the JMX connection information for all the nodes attached to the server.
   * @return a collection of <code>NodeManagementInfo</code> instances.
   * @see org.jppf.management.JPPFDriverAdminMBean#nodesInformation()
   */
  @Override
  public Collection<JPPFManagementInfo> nodesInformation()
  {
    try
    {
      List<JPPFManagementInfo> list = new LinkedList<JPPFManagementInfo>(driver.getNodeHandler().getNodeInformationMap().values());
      return list;
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Get the latest statistics snapshot from the JPPF driver.
   * @return a <code>JPPFStats</code> instance.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFDriverAdminMBean#statistics()
   */
  @Override
  public JPPFStats statistics() throws Exception
  {
    try
    {
      return driver.getStatsUpdater().getStats();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Change the bundle size tuning settings.
   * @param algorithm the name of the load-balancing algorithm to set.
   * @param parameters the algorithm's parameters.
   * @return an acknowledgement or error message.
   * @throws Exception if an error occurred while updating the settings.
   * @see org.jppf.management.JPPFDriverAdminMBean#changeLoadBalancerSettings(java.lang.String, java.util.Map)
   */
  @Override
  public String changeLoadBalancerSettings(final String algorithm, final Map<Object, Object> parameters) throws Exception
  {
    try
    {
      if (algorithm == null) return "Error: no algorithm specified (null value)";
      JPPFBundlerFactory factory = driver.getNodeNioServer().getBundlerFactory();
      if (!factory.getBundlerProviderNames().contains(algorithm)) return "Error: unknown algorithm '" + algorithm + '\'';
      TypedProperties props = new TypedProperties(parameters);
      synchronized(loadBalancingInformationLock)
      {
        currentLoadBalancingInformation = new LoadBalancingInformation(algorithm, props, currentLoadBalancingInformation.getAlgorithmNames());
      }
      Bundler bundler = factory.createBundler(algorithm, props);
      driver.getNodeNioServer().setBundler(bundler);
      //return new JPPFManagementResponse(localize((manual ? "manual" : "automatic") + ".settings.changed"), null);
      return localize("load.balancing.updated");
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return "Error : " + e.getMessage();
    }
  }

  /**
   * Perform a shutdown or restart of the server.
   * @param shutdownDelay the delay before shutting down the server, once the command is received.
   * @param restartDelay the delay before restarting, once the server is shutdown. If it is < 0, no restart occurs.
   * @return an acknowledgement message.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFDriverAdminMBean#restartShutdown(java.lang.Long, java.lang.Long)
   */
  @Override
  public String restartShutdown(final Long shutdownDelay, final Long restartDelay) throws Exception
  {
    try
    {
      if (debugEnabled) log.debug("request to restart/shutdown this driver, shutdownDelay=" + shutdownDelay + ", restartDelay=" + restartDelay);
      boolean restart = restartDelay >= 0;
      driver.initiateShutdownRestart(shutdownDelay, restart, restartDelay);
      return localize("request.acknowledged");
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return "Error : " + e.getMessage();
    }
  }

  /**
   * Obtain the current load-balancing settings.
   * @return an instance of <code>LoadBalancingInformation</code>.
   * @throws Exception if any error occurs.
   * @see org.jppf.management.JPPFDriverAdminMBean#loadBalancerInformation()
   */
  @Override
  public LoadBalancingInformation loadBalancerInformation() throws Exception
  {
    synchronized(loadBalancingInformationLock)
    {
      if (currentLoadBalancingInformation == null) currentLoadBalancingInformation = computeCurrentLoadBalancingInformation();
      return currentLoadBalancingInformation;
    }
  }
 
  /**
   * Compute the current load balancing parameters form the JPPF configuration.
   * @return a {@link LoadBalancingInformation} instance.
   */
  private LoadBalancingInformation computeCurrentLoadBalancingInformation()
  {
    TypedProperties props = JPPFConfiguration.getProperties();
    String algorithm = props.getString("jppf.load.balancing.algorithm", null);
    // for compatibility with v1.x configuration files
    if (algorithm == null) algorithm = props.getString("task.bundle.strategy", "manual");
    String profileName = props.getString("jppf.load.balancing.strategy", null);
    // for compatibility with v1.x configuration files
    if (profileName == null) profileName = props.getString("task.bundle.autotuned.strategy", "jppf");
    JPPFBundlerFactory factory = driver.getNodeNioServer().getBundlerFactory();
    TypedProperties params = factory.convertJPPFConfiguration(profileName, props);
    List<String> algorithmsList = null;
    try
    {
      algorithmsList = factory.getBundlerProviderNames();
    }
    catch (Exception e)
    {
      algorithmsList = new ArrayList<String>();
      log.error(e.getMessage(), e);
    }
    return new LoadBalancingInformation(algorithm, params, algorithmsList);
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  private static String localize(final String message)
  {
    return LocalizationUtils.getLocalized(I18N_BASE, message);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetStatistics() throws Exception
  {
    driver.getStatsManager().reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFSystemInformation systemInformation() throws Exception
  {
    JPPFSystemInformation info = new JPPFSystemInformation(driver.getUuid());
    info.populate();
    return info;
  }
}
