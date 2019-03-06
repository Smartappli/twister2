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
package edu.iu.dsc.tws.executor.threading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.executor.api.IExecution;
import edu.iu.dsc.tws.executor.api.INodeInstance;
import edu.iu.dsc.tws.executor.api.IParallelOperation;

public class BatchSharingExecutor extends ThreadSharingExecutor {
  private static final Logger LOG = Logger.getLogger(BatchSharingExecutor.class.getName());

  // keep track of finished executions
  private Map<Integer, Boolean> finishedInstances = new ConcurrentHashMap<>();

  private int workerId;

  private boolean notStopped = true;

  private boolean cleanUpCalled = false;

  public BatchSharingExecutor(int workerId) {
    this.workerId = workerId;
  }

  /**
   * Execution Method for Batch Tasks
   */
  public boolean runExecution() {
    Map<Integer, INodeInstance> nodes = executionPlan.getNodes();

    if (nodes.size() == 0) {
      LOG.warning(String.format("Worker %d has zero assigned tasks, you may "
          + "have more workers than tasks", workerId));
      return true;
    }

    scheduleExecution(nodes);

    // we progress until all the channel finish
    while (finishedInstances.size() != nodes.size()) {
      channel.progress();
    }

    cleanUp(nodes);
    return true;
  }

  private void scheduleExecution(Map<Integer, INodeInstance> nodes) {
    // initialize finished
    // initFinishedInstances();
    tasks = new ArrayBlockingQueue<>(nodes.size() * 2);
    tasks.addAll(nodes.values());

    int curTaskSize = tasks.size();
    BatchWorker[] workers = new BatchWorker[curTaskSize];

    // prepare the tasks
    for (INodeInstance node : tasks) {
      node.prepare(config);
    }

    for (int i = 0; i < curTaskSize; i++) {
      workers[i] = new BatchWorker();
      Thread t = new Thread(workers[i], "Executor-" + i);
      threads.add(t);
      t.start();
    }
  }

  private void cleanUp(Map<Integer, INodeInstance> nodes) {
    // lets wait for thread to finish
    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException ignore) {
      }
    }

    // clean up the instances
    for (INodeInstance node : nodes.values()) {
      node.close();
    }

    // lets close the operations
    List<IParallelOperation> ops = executionPlan.getParallelOperations();
    for (IParallelOperation op : ops) {
      op.close();
    }

    cleanUpCalled = true;
  }

  @Override
  public IExecution runIExecution() {
    Map<Integer, INodeInstance> nodes = executionPlan.getNodes();

    if (nodes.size() == 0) {
      LOG.warning(String.format("Worker %d has zero assigned tasks, you may "
          + "have more workers than tasks", workerId));
      return null;
    }

    scheduleExecution(nodes);
    return new BatchExecution(nodes);
  }

  protected class BatchWorker implements Runnable {
    @Override
    public void run() {
      while (notStopped) {
        try {
          INodeInstance nodeInstance = tasks.poll();
          if (nodeInstance != null) {
            boolean needsFurther = nodeInstance.execute();
            if (!needsFurther) {
              finishedInstances.put(nodeInstance.getId(), true);
            } else {
              // we need to further execute this task
              tasks.offer(nodeInstance);
            }
          } else {
            break;
          }
        } catch (Throwable t) {
          LOG.log(Level.SEVERE, String.format("%d Error in executor", workerId), t);
          throw new RuntimeException("Error occurred in execution of task", t);
        }
      }
    }
  }

  private class BatchExecution implements IExecution {
    private Map<Integer, INodeInstance> nodeMap;

    BatchExecution(Map<Integer, INodeInstance> nodeMap) {
      this.nodeMap = nodeMap;
    }

    @Override
    public boolean waitForCompletion() {
      // we progress until all the channel finish
      while (notStopped && finishedInstances.size() != nodeMap.size()) {
        channel.progress();
      }

      cleanUp(nodeMap);
      return true;
    }

    @Override
    public boolean progress() {
      // we progress until all the channel finish
      if (notStopped && finishedInstances.size() != nodeMap.size()) {
        channel.progress();
        return true;
      }

      return false;
    }

    public void close() {
      if (notStopped) {
        throw new RuntimeException("We need to stop the execution before close");
      }

      if (!cleanUpCalled) {
        cleanUp(nodeMap);
        cleanUpCalled = true;
      } else {
        throw new RuntimeException("Close is called on a already closed execution");
      }
    }

    @Override
    public void stop() {
      notStopped = false;
    }
  }
}
