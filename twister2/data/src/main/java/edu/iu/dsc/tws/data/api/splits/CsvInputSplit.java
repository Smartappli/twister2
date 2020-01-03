////  Licensed under the Apache License, Version 2.0 (the "License");
////  you may not use this file except in compliance with the License.
////  You may obtain a copy of the License at
////
////  http://www.apache.org/licenses/LICENSE-2.0
////
////  Unless required by applicable law or agreed to in writing, software
////  distributed under the License is distributed on an "AS IS" BASIS,
////  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
////  See the License for the specific language governing permissions and
////  limitations under the License.
package edu.iu.dsc.tws.data.api.splits;
//
//import java.io.IOException;
//import java.nio.ByteOrder;
//import java.nio.charset.Charset;
//import java.util.Arrays;
//import java.util.logging.Logger;
//
//import edu.iu.dsc.tws.api.config.Config;
//import edu.iu.dsc.tws.api.data.Path;
//import edu.iu.dsc.tws.data.api.formatters.FileInputPartitioner;
//
////public class CsvInputSplit extends FileInputSplit<byte[]> {
//public class CsvInputSplit extends
//
//  private static final Logger LOG = Logger.getLogger(CsvInputSplit.class.getName());
//*
//   * The configuration key to set the binary record length.
//
//
//  protected static final String RECORD_LENGTH = "binary-format.record-length";
//
//*
//   * The default value of the record length
//
//
//  protected static final int DEFAULT_RECORD_LENGTH = 8;
//
//*
//   * Endianess of the binary file, or the byte order
//
//
//  //private ByteOrder endianess = ByteOrder.LITTLE_ENDIAN;
//  private ByteOrder endianess = ByteOrder.BIG_ENDIAN;
//
//*
//   * The default read buffer size = 1MB.
//
//
//  private static final int DEFAULT_READ_BUFFER_SIZE = 1024 * 1024;
//
//  private int bufferSize = -1;
//
//*
//   * The length of a single record in the given binary file.
//
//
//  protected transient int recordLength;
//
//  // --------------------------------------------------------------------------------------------
//  //  Variables for internal parsing.
//  //  They are all transient, because we do not want them so be serialized
//  // --------------------------------------------------------------------------------------------
//
//  private transient byte[] readBuffer;
//
//  private transient byte[] wrapBuffer;
//
//  private transient int readPos;
//
//  private transient int limit;
//
//  private transient byte[] currBuffer;    // buffer in which current record byte sequence is found
//  private transient int currOffset;      // offset in above buffer
//  private transient int currLen;        // length of current byte sequence
//
//  private transient boolean overLimit;
//
//  private transient boolean end;
//
//  private long offset = -1;
//
//  private Config config;
//
//  private byte[] delimiter = new byte[]{'\n'};
//
//*
//   * Code of \r, used to remove \r from a line when the line ends with \r\n.
//
//
//  private static final byte CARRIAGE_RETURN = (byte) '\r';
//
//*
//   * Code of \n, used to identify if \n is used as delimiter.
//
//
//  private static final byte NEW_LINE = (byte) '\n';
//
//  private String delimiterString = null;
//
//  protected static final String RECORD_DELIMITER = "delimited-format.delimiter";
//
//*
//   * Constructs a split with host information.
//   *
//   * @param num the number of this input split
//   * @param file the file name
//   * @param start the position of the first byte in the file to process
//   * @param length the number of bytes in the file to process (-1 is flag for "read whole file")
//   * @param hosts the list of hosts containing the block, possibly <code>null</code>
//
//
//  public CsvInputSplit(int num, Path file, long start, long length, String[] hosts) {
//    super(num, file, start, length, hosts);
//  }
//
//  public ByteOrder getEndianess() {
//    return endianess;
//  }
//
//  public void setEndianess(ByteOrder order) {
//    if (endianess == null) {
//      throw new IllegalArgumentException("Endianess must not be null");
//    }
//    this.endianess = order;
//  }
//
//  public int getBufferSize() {
//    return bufferSize;
//  }
//
//  public void setBufferSize(int buffSize) {
//    if (buffSize < 2) {
//      throw new IllegalArgumentException("Buffer size must be at least 2.");
//    }
//    this.bufferSize = buffSize;
//  }
//
//  public int getRecordLength() {
//    return recordLength;
//  }
//
//  public void setRecordLength(int recordLen) {
//    if (recordLen <= 0) {
//      throw new IllegalArgumentException("RecordLength must be larger than 0");
//    }
//    this.recordLength = recordLen;
//    if (this.bufferSize % recordLen != 0) {
//      int bufferFactor = 1;
//      if (this.bufferSize > 0) {
//        bufferFactor = bufferSize / recordLen;
//      } else {
//        bufferFactor = DEFAULT_READ_BUFFER_SIZE / recordLen;
//      }
//      if (bufferFactor >= 1) {
//        setBufferSize(recordLen * bufferFactor);
//      } else {
//        setBufferSize(recordLen * 8);
//      }
//    }
//  }
//
//
//  public byte[] getDelimiter() {
//    return delimiter;
//  }
//
//*
//   * Configures this input format by reading the path to the file from the configuration and
//   setting
//   * the record length
//   *
//   * @param parameters The configuration object to read the parameters from.
//
//
//  @Override
//  public void configure(Config parameters) {
//    super.configure(parameters);
//    this.config = parameters;
//    if (Arrays.equals(delimiter, new byte[]{'\n'})) {
//      String delimString = parameters.getStringValue(RECORD_DELIMITER, null);
//      if (delimString != null) {
//        setDelimiterString(delimString);
//      }
//    }
//  }
//
//
//
//  public String getDelimiterString() {
//    return delimiterString;
//  }
//
//  public void setDelimiterString(String delimiterString) {
//    if (delimiter == null) {
//      throw new IllegalArgumentException("Delimiter must not be null");
//    }
//    this.delimiter = delimiterString.getBytes(getCharset());
//    this.delimiterString = delimiterString;
//  }
//
//  // The charset used to convert strings to bytes
//  private String charsetName = "UTF-8";
//
//  // Charset is not serializable
//  private transient Charset charset;
//
//  public Charset getCharset() {
//    if (this.charset == null) {
//      this.charset = Charset.forName(charsetName);
//    }
//    return this.charset;
//  }
//
//*
//   * Opens the given input split. This method opens the input stream to the specified file,
//   * allocates read buffers
//   * and positions the stream at the correct position, making sure that any partial record at
//   * the beginning is skipped.
//
//
//  public void open() throws IOException {
//    super.open();
//    initBuffers();
//    this.offset = splitStart;
//    if (this.splitStart != 0) {
//      this.stream.seek(offset);
//      if (this.overLimit) {
//        this.end = true;
//      }
//    } else {
//      fillBuffer(0);
//    }
//  }
//
//  public void open(Config cfg) throws IOException {
//    super.open(cfg);
//    initBuffers();
//    this.offset = splitStart;
//    if (this.splitStart != 0) {
//      this.stream.seek(offset);
//      if (this.overLimit) {
//        this.end = true;
//      }
//    } else {
//      fillBuffer(0);
//    }
//  }
//
//
//  @Override
//  public boolean reachedEnd() {
//    return this.end;
//  }
//
////  public byte[] nextRecord(byte[] record) throws IOException {
////    if (readLine()) {
////      readRecord(record, this.readBuffer, this.currOffset, this.currLen);
////    } else {
////      this.end = true;
////      return null;
////    }
////  }
//
//  private int lineLengthLimit = Integer.MAX_VALUE;
//
//  private void setResult(byte[] buffer, int resultOffset, int len) {
//    this.currBuffer = buffer;
//    this.currOffset = resultOffset;
//    this.currLen = len;
//  }
//
//
//  @Override
//  public byte[] nextRecord(byte[] record) throws IOException {
//    //if (checkAndBufferRecord()) {
//    if (readLine()) {
//      return readRecord(record, this.readBuffer, this.currOffset, this.currLen);
//    } else {
//      this.end = true;
//      return null;
//    }
//  }
//
//  public byte[] readRecord(byte[] reusable, byte[] bytes, int readOffset, int numBytes)
//      throws IOException {
//    if (reusable != null) {
//      System.arraycopy(bytes, readOffset, reusable, 0, numBytes);
//      return reusable;
//    } else {
//      byte[] tmp = new byte[this.recordLength];
//      System.arraycopy(bytes, readOffset, tmp, 0, numBytes);
//      return tmp;
//    }
//  }
//
//  private boolean checkAndBufferRecord() throws IOException {
//    if (this.readPos < this.limit) {
//      //The next record is already in the buffer
//      this.currOffset = this.readPos;
//      this.currLen = this.recordLength;
//      this.readPos = this.readPos + this.recordLength;
//      return true;
//    } else {
//      //Need to refill the buffer
//      if (fillBuffer(0)) {
//        this.currOffset = this.readPos;
//        this.currLen = this.recordLength;
//        this.readPos = this.readPos + this.recordLength;
//        return true;
//      } else {
//        return false;
//      }
//    }
//  }
//
//  private void initBuffers() {
//    this.bufferSize = this.bufferSize <= 0 ? DEFAULT_READ_BUFFER_SIZE : this.bufferSize;
//
//    if (this.bufferSize <= this.delimiter.length) {
//      throw new IllegalArgumentException("Buffer size must be greater than length of delimiter.");
//    }
//
//    if (this.readBuffer == null || this.readBuffer.length != this.bufferSize) {
//      this.readBuffer = new byte[this.bufferSize];
//    }
//    if (this.wrapBuffer == null || this.wrapBuffer.length < 256) {
//      this.wrapBuffer = new byte[256];
//    }
//
//    this.readPos = 0;
//    this.limit = 0;
//    this.overLimit = false;
//    this.end = false;
//  }
//
//*
//   * Fills the read buffer with bytes read from the file starting from an offset.
//
//
//  private boolean fillBuffer(int fillOffset) throws IOException {
//    int maxReadLength = this.readBuffer.length - fillOffset;
//    if (this.splitLength == FileInputPartitioner.READ_WHOLE_SPLIT_FLAG) {
//      int read = this.stream.read(this.readBuffer, fillOffset, maxReadLength);
//      if (read == -1) {
//        this.stream.close();
//        this.stream = null;
//        return false;
//      } else {
//        this.readPos = fillOffset;
//        this.limit = read;
//        return true;
//      }
//    }
//
//    int toRead;
//    if (this.splitLength > 0) {
//      // if we have more data, read that
//      toRead = this.splitLength > maxReadLength ? maxReadLength : (int) this.splitLength;
//    } else {
//      toRead = maxReadLength;
//      this.overLimit = true;
//    }
//
//    int read = this.stream.read(this.readBuffer, fillOffset, toRead);
//    if (read == -1) {
//      this.stream.close();
//      this.stream = null;
//      return false;
//    } else {
//      this.splitLength -= read;
//      this.readPos = fillOffset; // position from where to start reading
//      this.limit = read + fillOffset; // number of valid bytes in the read buffer
//      return true;
//    }
//  }
//
//
//  protected final boolean readLine() throws IOException {
//    if (this.stream == null || this.overLimit) {
//      return false;
//    }
//
//    int countInWrapBuffer = 0;
//    int delimPos = 0;
//
//    while (true) {
//      if (this.readPos >= this.limit) {
//        if (!fillBuffer(delimPos)) {
//          int countInReadBuffer = delimPos;
//          if (countInWrapBuffer + countInReadBuffer > 0) {
//            if (countInReadBuffer > 0) {
//              if (this.wrapBuffer.length - countInWrapBuffer < countInReadBuffer) {
//                byte[] tmp = new byte[countInWrapBuffer + countInReadBuffer];
//                System.arraycopy(this.wrapBuffer, 0, tmp, 0, countInWrapBuffer);
//                this.wrapBuffer = tmp;
//              }
//              System.arraycopy(this.readBuffer, 0, this.wrapBuffer,
//                  countInWrapBuffer, countInReadBuffer);
//              countInWrapBuffer += countInReadBuffer;
//            }
//
//            this.offset += countInWrapBuffer;
//            setResult(this.wrapBuffer, 0, countInWrapBuffer);
//            return true;
//          } else {
//            return true;
//          }
//        }
//      }
//
//      int startPos = this.readPos - delimPos;
//      int count;
//      // Search for next occurence of delimiter in read buffer.
//      while (this.readPos < this.limit && delimPos < this.delimiter.length) {
//        if ((this.readBuffer[this.readPos]) == this.delimiter[delimPos]) {
//          delimPos++;
//        } else {
//          readPos -= delimPos;
//          delimPos = 0;
//        }
//        readPos++;
//      }
//      if (readPos == limit) {
//        this.end = true;
//      }
//
//      if (delimPos == this.delimiter.length) {
//        // we found a delimiter
//        int readBufferBytesRead = this.readPos - startPos;
//        this.offset += countInWrapBuffer + readBufferBytesRead;
//        count = readBufferBytesRead - this.delimiter.length;
//
//        if (countInWrapBuffer > 0) {
//          if (this.wrapBuffer.length < countInWrapBuffer + count) {
//            final byte[] nb = new byte[countInWrapBuffer + count];
//            System.arraycopy(this.wrapBuffer, 0, nb, 0, countInWrapBuffer);
//            this.wrapBuffer = nb;
//          }
//          if (count >= 0) {
//            System.arraycopy(this.readBuffer, 0, this.wrapBuffer, countInWrapBuffer, count);
//          }
//          setResult(this.wrapBuffer, 0, countInWrapBuffer + count);
//          return true;
//        } else {
//          setResult(this.readBuffer, startPos, count);
//          return true;
//        }
//      } else {
//        count = this.limit - startPos;
//        if (((long) countInWrapBuffer) + count > this.lineLengthLimit) {
//          throw new IOException("The record length exceeded the maximum record length ("
//              + this.lineLengthLimit + ").");
//        }
//
//        int bytesToMove = count - delimPos;
//        if (this.wrapBuffer.length - countInWrapBuffer < bytesToMove) {
//          byte[] tmp = new byte[Math.max(this.wrapBuffer.length * 2,
//              countInWrapBuffer + bytesToMove)];
//          System.arraycopy(this.wrapBuffer, 0, tmp, 0, countInWrapBuffer);
//          this.wrapBuffer = tmp;
//        }
//
//        System.arraycopy(this.readBuffer, startPos, this.wrapBuffer,
//            countInWrapBuffer, bytesToMove);
//        countInWrapBuffer += bytesToMove;
//        System.arraycopy(this.readBuffer, this.readPos - delimPos, this.readBuffer, 0, delimPos);
//      }
//    }
//  }
//}
