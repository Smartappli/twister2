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
package edu.iu.dsc.tws.comms.mpi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.api.MessageHeader;
import edu.iu.dsc.tws.comms.api.MessageReceiver;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.TWSChannel;
import edu.iu.dsc.tws.comms.core.TaskPlan;
import edu.iu.dsc.tws.comms.mpi.io.MPIMultiMessageDeserializer;
import edu.iu.dsc.tws.comms.mpi.io.MPIMultiMessageSerializer;
import edu.iu.dsc.tws.comms.mpi.io.MessageDeSerializer;
import edu.iu.dsc.tws.comms.mpi.io.MessageSerializer;
import edu.iu.dsc.tws.comms.routing.InvertedBinaryTreeRouter;
import edu.iu.dsc.tws.comms.utils.KryoSerializer;

public class MPIDataFlowGather implements DataFlowOperation, MPIMessageReceiver {
  private static final Logger LOG = Logger.getLogger(MPIDataFlowGather.class.getName());

  // the source tasks
  protected Set<Integer> sources;

  // the destination task
  protected int destination;

  private InvertedBinaryTreeRouter router;

  private MessageReceiver finalReceiver;

  private MessageReceiver partialReceiver;

  private int index = 0;

  private int pathToUse = MPIContext.DEFAULT_PATH;

  private MPIDataFlowOperation delegete;
  private TaskPlan instancePlan;
  private int executor;
  private MessageType type;
  private MessageType keyType;
  private boolean isKeyed;

  public MPIDataFlowGather(TWSChannel channel, Set<Integer> sources, int destination,
                           MessageReceiver finalRcvr,
                           int indx, int p,
                           Config cfg, MessageType t, TaskPlan taskPlan, int edge) {
    this(channel, sources, destination, finalRcvr, new PartialGather(),
        indx, p, cfg, t, taskPlan, edge);
  }

  public MPIDataFlowGather(TWSChannel channel, Set<Integer> sources, int destination,
                           MessageReceiver finalRcvr,
                           int indx, int p,
                           Config cfg, MessageType t, MessageType keyType,
                           TaskPlan taskPlan, int edge) {
    this(channel, sources, destination, finalRcvr, new PartialGather(),
        indx, p, cfg, t, keyType, taskPlan, edge);
    this.isKeyed = true;
  }

  public MPIDataFlowGather(TWSChannel channel, Set<Integer> sources, int destination,
                           MessageReceiver finalRcvr,
                           MessageReceiver partialRcvr, int indx, int p,
                           Config cfg, MessageType t, TaskPlan taskPlan, int edge) {
    this(channel, sources, destination, finalRcvr, partialRcvr,
        indx, p, cfg, t, MessageType.SHORT, taskPlan, edge);
    this.isKeyed = false;
  }

  public MPIDataFlowGather(TWSChannel channel, Set<Integer> sources, int destination,
                           MessageReceiver finalRcvr,
                           MessageReceiver partialRcvr, int indx, int p,
                           Config cfg, MessageType t, MessageType kt, TaskPlan taskPlan, int edge) {
    this.index = indx;
    this.sources = sources;
    this.destination = destination;
    this.finalReceiver = finalRcvr;
    this.partialReceiver = partialRcvr;
    this.pathToUse = p;
    this.keyType = kt;
    this.instancePlan = taskPlan;
    this.isKeyed = true;

    this.delegete = new MPIDataFlowOperation(channel);
  }

  protected boolean isLast() {
    return router.isLastReceiver();
  }

  /**
   * We can receive messages from internal tasks or an external task, we allways receive messages
   * to the main task of the executor and we go from there
   *
   * @param currentMessage
   * @param object
   */
  @Override
  public boolean receiveMessage(MPIMessage currentMessage, Object object) {
    MessageHeader header = currentMessage.getHeader();

    // we always receive to the main task
    int messageDestId = currentMessage.getHeader().getDestinationIdentifier();
    // check weather this message is for a sub task
    if (!isLast()
        && partialReceiver != null) {
//      LOG.info(String.format("%d calling PARTIAL receiver %d", executor, header.getSourceId()));
      return partialReceiver.onMessage(header.getSourceId(),
          MPIContext.DEFAULT_PATH,
          router.mainTaskOfExecutor(instancePlan.getThisExecutor(),
              MPIContext.DEFAULT_PATH), header.getFlags(), currentMessage);
    } else {
//      LOG.info(String.format("%d calling FINAL receiver %d", executor, header.getSourceId()));
      return finalReceiver.onMessage(header.getSourceId(),
          MPIContext.DEFAULT_PATH, router.mainTaskOfExecutor(instancePlan.getThisExecutor(),
              MPIContext.DEFAULT_PATH), header.getFlags(), object);
    }
  }

  private RoutingParameters partialSendRoutingParameters(int source, int path) {
    RoutingParameters routingParameters = new RoutingParameters();
    // get the expected routes
    Map<Integer, Set<Integer>> internalRoutes = router.getInternalSendTasks(source);
    if (internalRoutes == null) {
      throw new RuntimeException("Un-expected message from source: " + source);
    }

    Set<Integer> sourceInternalRouting = internalRoutes.get(source);
    if (sourceInternalRouting != null) {
      routingParameters.addInternalRoutes(sourceInternalRouting);
    }

    // get the expected routes
    Map<Integer, Set<Integer>> externalRoutes =
        router.getExternalSendTasksForPartial(source);
    if (externalRoutes == null) {
      throw new RuntimeException("Un-expected message from source: " + source);
    }

    Set<Integer> sourceRouting = externalRoutes.get(source);
    if (sourceRouting != null) {
      routingParameters.addExternalRoutes(sourceRouting);
    }

    routingParameters.setDestinationId(router.destinationIdentifier(source, path));
    return routingParameters;
  }

  private RoutingParameters sendRoutingParameters(int source, int path) {
    RoutingParameters routingParameters = new RoutingParameters();

    // get the expected routes
    Map<Integer, Set<Integer>> internalRouting = router.getInternalSendTasks(source);
    if (internalRouting == null) {
      throw new RuntimeException("Un-expected message from source: " + source);
    }

    // we are going to add source if we are the main executor
    if (router.mainTaskOfExecutor(instancePlan.getThisExecutor(),
        MPIContext.DEFAULT_PATH) == source) {
      routingParameters.addInteranlRoute(source);
    }

    // we should not have the route for main task to outside at this point
    Set<Integer> sourceInternalRouting = internalRouting.get(source);
    if (sourceInternalRouting != null) {
      routingParameters.addInternalRoutes(sourceInternalRouting);
    }

    routingParameters.setDestinationId(router.destinationIdentifier(source, path));
    return routingParameters;
  }

  private boolean isLastReceiver() {
    return router.isLastReceiver();
  }

  public boolean receiveSendInternally(int source, int t, int path, int flags, Object message) {
    // check weather this is the last task
    if (router.isLastReceiver()) {
//      LOG.info(String.format("%d internally FINAL receiver %d %s", executor, source,
//          finalReceiver.getClass().getName()));
      return finalReceiver.onMessage(source, path, t, flags, message);
    } else {
//      LOG.info(String.format("%d internally PARTIAL receiver %d %s", executor, source,
//          partialReceiver.getClass().getName()));
      // now we need to serialize this to the buffer
      return partialReceiver.onMessage(source, path, t, flags, message);
    }
  }

  @Override
  public boolean passMessageDownstream(Object object, MPIMessage currentMessage) {
    return true;
  }

  @Override
  public boolean send(int source, Object message, int flags) {
    return delegete.sendMessage(source, message, pathToUse, flags,
        sendRoutingParameters(source, pathToUse));
  }

  @Override
  public boolean send(int source, Object message, int flags, int dest) {
    return delegete.sendMessage(source, message, dest, flags,
        sendRoutingParameters(source, dest));
  }

  @Override
  public boolean sendPartial(int source, Object message, int flags, int dest) {
    return delegete.sendMessagePartial(source, message, dest, flags,
        partialSendRoutingParameters(source, dest));
  }

  @Override
  public void init(Config cfg, MessageType t, TaskPlan taskPlan, int edge) {
    this.type = t;
    this.instancePlan = taskPlan;
    this.executor = taskPlan.getThisExecutor();
    // we only have one path
    this.router = new InvertedBinaryTreeRouter(cfg, taskPlan,
        destination, sources, index);

    // initialize the receive
    if (this.partialReceiver != null && !isLastReceiver()) {
      partialReceiver.init(cfg, this, receiveExpectedTaskIds());
    }

    if (this.finalReceiver != null && isLastReceiver()) {
      this.finalReceiver.init(cfg, this, receiveExpectedTaskIds());
    }

    Map<Integer, ArrayBlockingQueue<Pair<Object, MPISendMessage>>> pendingSendMessagesPerSource =
        new HashMap<>();
    Map<Integer, Queue<Pair<Object, MPIMessage>>> pendingReceiveMessagesPerSource = new HashMap<>();
    Map<Integer, Queue<MPIMessage>> pendingReceiveDeSerializations = new HashMap<>();

    Set<Integer> srcs = router.sendQueueIds();
    for (int s : srcs) {
      // later look at how not to allocate pairs for this each time
      ArrayBlockingQueue<Pair<Object, MPISendMessage>> pendingSendMessages =
          new ArrayBlockingQueue<Pair<Object, MPISendMessage>>(
              MPIContext.sendPendingMax(cfg));
      pendingSendMessagesPerSource.put(s, pendingSendMessages);
    }

    int maxReceiveBuffers = MPIContext.receiveBufferCount(cfg);
    int receiveExecutorsSize = receivingExecutors().size();
    if (receiveExecutorsSize == 0) {
      receiveExecutorsSize = 1;
    }
    Set<Integer> execs = router.receivingExecutors();
    for (int e : execs) {
      int capacity = maxReceiveBuffers * 2 * receiveExecutorsSize;
      Queue<Pair<Object, MPIMessage>> pendingReceiveMessages =
          new ArrayBlockingQueue<Pair<Object, MPIMessage>>(
              capacity);
      pendingReceiveMessagesPerSource.put(e, pendingReceiveMessages);
      pendingReceiveDeSerializations.put(e, new ArrayBlockingQueue<MPIMessage>(capacity));
    }

    KryoSerializer kryoSerializer = new KryoSerializer();
    kryoSerializer.init(new HashMap<String, Object>());

    MessageDeSerializer messageDeSerializer =
        new MPIMultiMessageDeserializer(kryoSerializer, executor);
    MessageSerializer messageSerializer =
        new MPIMultiMessageSerializer(kryoSerializer, executor);

    delegete.init(cfg, t, taskPlan, edge,
        router.receivingExecutors(), router.isLastReceiver(), this,
        pendingSendMessagesPerSource, pendingReceiveMessagesPerSource,
        pendingReceiveDeSerializations, messageSerializer, messageDeSerializer, isKeyed);
    delegete.setKeyType(keyType);
  }

  @Override
  public boolean sendPartial(int source, Object message, int flags) {
    // now what we need to do
    return delegete.sendMessagePartial(source, message, pathToUse, flags,
        partialSendRoutingParameters(source, pathToUse));
  }

  protected Set<Integer> receivingExecutors() {
    return router.receivingExecutors();
  }

  public Map<Integer, List<Integer>> receiveExpectedTaskIds() {
    Map<Integer, List<Integer>> integerMapMap = router.receiveExpectedTaskIds();
    // add the main task to receive from iteself
    int key = router.mainTaskOfExecutor(instancePlan.getThisExecutor(), MPIContext.DEFAULT_PATH);
    List<Integer> mainReceives = integerMapMap.get(key);
    if (mainReceives == null) {
      mainReceives = new ArrayList<>();
      integerMapMap.put(key, mainReceives);
    }
    if (key != destination) {
      mainReceives.add(key);
    }
    return integerMapMap;
  }

  @Override
  public void progress() {
    delegete.progress();

    finalReceiver.progress();
    partialReceiver.progress();
  }

  @Override
  public void close() {

  }

  @Override
  public void finish() {

  }

  @Override
  public MessageType getType() {
    return type;
  }

  @Override
  public TaskPlan getTaskPlan() {
    return instancePlan;
  }

  @Override
  public void setMemoryMapped(boolean memoryMapped) {
    delegete.setStoreBased(memoryMapped);
  }

  private static class PartialGather implements MessageReceiver {
    // lets keep track of the messages
    // for each task we need to keep track of incoming messages
    private Map<Integer, Map<Integer, List<Object>>> messages = new TreeMap<>();
    private Map<Integer, Map<Integer, Integer>> counts = new HashMap<>();
    private int currentIndex = 0;
    private DataFlowOperation dataFlowOperation;
    private int executor;
    private String threadName;

    @Override
    public void init(Config cfg, DataFlowOperation op, Map<Integer, List<Integer>> expectedIds) {
      executor = op.getTaskPlan().getThisExecutor();

      LOG.info(String.format("%d expected ids %s", executor, expectedIds));
      for (Map.Entry<Integer, List<Integer>> e : expectedIds.entrySet()) {
        Map<Integer, List<Object>> messagesPerTask = new HashMap<>();
        Map<Integer, Integer> countsPerTask = new HashMap<>();

        for (int i : e.getValue()) {
          messagesPerTask.put(i, new ArrayList<Object>());
          countsPerTask.put(i, 0);
        }

        messages.put(e.getKey(), messagesPerTask);
        counts.put(e.getKey(), countsPerTask);
      }
      this.dataFlowOperation = op;
      this.executor = dataFlowOperation.getTaskPlan().getThisExecutor();
    }

    @Override
    public boolean onMessage(int source, int path, int target, int flags, Object object) {
      // add the object to the map
      boolean canAdd = true;

      if (this.threadName == null) {
        this.threadName = Thread.currentThread().getName();
      }
      String tn = Thread.currentThread().getName();
      if (!tn.equals(threadName)) {
        throw new RuntimeException(String.format("%d Threads are not equal %s %s",
            executor, threadName, tn));
      }
      if (messages.get(target) == null) {
        throw new RuntimeException(String.format("%d Partial receive error %d", executor, target));
      }
      List<Object> m = messages.get(target).get(source);
      Integer c = counts.get(target).get(source);
      if (m.size() > 16) {
        canAdd = false;
//       LOG.info(String.format("%d Partial false: target %d source %d", executor, target, source));
      } else {
        // we need to increment the reference count to make the buffers available
        // other wise they will bre reclaimed
//        LOG.info(String.format("%d Partial true: target %d source %d %s",
//            executor, target, source, counts.get(target)));
        if (object instanceof MPIMessage) {
          ((MPIMessage) object).incrementRefCount();
        }
        m.add(object);
        counts.get(target).put(source, c + 1);
      }
      return canAdd;
    }

    public void progress() {
      if (this.threadName == null) {
        this.threadName = Thread.currentThread().getName();
      }
      String tn = Thread.currentThread().getName();
      if (!tn.equals(threadName)) {
        throw new RuntimeException(String.format("%d Threads are not equal %s %s",
            executor, threadName, tn));
      }

      for (int t : messages.keySet()) {
        boolean canProgress = true;
        while (canProgress) {
          // now check weather we have the messages for this source
          Map<Integer, List<Object>> map = messages.get(t);
          Map<Integer, Integer> cMap = counts.get(t);
          boolean found = true;
          for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
            if (e.getValue().size() == 0) {
              found = false;
              canProgress = false;
            }
          }

          if (map.entrySet().size() == 0) {
            LOG.info(String.format("%d entry size is ZERO %d %s", executor, t, counts));
          }

          if (found) {
            List<Object> out = new ArrayList<>();
            for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
              Object e1 = e.getValue().get(0);
              out.add(e1);
            }
            if (dataFlowOperation.sendPartial(t, out, MessageFlags.FLAGS_MULTI_MSG, t)) {
              for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
                List<Object> value = e.getValue();
                if (value.size() == 0) {
                  LOG.info(String.format("%d list size ZERO task %d %d", executor, t, e.getKey()));
                }
                value.remove(0);
              }
              for (Map.Entry<Integer, Integer> e : cMap.entrySet()) {
                Integer i = e.getValue();
                e.setValue(i - 1);
              }
            } else {
              canProgress = false;
            }
          }
        }
      }
    }
  }
}
