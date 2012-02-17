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

package org.jppf.server.nio.acceptor;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jppf.server.nio.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public class AcceptorContext extends SimpleNioContext<AcceptorState>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AcceptorContext.class);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Identifier for the channel.
   */
  private int id = JPPFIdentifiers.UNKNOWN;

  /**
   * Read data from a channel.
   * @param wrapper the channel to read the data from.
   * @return true if all the data has been read, false otherwise.
   * @throws Exception if an error occurs while reading the data.
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> wrapper) throws Exception
  {
    ReadableByteChannel channel = (ReadableByteChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
    if (message == null)
    {
      message = new NioMessage();
      message.length = 4;
      message.buffer = ByteBuffer.wrap(new byte[4]);
      readByteCount = 0;
    }
    readByteCount += channel.read(message.buffer);
    if (traceEnabled) log.trace("read " + readByteCount + " bytes out of " + message.length + " for " + wrapper);
    boolean b = readByteCount >= message.length;
    if (b) id = SerializationUtils.readInt(message.buffer.array(), 0);
    return b;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleException(final ChannelWrapper<?> channel)
  {
    AcceptorNioServer.closeChannel(channel);
  }

  /**
   * get the identifier for the channel.
   * @return the identifier as an int value.
   */
  public int getId()
  {
    return id;
  }
}
