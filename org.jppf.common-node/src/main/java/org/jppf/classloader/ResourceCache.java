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

package org.jppf.classloader;

import java.io.*;
import java.net.*;
import java.security.AccessController;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class are used as cache for resources downloaded from a driver or client, using the JPPF class loader APIs.
 * @author Laurent Cohen
 */
class ResourceCache
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ResourceCache.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Name of the resource cache root.
   */
  private static String ROOT_NAME = ".jppf";
  /**
   * Map of resource names to temporary file names to which their content is stored.
   */
  private Map<String, List<String>> cache = new Hashtable<String, List<String>>();
  /**
   * List of temp folders used by this cache.
   */
  private List<String> tempFolders = new LinkedList<String>();

  /**
   * Default initializations.
   */
  ResourceCache()
  {
    Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
    initTempFolders();
  }

  /**
   * Get the list of locations for the resource with the specified name.
   * @param name the name of the resource to lookup.
   * @return a list of file paths, or null if the resource is not found in the cache.
   */
  public synchronized List<String> getResourcesLocations(final String name)
  {
    return cache.get(name);
  }

  /**
   * Get a location for the resource with the specified name.
   * @param name the name of the resource to lookup.
   * @return a file path, or null if the resource is not found in the cache.
   */
  public synchronized String getResourceLocation(final String name)
  {
    List<String> locations = cache.get(name);
    if ((locations == null) || locations.isEmpty()) return null;
    return locations.get(0);
  }

  /**
   * Set the list of locations for the resource with the specified name.
   * @param name the name of the resource to lookup.
   * @param locations a list of file paths.
   */
  public synchronized void setResourcesLocations(final String name, final List<String> locations)
  {
    cache.put(name, locations);
  }

  /**
   * Save the definitions for a resource to temporary files, and register their location with this cache.
   * @param name the name of the resource to register.
   * @param definitions a list of byte array definitions.
   * @throws Exception if any I/O error occurs.
   */
  public synchronized void registerResources(final String name, final List<byte[]> definitions) throws Exception
  {
    if (isAbsolutePath(name)) return;
    List<String> locations = new LinkedList<String>();
    //for (byte[] def: definitions) locations.add(saveToTempFile(def));
    for (byte[] def: definitions) locations.add(saveToTempFile(name, def));
    if (!locations.isEmpty()) setResourcesLocations(name, locations);
  }

  /**
   * Save the specified resource definition to a temporary file.
   * @param name the original name of the resource to save.
   * @param definition the definition to save, specified as a byte array.
   * @return the path to the created file.
   * @throws Exception if any I/O error occurs.
   */
  private String saveToTempFile(final String name, final byte[] definition) throws Exception
  {
    SaveFileAction action = new SaveFileAction(tempFolders, name, definition);
    File file = AccessController.doPrivileged(action);
    if (action.getException() != null) throw action.getException();
    if (traceEnabled) log.trace("saved resource [" + name + "] to file " + file);
    return file.getCanonicalPath();
  }

  /**
   * Get the URL for a cached resource.
   * @param name the name of the resource to find.
   * @return resource location expressed as a URL.
   */
  public URL getResourceURL(final String name)
  {
    String path = getResourceLocation(name);
    if (path == null) return null;
    return getURLFromPath(path);
  }

  /**
   * Transform a file path into a URL.
   * @param path the path to transform.
   * @return the path expressed as a URL.
   */
  public URL getURLFromPath(final String path)
  {
    File file = new File(path);
    try
    {
      return file.toURI().toURL();
    }
    catch (MalformedURLException ignore)
    {
    }
    return null;
  }

  /**
   * Initializations of the temps folders.
   */
  private void initTempFolders()
  {
    try
    {
      String base = JPPFConfiguration.getProperties().getString("jppf.resource.cache.dir", null);
      if (base == null)
      {
        base = System.getProperty("java.io.tmpdir");
        if (base == null) base = System.getProperty("user.home");
        if (base == null) base = System.getProperty("user.dir");
        if (base != null)
        {
          if (!base.endsWith(File.separator)) base += File.separator;
          base += ROOT_NAME;
        }
      }
      if (base == null) base = "." + File.separator + ROOT_NAME;
      if (traceEnabled) log.trace("base = " + base);
      int n = findFolderIndex(base);
      String s = base + File.separator + n;
      File baseDir = new File(s + File.separator);
      FileUtils.mkdirs(baseDir);
      tempFolders.add(s);
      if (traceEnabled) log.trace("added temp folder " + s);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Find an index that doesn't exist for the folder suffix.
   * @param folder the folder to which the new folder will belong.
   * @return the maximum existing index + 1, or 0 if no such folder already exists.
   * @throws Exception if any error occurs.
   */
  private int findFolderIndex(final String folder) throws Exception
  {
    File dir = new File(folder);
    FileUtils.mkdirs(dir);
    File[] subdirs = dir.listFiles(new FileFilter()
    {
      @Override
      public boolean accept(final File path)
      {
        if (traceEnabled) log.trace("checking '" + path.getPath() + '\'');
        return path.isDirectory();
      }
    });
    int max = -1;
    if(subdirs != null && subdirs.length > 0)
    {
      for (File f: subdirs)
      {
        try
        {
          int n = Integer.valueOf(f.getName());
          if (n > max) max = n;
        }
        catch(Exception e)
        {
        }
      }
    }
    if (traceEnabled) log.trace("max index = " + max);
    return max + 1;
  }

  /**
   * Determine whether the specified path is absolute, in a system-independent way.
   * @param path the path to verify.
   * @return true if the path is absolute, false otherwise
   */
  private boolean isAbsolutePath(final String path)
  {
    if (path.startsWith("/") || path.startsWith("\\")) return true;
    if (path.length() < 3) return false;
    char c = path.charAt(0);
    if ((((c >= 'A') && (c <='Z')) || ((c >= 'a') && (c <= 'z'))) && (path.charAt(1) == ':')) return true;
    return false;
  }

  /**
   * A runnable invoked whenever this resource cache is garbage collected or the JVM shuts down,
   * so as to cleanup all cached resources on the file system.
   */
  private class ShutdownHook implements Runnable
  {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
      while (!tempFolders.isEmpty()) FileUtils.deletePath(new File(tempFolders.remove(0)));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void finalize() throws Throwable
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        String[] paths = tempFolders.toArray(StringUtils.ZERO_STRING);
        tempFolders.clear();
        for (String path: paths) FileUtils.deletePath(new File(tempFolders.remove(0)));
      }
    };
    new Thread(r).start();
    super.finalize();
  }
}
