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
package org.jppf.io;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Implementation of an input stream backed by a <code>ByteBuffer</code>.
 * @author Laurent Cohen
 */
public class ByteBufferInputStream extends InputStream
{
  /**
   * The underlying byte buffer for this input stream.
   */
  private ByteBuffer buffer = null;

  /**
   * Initialize this stream to read from the specified byte buffer.
   * @param buffer the buffer to read from.
   */
  public ByteBufferInputStream(final ByteBuffer buffer)
  {
    this(buffer, false);
  }

  /**
   * Initialize this stream to read from the specified byte buffer.
   * @param buffer the buffer to read from.
   * @param flip if true, then the buffer is flipped.
   */
  public ByteBufferInputStream(final ByteBuffer buffer, final boolean flip)
  {
    this.buffer = buffer;
    if (flip) buffer.flip();
  }

  /**
   * Read one byte from
   * @return the next byte of data, or <code>-1</code> if the end of the stream is reached.
   * @throws IOException if an I/O error occurs.
   * @see java.io.InputStream#read()
   */
  @Override
  public int read() throws IOException
  {
    if (!buffer.hasRemaining()) return -1;
    return buffer.get();
  }

  /**
   * Get the number of bytes that can still be read from this stream without blocking.
   * @return the number of bytes that can be read from this input stream without blocking.
   * @throws IOException if an I/O error occurs.
   * @see java.io.InputStream#available()
   */
  @Override
  public int available() throws IOException
  {
    return buffer.remaining();
  }

  /**
   * Close this input stream.
   * @throws IOException if an I/O error occurs.
   * @see java.io.InputStream#close()
   */
  @Override
  public void close() throws IOException
  {
    super.close();
  }

  /**
   * Marks the current position in this input stream.
   * @param readlimit the maximum limit of bytes that can be read before the mark position becomes invalid.
   * @see java.io.InputStream#mark(int)
   */
  @Override
  public synchronized void mark(final int readlimit)
  {
    super.mark(readlimit);
  }

  /**
   * Tests if this input stream supports the <code>mark</code> and <code>reset</code> methods.
   * @return <code>true</code> if this stream instance supports the mark and reset methods, <code>false</code> otherwise.
   * @see java.io.InputStream#markSupported()
   */
  @Override
  public boolean markSupported()
  {
    return super.markSupported();
  }

  /**
   * 
   * @param b the buffer into which the data is read.
   * @param off the start offset in array <code>b</code> at which the data is written.
   * @param len the maximum number of bytes to read.
   * @return the total number of bytes read into the buffer, or <code>-1</code> if the end of the stream has been reached.
   * @exception IOException if an I/O error occurs.
   * @see java.io.InputStream#read(byte[], int, int)
   */
  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException
  {
    if (b == null) throw new NullPointerException();
    else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
      throw new IndexOutOfBoundsException();
    else if (len == 0) return 0;
    else if (buffer.remaining() <= 0) return -1;
    int length = (len > buffer.remaining()) ? buffer.remaining() : len;
    buffer.get(b, off, length);
    return length;
  }

  /**
   * 
   * @param b the buffer into which the data is read.
   * @return the total number of bytes read into the buffer, or <code>-1</code> if the end of the stream has been reached.
   * @exception IOException if an I/O error occurs.
   * @see java.io.InputStream#read(byte[])
   */
  @Override
  public int read(final byte[] b) throws IOException
  {
    if (b == null) throw new NullPointerException();
    return read(b, 0, b.length);
  }

  /**
   * Repositions this stream to the position at the time the <code>mark</code> method was last called on this input stream.
   * @throws IOException if this stream has not been marked or if the mark has been invalidated.
   * @see java.io.InputStream#reset()
   */
  @Override
  public synchronized void reset() throws IOException
  {
    super.reset();
  }

  /**
   * Skips over and discards <code>n</code> bytes of data from this input stream.
   * @param n the number of bytes to be skipped.
   * @return the actual number of bytes skipped.
   * @throws IOException if an I/O error occurs.
   * @see java.io.InputStream#skip(long)
   */
  @Override
  public long skip(final long n) throws IOException
  {
    if (n < 0) return 0;
    int pos = buffer.position();
    int newPos = (pos + n > buffer.limit()) ? buffer.limit() : pos + (int) n;
    buffer.position(newPos);
    return newPos - pos;
  }
}
