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
package edu.iu.dsc.tws.comms.dfw;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.api.MessageHeader;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.TWSChannel;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.io.MessageDeSerializer;
import edu.iu.dsc.tws.comms.dfw.io.MessageSerializer;

public class ChannelDataFlowOperation implements ChannelListener, ChannelMessageReleaseCallback {
  private static final Logger LOG = Logger.getLogger(ChannelDataFlowOperation.class.getName());

  /**
   * The default path to be used
   */
  private static final int DEFAULT_PATH = -1;

  // the configuration
  private Config config;
  // the task plan
  private TaskPlan instancePlan;

  /**
   * The edge used
   */
  private int edge;
  /**
   * The network channel
   */
  private TWSChannel channel;
  /**
   * Set of de-serializers
   */
  private Map<Integer, MessageDeSerializer> messageDeSerializer;

  /**
   * Set of serializers
   */
  private Map<Integer, MessageSerializer> messageSerializer;

  // we may have multiple routes throughus
  private MessageType dataType;

  /**
   * The key type
   */
  private MessageType keyType = MessageType.BYTE;
  /**
   * Receive data type
   */
  private MessageType receiveDataType;
  /**
   * Receive key type
   */
  private MessageType receiveKeyType;
  /**
   * Weather keys are involved
   */
  private boolean isKeyed = false;
  /**
   * Lock for serializing the operations
   */
  private Lock lock = new ReentrantLock();

  /**
   * Executor id
   */
  private int executor;
  /**
   * The send sendBuffers used by the operation
   */
  private Queue<DataBuffer> sendBuffers;

  /**
   * Receive availableBuffers, for each receive we need to make
   */
  private Map<Integer, Queue<DataBuffer>> receiveBuffers;

  /**
   * Local buffers that are used when receive buffers need to be freed. Buffer are only added
   * to the list when needed
   */
  private Queue<DataBuffer> localReceiveBuffers;

  /**
   * Pending send messages
   */
  private Map<Integer, ArrayBlockingQueue<Pair<Object, OutMessage>>>
      pendingSendMessagesPerSource;

  /**
   * Pending receives in case the receives are not ready
   */
  private Map<Integer, Queue<Pair<Object, InMessage>>> pendingReceiveMessagesPerSource;

  /**
   * Pending deserialization
   */
  private Map<Integer, Queue<InMessage>> pendingReceiveDeSerializations;
  /**
   * Non grouped current messages
   */
  private Map<Integer, InMessage> currentMessages = new HashMap<>();

  /**
   * These are the workers from which we receive messages
   */
  private Set<Integer> receivingExecutors;

  /**
   * The message receiver for MPI messages
   */
  private ChannelReceiver receiver;

  /**
   * Send communicationProgress tracker
   */
  private ProgressionTracker sendProgressTracker;

  /**
   * Receive communicationProgress tracker
   */
  private ProgressionTracker receiveProgressTracker;

  /**
   * Deserialize communicationProgress track
   */
  private ProgressionTracker deserializeProgressTracker;

  private AtomicInteger externalSendsPending = new AtomicInteger(0);

  public ChannelDataFlowOperation(TWSChannel channel) {
    this.channel = channel;
  }


  /**
   * init method
   */
  public void init(Config cfg, MessageType messageType, MessageType rcvDataType,
                   MessageType kType, MessageType rcvKeyType, TaskPlan plan,
                   int graphEdge, Set<Integer> recvExecutors,
                   ChannelReceiver msgReceiver,
                   Map<Integer, ArrayBlockingQueue<Pair<Object, OutMessage>>>
                       pendingSendPerSource,
                   Map<Integer, Queue<Pair<Object, InMessage>>> pRMPS,
                   Map<Integer, Queue<InMessage>> pendingReceiveDesrialize,
                   Map<Integer, MessageSerializer> serializer,
                   Map<Integer, MessageDeSerializer> deSerializer, boolean keyed) {
    this.config = cfg;
    this.instancePlan = plan;
    this.edge = graphEdge;
    this.dataType = messageType;
    this.receiveDataType = rcvDataType;
    this.receiveKeyType = rcvKeyType;
    this.keyType = kType;
    this.executor = instancePlan.getThisExecutor();
    this.receivingExecutors = recvExecutors;
    this.receiver = msgReceiver;
    this.isKeyed = keyed;

    this.pendingReceiveMessagesPerSource = pRMPS;
    this.pendingSendMessagesPerSource = pendingSendPerSource;
    this.pendingReceiveDeSerializations = pendingReceiveDesrialize;

    this.messageSerializer = serializer;
    this.messageDeSerializer = deSerializer;

    int noOfSendBuffers = DataFlowContext.sendBuffersCount(config);
    int sendBufferSize = DataFlowContext.bufferSize(config);

    this.sendBuffers = new ArrayBlockingQueue<>(noOfSendBuffers);
    for (int i = 0; i < noOfSendBuffers; i++) {
      sendBuffers.offer(new DataBuffer(channel.createBuffer(sendBufferSize)));
    }
    this.receiveBuffers = new HashMap<>();
    this.localReceiveBuffers = new ArrayDeque<>();

    LOG.log(Level.FINE, String.format("%d setup communication", instancePlan.getThisExecutor()));
    // now setup the sends and receives
    setupCommunication();

    // initialize the serializers
    LOG.fine(String.format("%d setup initializers", instancePlan.getThisExecutor()));
    initSerializers();

    initProgressTrackers();
  }

  /**
   * init method
   */
  public void init(Config cfg, MessageType messageType, TaskPlan plan,
                   int graphEdge, Set<Integer> recvExecutors,
                   ChannelReceiver msgReceiver,
                   Map<Integer, ArrayBlockingQueue<Pair<Object, OutMessage>>>
                       pendingSendPerSource,
                   Map<Integer, Queue<Pair<Object, InMessage>>> pRMPS,
                   Map<Integer, Queue<InMessage>> pendingReceiveDesrialize,
                   Map<Integer, MessageSerializer> serializer,
                   Map<Integer, MessageDeSerializer> deSerializer, boolean keyed) {
    init(cfg, messageType, messageType, keyType, keyType,
        plan, graphEdge, recvExecutors, msgReceiver,
        pendingSendPerSource, pRMPS, pendingReceiveDesrialize, serializer, deSerializer, keyed);
  }

  private void initSerializers() {
    // initialize the serializers
    for (MessageSerializer serializer : messageSerializer.values()) {
      serializer.init(config, sendBuffers, isKeyed);
    }
    for (MessageDeSerializer deSerializer : messageDeSerializer.values()) {
      deSerializer.init(config, isKeyed);
    }
  }

  private void initProgressTrackers() {
    Set<Integer> sendItems = pendingSendMessagesPerSource.keySet();
    sendProgressTracker = new ProgressionTracker(sendItems);

    Set<Integer> receiveItems = pendingReceiveMessagesPerSource.keySet();
    receiveProgressTracker = new ProgressionTracker(receiveItems);

    Set<Integer> desrializeItems = pendingReceiveDeSerializations.keySet();
    deserializeProgressTracker = new ProgressionTracker(desrializeItems);
  }

  /**
   * Setup the receives and send sendBuffers
   */
  private void setupCommunication() {
    // we will receive from these
    int maxReceiveBuffers = DataFlowContext.receiveBufferCount(config);
    int receiveBufferSize = DataFlowContext.bufferSize(config);
    for (Integer recv : receivingExecutors) {
      Queue<DataBuffer> recvList = new LinkedBlockingQueue<>();
      for (int i = 0; i < maxReceiveBuffers; i++) {
        recvList.add(new DataBuffer(channel.createBuffer(receiveBufferSize)));
      }
      // register with the channel
      LOG.fine(instancePlan.getThisExecutor() + " Register to receive from: " + recv);
      channel.receiveMessage(recv, edge, this, recvList);
      receiveBuffers.put(recv, recvList);
    }

    // configure the send sendBuffers
    int sendBufferSize = DataFlowContext.bufferSize(config);
    int sendBufferCount = DataFlowContext.sendBuffersCount(config);
    for (int i = 0; i < sendBufferCount; i++) {
      DataBuffer buffer = new DataBuffer(channel.createBuffer(sendBufferSize));
      sendBuffers.offer(buffer);
    }
  }

  /**
   * Sends a message from a partil location
   *
   * @param source source id
   * @param message the actual message
   * @param target an specific target
   * @param flags message flags
   * @param routingParameters routing parameter
   * @return true if the message is accepted
   */
  public boolean sendMessagePartial(int source, Object message, int target,
                                    int flags, RoutingParameters routingParameters) {
    // for partial sends we use minus value to find the correct queue
    ArrayBlockingQueue<Pair<Object, OutMessage>> pendingSendMessages =
        pendingSendMessagesPerSource.get(source * -1 - 1);
    return offerForSend(source, message, target, flags,
        routingParameters, pendingSendMessages);
  }

  /**
   * Sends a message from a originating location
   *
   * @param source source id
   * @param message the actual message
   * @param target an specific target
   * @param flags message flags
   * @param routingParameters routing parameter
   * @return true if the message is accepted
   */
  public boolean sendMessage(int source, Object message, int target,
                             int flags, RoutingParameters routingParameters) {
    ArrayBlockingQueue<Pair<Object, OutMessage>> pendingSendMessages =
        pendingSendMessagesPerSource.get(source);
    if (pendingSendMessages == null) {
      throw new RuntimeException(String.format("%d No send messages %d", executor, source));
    }
    return offerForSend(source, message, target, flags,
        routingParameters, pendingSendMessages);
  }

  @Override
  public void onReceiveComplete(int id, int e, DataBuffer buffer, boolean releaseBuffer) {
    // we need to try to build the message here, we may need many more messages to complete
    ByteBuffer byteBuffer = buffer.getByteBuffer();
    byteBuffer.position(buffer.getSize());
    byteBuffer.flip();

    InMessage currentMessage = currentMessages.get(id);
    if (currentMessage == null) {
      MessageHeader header = messageDeSerializer.get(id).buildHeader(buffer, e);

      currentMessage = new InMessage(id, receiveDataType, this, header);
      if (isKeyed) {
        currentMessage.setKeyType(receiveKeyType);
      }
      currentMessages.put(id, currentMessage);

      // we add the message immediately to the deserialization as we can deserialize partially
      Queue<InMessage> deserializeQueue = pendingReceiveDeSerializations.get(id);
      if (!deserializeQueue.offer(currentMessage)) {
        throw new RuntimeException(executor + " We should have enough space: "
            + deserializeQueue.size());
      }
    }

    if (currentMessage.addBufferAndCalculate(buffer)) {
      currentMessages.remove(id);
    }

    // we need to free the buffer because we don't have space
    if (releaseBuffer) {
      freeReceiveBuffers(id);
    }
  }

  /**
   * Weather we have more data to complete
   */
  public boolean isComplete() {
    for (Map.Entry<Integer, Queue<Pair<Object, InMessage>>> e
        : pendingReceiveMessagesPerSource.entrySet()) {
      if (e.getValue().size() > 0) {
        return false;
      }
    }

    for (Map.Entry<Integer, ArrayBlockingQueue<Pair<Object, OutMessage>>> e
        : pendingSendMessagesPerSource.entrySet()) {
      if (e.getValue().size() > 0) {
        return false;
      }
    }

    for (Map.Entry<Integer, Queue<InMessage>> e : pendingReceiveDeSerializations.entrySet()) {
      if (e.getValue().size() > 0) {
        return false;
      }
    }

    return externalSendsPending.get() == 0;
  }

  /**
   * Progress the serializations and receives, this method must be called by threads to
   * send messages through this communication
   */
  public void progress() {
    if (sendProgressTracker.canProgress()) {
      int sendId = sendProgressTracker.next();
      if (sendId != Integer.MIN_VALUE) {
        sendProgress(pendingSendMessagesPerSource.get(sendId), sendId);
        sendProgressTracker.finish(sendId);
      }
    }

    if (deserializeProgressTracker.canProgress()) {
      int deserializeId = deserializeProgressTracker.next();
      if (deserializeId != Integer.MIN_VALUE) {
        receiveDeserializeProgress(
            pendingReceiveDeSerializations.get(deserializeId), deserializeId);
        deserializeProgressTracker.finish(deserializeId);
      }
    }

    if (receiveProgressTracker.canProgress()) {
      int receiveId = receiveProgressTracker.next();
      if (receiveId != Integer.MIN_VALUE) {
        receiveProgress(pendingReceiveMessagesPerSource.get(receiveId));
        receiveProgressTracker.finish(receiveId);
      }
    }
  }

  /**
   * Put the message into internal queues, to be serialized and then send to the network channel
   *
   * @param source source
   * @param message data
   * @param target target
   * @param flags flags
   * @param routingParameters routing parameters
   * @param pendingSendMessages the message queue
   * @return true if message is accepted
   */
  private boolean offerForSend(int source, Object message, int target, int flags,
                               RoutingParameters routingParameters,
                               ArrayBlockingQueue<Pair<Object, OutMessage>> pendingSendMessages) {
    if (pendingSendMessages.remainingCapacity() > 0) {
      int path = DEFAULT_PATH;
      if (routingParameters.getExternalRoutes().size() > 0) {
        path = routingParameters.getDestinationId();
      }

      OutMessage sendMessage = new OutMessage(source, edge,
          path, target, flags, routingParameters.getInternalRoutes(),
          routingParameters.getExternalRoutes(), dataType, keyType, this);

      // now try to put this into pending
      return pendingSendMessages.offer(
          new ImmutablePair<>(message, sendMessage));
    }
    return false;
  }

  /**
   * Go through the out messages, create channel messages by using the serializer send them
   *
   * @param pendingSendMessages the pending message queue
   * @param sendId send target
   */
  private void sendProgress(Queue<Pair<Object, OutMessage>> pendingSendMessages, int sendId) {
    boolean canProgress = true;

    while (pendingSendMessages.size() > 0 && canProgress) {
      // take out pending messages
      Pair<Object, OutMessage> pair = pendingSendMessages.peek();
      OutMessage outMessage = pair.getValue();
      Object data = pair.getKey();

      // first lets send the message to internal destinations
      canProgress = sendInternally(outMessage, data);

      if (canProgress) {
        // we don't have an external executor to send this message
        if (outMessage.getExternalSends().size() == 0) {
          pendingSendMessages.poll();
          continue;
        }
        Queue<ChannelMessage> channelMessages = outMessage.getChannelMessages();
        // at this point lets build the message
        ChannelMessage serializeMessage = (ChannelMessage)
            messageSerializer.get(sendId).build(pair.getKey(), outMessage);
        if (serializeMessage != null) {
          channelMessages.offer(serializeMessage);
        }

        ChannelMessage chMessage = channelMessages.peek();
        if (chMessage == null) {
          continue;
        }

        List<Integer> externalRoutes = new ArrayList<>(outMessage.getExternalSends());
        // okay we build the message, send it
        if (outMessage.serializedState() == OutMessage.SendState.SERIALIZED) {
          int startOfExternalRouts = chMessage.getAcceptedExternalSends();
          canProgress = sendExternally(outMessage, chMessage, externalRoutes, startOfExternalRouts);
          if (startOfExternalRouts == externalRoutes.size()) {
            // we are done
            pendingSendMessages.poll();
            channelMessages.poll();
          }
        } else if (outMessage.serializedState() == OutMessage.SendState.PARTIALLY_SERIALIZED) {
          List<Integer> exRoutes = new ArrayList<>(outMessage.getExternalSends());
          int startOfExternalRouts = chMessage.getAcceptedExternalSends();

          canProgress = sendExternally(outMessage, chMessage, exRoutes, startOfExternalRouts);
          if (startOfExternalRouts == externalRoutes.size()) {
            // we are done sending this channel message
            channelMessages.poll();
          }
        } else {
          break;
        }
      }
    }
  }

  private boolean sendExternally(OutMessage outMessage, ChannelMessage chMessage,
                                 List<Integer> exRoutes, int startOfExternalRouts) {
    boolean canProgress = true;
    lock.lock();
    try {
      if (!chMessage.isOutCountUpdated()) {
        chMessage.incrementRefCount(outMessage.getExternalSends().size());
        chMessage.setOutCountUpdated(true);
      }
      for (int i = startOfExternalRouts; i < exRoutes.size(); i++) {
        boolean sendAccepted = sendMessageToTarget(chMessage, exRoutes.get(i));
        // if no longer accepts stop
        if (!sendAccepted) {
          canProgress = false;
          break;
        } else {
          //remove the buffers from the original message
          chMessage.removeAllBuffers();
          externalSendsPending.incrementAndGet();
        }
      }
    } finally {
      lock.unlock();
    }
    return canProgress;
  }

  private boolean sendInternally(OutMessage outMessage, Object messageObject) {
    boolean canProgress = true;
    if (outMessage.serializedState() == OutMessage.SendState.INIT) {
      // send it internally
      int startOfInternalRouts = outMessage.getAcceptedInternalSends();
      List<Integer> inRoutes = new ArrayList<>(outMessage.getInternalSends());
      for (int i = startOfInternalRouts; i < outMessage.getInternalSends().size(); i++) {
        boolean receiveAccepted;
        lock.lock();
        try {
          receiveAccepted = receiver.receiveSendInternally(
              outMessage.getSource(), inRoutes.get(i), outMessage.getTarget(),
              outMessage.getFlags(), messageObject);
        } finally {
          lock.unlock();
        }
        if (!receiveAccepted) {
          canProgress = false;
          break;
        }
        outMessage.incrementAcceptedInternalSends();
      }
      if (canProgress) {
        outMessage.setSendState(OutMessage.SendState.SENT_INTERNALLY);
      }
    }
    return canProgress;
  }

  private void receiveDeserializeProgress(Queue<InMessage> msgQueue, int receiveId) {
    InMessage currentMessage = msgQueue.peek();
    if (currentMessage == null) {
      return;
    }

    int id = currentMessage.getOriginatingId();
    MessageHeader header = currentMessage.getHeader();
    Object object = DataFlowContext.EMPTY_OBJECT;
    if ((header.getFlags() & MessageFlags.END) != MessageFlags.END) {
      object = messageDeSerializer.get(receiveId).build(currentMessage,
          currentMessage.getHeader().getEdge());
    } else if ((header.getFlags() & MessageFlags.BARRIER) == MessageFlags.BARRIER) {
      object = messageDeSerializer.get(receiveId).build(currentMessage,
          currentMessage.getHeader().getEdge());
    }
    Queue<Pair<Object, InMessage>> pendingReceiveMessages =
        pendingReceiveMessagesPerSource.get(id);
    if (!pendingReceiveMessages.offer(new ImmutablePair<>(object, currentMessage))) {
      throw new RuntimeException(executor + " We should have enough space: "
          + pendingReceiveMessages.size());
    }

    // we remove only when the unpacking is complete and ready to receive
    if (currentMessage.getReceivedState() == InMessage.ReceivedState.RECEIVE) {
      msgQueue.poll();
    }
  }


  private void receiveProgress(Queue<Pair<Object, InMessage>> pendingReceiveMessages) {
    while (pendingReceiveMessages.size() > 0) {
      Pair<Object, InMessage> pair = pendingReceiveMessages.peek();
      InMessage.ReceivedState state = pair.getRight().getReceivedState();
      InMessage currentMessage = pair.getRight();

      lock.lock();
      try {
        if (state == InMessage.ReceivedState.BUILDING
            || state == InMessage.ReceivedState.BUILT) {
          // get the first channel message
          ChannelMessage msg = currentMessage.getBuiltMessages().peek();
          if (msg != null) {
            if (!receiver.handleReceivedChannelMessage(msg)) {
              break;
            }
            currentMessage.getBuiltMessages().poll();
          }

          if (state == InMessage.ReceivedState.BUILT
              && currentMessage.getBuiltMessages().size() == 0) {
            currentMessage.setReceivedState(InMessage.ReceivedState.RECEIVE);
          }
        }

        if (state == InMessage.ReceivedState.RECEIVE) {
          Object object = currentMessage.getDeserializedData();
          if (!receiver.receiveMessage(currentMessage.getHeader(), object)) {
            break;
          }

          currentMessage.setReceivedState(InMessage.ReceivedState.DONE);
          pendingReceiveMessages.poll();
        }
      } finally {
        lock.unlock();
      }
    }
  }

  private boolean sendMessageToTarget(ChannelMessage channelMessage, int i) {
    int e = instancePlan.getExecutorForChannel(i);
    return channel.sendMessage(e, channelMessage, this);
  }

  @Override
  public void release(ChannelMessage message) {
    if (message.doneProcessing()) {
      int originatingId = message.getOriginatingId();
      releaseTheBuffers(originatingId, message);
    }
  }

  @Override
  public void onSendComplete(int id, int messageStream, ChannelMessage message) {
    // ok we don't have anything else to do
    message.release();
    externalSendsPending.getAndDecrement();
  }

  private void freeReceiveBuffers(int id) {
    InMessage currentMessage = currentMessages.get(id);
    if (currentMessage == null) {
      return;
    }
    Queue<DataBuffer> buffers = currentMessage.getBuffers();
    DataBuffer channelMessage = buffers.peek();
    if (channelMessage == null) {
      LOG.info("There are no receive buffers to be released for rank : " + id);
      return;
    }
    //Need to reuse created byte[]'s
    DataBuffer local;
    int receiveBufferSize = DataFlowContext.bufferSize(config);
    if (localReceiveBuffers.size() == 0) {
      local = new DataBuffer(ByteBuffer.allocate(receiveBufferSize));
    } else {
      local = localReceiveBuffers.poll();
    }

    // we should always have a buffer
    copyToLocalBuffer(id, channelMessage, local, currentMessage);
  }

  private void copyToLocalBuffer(int id, DataBuffer dataBuffer, DataBuffer localBuffer,
                                 InMessage message) {
    ByteBuffer original = dataBuffer.getByteBuffer();
    ByteBuffer local = localBuffer.getByteBuffer();
    int position = original.position();
    original.rewind();
    local.put(original);
    local.flip();
    local.position(position);
    localBuffer.setSize(dataBuffer.getSize());
    message.addOverFlowBuffer(localBuffer);
    original.clear();
    Queue<DataBuffer> list = receiveBuffers.get(id);
    if (!list.offer(dataBuffer)) {
      throw new RuntimeException(String.format("%d Buffer release failed for target %d",
          executor, message.getHeader().getDestinationIdentifier()));
    }
  }

  private void releaseTheBuffers(int id, ChannelMessage message) {
    if (MessageDirection.IN == message.getMessageDirection()) {
      Queue<DataBuffer> list = receiveBuffers.get(id);
      for (DataBuffer buffer : message.getNormalBuffers()) {
        // we need to reset the buffer so it can be used again
        buffer.getByteBuffer().clear();
        if (!list.offer(buffer)) {
          throw new RuntimeException(String.format("%d Buffer release failed for target %d",
              executor, message.getHeader().getDestinationIdentifier()));
        }
      }
      if (message.getOverflowBuffers().size() > 0) {
        for (DataBuffer byteBuffer : message.getOverflowBuffers()) {
          byteBuffer.getByteBuffer().clear();
          if (!localReceiveBuffers.offer(byteBuffer)) {
            throw new RuntimeException(String.format("%d Local buffer release failed for target %d",
                executor, message.getHeader().getDestinationIdentifier()));
          }
        }
        message.getOverflowBuffers().clear();
      }
    } else if (MessageDirection.OUT == message.getMessageDirection()) {
      ArrayBlockingQueue<DataBuffer> queue = (ArrayBlockingQueue<DataBuffer>) sendBuffers;
      for (DataBuffer buffer : message.getNormalBuffers()) {
        // we need to reset the buffer so it can be used again
        buffer.getByteBuffer().clear();
        if (!queue.offer(buffer)) {
          throw new RuntimeException(String.format("%d Buffer release failed for source %d %d %d",
              executor, message.getOriginatingId(), queue.size(), queue.remainingCapacity()));
        }
      }
    }
  }

  public TaskPlan getInstancePlan() {
    return instancePlan;
  }

  public Config getConfig() {
    return config;
  }

  public void setKeyType(MessageType keyType) {
    this.keyType = keyType;
  }

  public void close() {
    for (int exec : receivingExecutors) {
      channel.releaseBuffers(exec, edge);
    }
  }
}
