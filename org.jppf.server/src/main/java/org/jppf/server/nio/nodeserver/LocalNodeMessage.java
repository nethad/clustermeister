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

package org.jppf.server.nio.nodeserver;

import java.io.InputStream;

import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * Node message implementation for an in-VM node.
 * @author Laurent Cohen
 */
public class LocalNodeMessage extends AbstractNodeMessage
{
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean read(final ChannelWrapper<?> wrapper) throws Exception
  {
    InputStream is = locations.get(0).getInputStream();
    byte[] data = null;
    try
    {
      data = StreamUtils.getInputStreamAsByte(is);
    }
    finally
    {
      StreamUtils.close(is);
    }
    data = JPPFDataTransformFactory.transform(false, data, 0, data.length);
    SerializationHelper helper = new SerializationHelperImpl();
    bundle = (JPPFTaskBundle) helper.getSerializer().deserialize(data);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected synchronized boolean readNextObject(final ChannelWrapper<?> wrapper) throws Exception
  {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean write(final ChannelWrapper<?> wrapper) throws Exception
  {
    //((LocalNodeWrapperHandler) wrapper).wakeUp();
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean writeNextObject(final ChannelWrapper<?> wrapper) throws Exception
  {
    return true;
  }
}
