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

import org.jppf.io.DataLocation;
import org.jppf.serialization.JPPFObjectStreamFactory;
import org.jppf.utils.streams.JPPFByteArrayOutputStream;

/**
 * Instances of this class are used to serialize or deserialize objects to or from an array of bytes.<br>
 * A specific use of this class is that it can be loaded by a new classloader, making the execution transparent
 * to any change in the client code.
 * @author Laurent Cohen
 */
public class ObjectSerializerImpl implements ObjectSerializer
{
  /**
   * The default constructor must be public to allow for instantiation through Java reflection.
   */
  public ObjectSerializerImpl()
  {
  }

  /**
   * Serialize an object into an array of bytes.
   * @param o the object to Serialize.
   * @return a <code>JPPFBuffer</code> instance holding the serialized object.
   * @throws Exception if the object can't be serialized.
   * @see org.jppf.utils.ObjectSerializer#serialize(java.lang.Object)
   */
  @Override
  public JPPFBuffer serialize(final Object o) throws Exception
  {
    return serialize(o, false);
  }

  /**
   * Serialize an object into an array of bytes.
   * @param o the object to Serialize.
   * @param noCopy avoid copying intermediate buffers.
   * @return a <code>JPPFBuffer</code> instance holding the serialized object.
   * @throws Exception if the object can't be serialized.
   * @see org.jppf.utils.ObjectSerializer#serialize(java.lang.Object)
   */
  @Override
  public JPPFBuffer serialize(final Object o, final boolean noCopy) throws Exception
  {
    JPPFByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
    serialize(o, baos);
    byte[] data = noCopy ? baos.getBuf() : baos.toByteArray();
    return new JPPFBuffer(data, baos.size());
  }

  /**
   * Serialize an object into an output stream.
   * @param o the object to Serialize.
   * @param os the output stream to serialize to.
   * @throws Exception if the object can't be serialized.
   * @see org.jppf.utils.ObjectSerializer#serialize(java.lang.Object, java.io.OutputStream)
   */
  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception
  {
    ObjectOutputStream oos = JPPFObjectStreamFactory.newObjectOutputStream(os);
    oos.writeObject(o);
    oos.flush();
    oos.close();
  }

  /**
   * Serialize an object into an output stream.
   * @param o the object to Serialize.
   * @param location the location to serialize to.
   * @throws Exception if the object can't be serialized.
   */
  public static void serialize(final Object o, final DataLocation location) throws Exception
  {
    ObjectOutputStream oos = JPPFObjectStreamFactory.newObjectOutputStream(location.getOutputStream());
    try
    {
      oos.writeObject(o);
      oos.flush();
    }
    finally
    {
      oos.close();
    }
  }

  /**
   * Read an object from an array of bytes.
   * @param buf buffer holding the array of bytes to deserialize from.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   * @see org.jppf.utils.ObjectSerializer#deserialize(org.jppf.utils.JPPFBuffer)
   */
  @Override
  public Object deserialize(final JPPFBuffer buf) throws Exception
  {
    return deserialize(new ByteArrayInputStream(buf.getBuffer(), 0, buf.getLength()));
  }

  /**
   * Read an object from an array of bytes.
   * @param bytes buffer holding the array of bytes to deserialize from.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   * @see org.jppf.utils.ObjectSerializer#deserialize(byte[])
   */
  @Override
  public Object deserialize(final byte[] bytes) throws Exception
  {
    return deserialize(new ByteArrayInputStream(bytes));
  }

  /**
   * Read an object from an array of bytes.
   * @param bytes buffer holding the array of bytes to deserialize from.
   * @param offset position at which to start reading the bytes from.
   * @param length the number of bytes to read.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   * @see org.jppf.utils.ObjectSerializer#deserialize(byte[], int, int)
   */
  @Override
  public Object deserialize(final byte[] bytes, final int offset, final int length) throws Exception
  {
    return deserialize(new ByteArrayInputStream(bytes, offset, length));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object deserialize(final InputStream is) throws Exception
  {
    return deserialize(is, true);
  }

  /**
   * Read an object from an input stream.
   * @param is the input stream to deserialize from.
   * @param closeStream <code>true</code> to close the stream, <code>false</code> otherwise.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   */
  public Object deserialize(final InputStream is, final boolean closeStream) throws Exception
  {
    Object o = null;
    ObjectInputStream ois = JPPFObjectStreamFactory.newObjectInputStream(is);
    o = ois.readObject();
    if (closeStream) ois.close();
    return o;
  }
}
