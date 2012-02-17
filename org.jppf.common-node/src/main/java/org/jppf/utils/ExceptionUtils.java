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

import java.io.*;

/**
 * This class provides a set of utility methods for manipulating {@link Throwable} objects.
 * @author Laurent Cohen
 */
public final class ExceptionUtils
{
  /**
   * Instantiation of this class is not permitted.
   */
  private ExceptionUtils()
  {
  }

  /**
   * Get a throwable's stack trace.
   * @param t the throwable to get the stack trace from.
   * @return the stack trace as a string.
   */
  public static String getStackTrace(final Throwable t)
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.close();
    return sw.toString();
  }

  /**
   * Get the message of the specified <code>Throwable</code> along with its class name.
   * @param t the <code>Throwable</code> object from which to get the message.
   * @return a formatted message from the <code>Throwable</code>.
   */
  public static String getMessage(final Throwable t)
  {
    return t.getClass().getName() + ": " + t.getMessage();
  }
}
