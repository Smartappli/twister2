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
package edu.iu.dsc.tws.common.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;

public class Client implements SelectHandler {
  private static final Logger LOG = Logger.getLogger(Client.class.getName());

  /**
   * The socket channel
   */
  private SocketChannel socketChannel;

  /**
   * Network address
   */
  private InetSocketAddress address;

  /**
   * Configuration
   */
  private Config config;

  /**
   * Selector
   */
  private Progress progress;

  /**
   * The channel to read and receive
   */
  private BaseNetworkChannel channel;

  /**
   * Weather we are connected
   */
  private boolean isConnected;

  /**
   * The channel callback
   */
  private ChannelHandler channelHandler;

  /**
   * Fixed buffers
   */
  private boolean fixedBuffers = true;

  public Client(String host, int port, Config cfg, Progress looper, ChannelHandler handler) {
    address = new InetSocketAddress(host, port);
    config = cfg;
    isConnected = false;
    progress = looper;
    channelHandler = handler;
  }

  public Client(String host, int port, Config cfg, Progress looper,
                ChannelHandler handler, boolean fixBuffers) {
    address = new InetSocketAddress(host, port);
    config = cfg;
    isConnected = false;
    progress = looper;
    channelHandler = handler;
    fixedBuffers = fixBuffers;
  }

  public boolean connect() {
    try {
      socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);
      socketChannel.socket().setTcpNoDelay(true);

      LOG.finest("Connecting to endpoint: " + address);
      if (socketChannel.connect(address)) {
        handleConnect(socketChannel);
      } else {
        progress.registerConnect(socketChannel, this);
      }
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Error connecting to remote endpoint: " + address, e);
      return false;
    }

    return true;
  }

  public TCPMessage send(SocketChannel sc, ByteBuffer buffer, int size, int edge) {
    if (sc != socketChannel) {
      return null;
    }

    if (!isConnected) {
      return null;
    }

    channel.enableWriting();
    TCPMessage request = new TCPMessage(buffer, edge, size);
    if (channel.addWriteRequest(request)) {
      return request;
    }
    return null;
  }

  public TCPMessage receive(SocketChannel sc, ByteBuffer buffer, int size, int edge) {
    if (sc != socketChannel) {
      return null;
    }

    if (!isConnected) {
      return null;
    }

    TCPMessage request = new TCPMessage(buffer, edge, size);
    channel.addReadRequest(request);

    return request;
  }

  public void disconnect() {
    if (!isConnected) {
      return;
    }

    channel.forceFlush();

    try {
      socketChannel.close();
      // we call the onclose with null value
      channelHandler.onClose(null);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to stop Client", e);
    }
  }

  @Override
  public void handleRead(SelectableChannel ch) {
    channel.read();
  }

  @Override
  public void handleWrite(SelectableChannel ch) {
    channel.write();
  }

  @Override
  public void handleAccept(SelectableChannel ch) {
    throw new RuntimeException("Client cannot accept connections");
  }

  @Override
  public void handleConnect(SelectableChannel selectableChannel) {
    try {
      if (socketChannel.finishConnect()) {
        progress.unregisterConnect(selectableChannel);
      }
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to FinishConnect to endpoint: " + address, e);
      channelHandler.onConnect(socketChannel, StatusCode.ERROR_CONN);
      return;
    }
    if (fixedBuffers) {
      channel = new FixedBufferChannel(config, progress, this, socketChannel, channelHandler);
    } else {
      channel = new DynamicBufferChannel(config, progress, this, socketChannel, channelHandler);
    }
    channel.enableReading();
    channel.enableWriting();

    isConnected = true;
    channelHandler.onConnect(socketChannel, StatusCode.SUCCESS);
  }

  @Override
  public void handleError(SelectableChannel ch) {
    channel.clear();
    progress.removeAllInterest(ch);

    try {
      ch.close();
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to close connection in handleError", e);
    }

    isConnected = false;
  }

  SocketChannel getSocketChannel() {
    return socketChannel;
  }
}
