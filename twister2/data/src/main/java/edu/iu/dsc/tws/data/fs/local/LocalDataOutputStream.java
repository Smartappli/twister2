//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package edu.iu.dsc.tws.data.fs.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import edu.iu.dsc.tws.api.data.FSDataOutputStream;

/**
 * The <code>LocalDataOutputStream</code> class is a wrapper class for a data
 * output stream to the local file system.
 */
public class LocalDataOutputStream extends FSDataOutputStream {

  /**
   * The file output stream used to write data.
   */
  private final FileOutputStream fos;

  /**
   * Constructs a new <code>LocalDataOutputStream</code> object from a given {@link File} object.
   *
   * @param file the {@link File} object the data stream is read from
   * @throws IOException thrown if the data output stream cannot be created
   */
  public LocalDataOutputStream(final File file) throws IOException {
    this.fos = new FileOutputStream(file);
  }

  @Override
  public void write(final int b) throws IOException {
    fos.write(b);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    fos.write(b, off, len);
  }

  @Override
  public void close() throws IOException {
    fos.close();
  }

  @Override
  public void write(ByteBuffer byteBuffer) throws IOException {
    fos.getChannel().write(byteBuffer);
  }

  @Override
  public void flush() throws IOException {
    fos.flush();
  }

  @Override
  public void sync() throws IOException {
    fos.getFD().sync();
  }

  @Override
  public long getPos() throws IOException {
    return fos.getChannel().position();
  }

  public FileChannel getChannel() {
    return fos.getChannel();
  }
}
