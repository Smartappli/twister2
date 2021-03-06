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
package edu.iu.dsc.tws.tsched.streaming.roundrobin;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import edu.iu.dsc.tws.api.comms.Op;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.compute.graph.ComputeGraph;
import edu.iu.dsc.tws.api.compute.graph.OperationMode;
import edu.iu.dsc.tws.api.compute.schedule.elements.TaskInstancePlan;
import edu.iu.dsc.tws.api.compute.schedule.elements.TaskSchedulePlan;
import edu.iu.dsc.tws.api.compute.schedule.elements.Worker;
import edu.iu.dsc.tws.api.compute.schedule.elements.WorkerPlan;
import edu.iu.dsc.tws.api.compute.schedule.elements.WorkerSchedulePlan;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.config.Context;
import edu.iu.dsc.tws.task.impl.ComputeConnection;
import edu.iu.dsc.tws.task.impl.ComputeGraphBuilder;
import edu.iu.dsc.tws.tsched.utils.TaskSchedulerClassTest;

public class RoundRobinTaskSchedulerTest {

  private static final Logger LOG = Logger.getLogger(RoundRobinTaskSchedulerTest.class.getName());

  @Test
  public void testUniqueSchedules() {
    int parallel = 256;
    ComputeGraph graph = createGraph(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(parallel);

    for (int i = 0; i < 1; i++) {
      TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);
      TaskSchedulePlan plan2 = scheduler.schedule(graph, workerPlan);

      Assert.assertEquals(plan1.getContainers().size(), plan2.getContainers().size());

      Map<Integer, WorkerSchedulePlan> map2 = plan2.getContainersMap();
      for (WorkerSchedulePlan workerSchedulePlan : plan1.getContainers()) {
        WorkerSchedulePlan p2 = map2.get(workerSchedulePlan.getContainerId());
        Assert.assertTrue(containerEquals(workerSchedulePlan, p2));
      }
    }
  }

  @Test
  public void testUniqueSchedules2() {
    int parallel = 256;
    ComputeGraph graph = createGraph(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(parallel);
    TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);

    WorkerPlan workerPlan2 = createWorkPlan2(parallel);
    for (int i = 0; i < 1000; i++) {
      TaskSchedulePlan plan2 = scheduler.schedule(graph, workerPlan2);

      Assert.assertEquals(plan1.getContainers().size(), plan2.getContainers().size());

      Map<Integer, WorkerSchedulePlan> map2 = plan2.getContainersMap();
      for (WorkerSchedulePlan workerSchedulePlan : plan1.getContainers()) {
        WorkerSchedulePlan p2 = map2.get(workerSchedulePlan.getContainerId());

        Assert.assertTrue(containerEquals(workerSchedulePlan, p2));
      }
    }
  }

  @Test
  public void testUniqueSchedules3() {
    int parallel = 16;
    int workers = 2;
    ComputeGraph graph = createGraphWithGraphConstraints(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(workers);
    TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);

    Map<Integer, WorkerSchedulePlan> containersMap = plan1.getContainersMap();
    for (Map.Entry<Integer, WorkerSchedulePlan> entry : containersMap.entrySet()) {

      WorkerSchedulePlan workerSchedulePlan = entry.getValue();
      Set<TaskInstancePlan> containerPlanTaskInstances = workerSchedulePlan.getTaskInstances();

      Assert.assertEquals(containerPlanTaskInstances.size(), Integer.parseInt(
          graph.getGraphConstraints().get(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)));
    }
  }

  @Test
  public void testUniqueSchedules4() {
    int parallel = 16;
    int workers = 2;
    ComputeGraph graph = createGraphWithComputeTaskAndConstraints(parallel);
    RoundRobinTaskScheduler scheduler = new RoundRobinTaskScheduler();
    scheduler.initialize(Config.newBuilder().build());

    WorkerPlan workerPlan = createWorkPlan(workers);
    TaskSchedulePlan plan1 = scheduler.schedule(graph, workerPlan);

    Map<Integer, WorkerSchedulePlan> containersMap = plan1.getContainersMap();
    for (Map.Entry<Integer, WorkerSchedulePlan> entry : containersMap.entrySet()) {

      WorkerSchedulePlan workerSchedulePlan = entry.getValue();
      Set<TaskInstancePlan> containerPlanTaskInstances = workerSchedulePlan.getTaskInstances();

      Assert.assertEquals(containerPlanTaskInstances.size(), Integer.parseInt(
          graph.getGraphConstraints().get(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)));
    }
  }

  private boolean containerEquals(WorkerSchedulePlan p1,
                                  WorkerSchedulePlan p2) {
    if (p1.getContainerId() != p2.getContainerId()) {
      return false;
    }

    if (p1.getTaskInstances().size() != p2.getTaskInstances().size()) {
      return false;
    }

    for (TaskInstancePlan instancePlan : p1.getTaskInstances()) {
      if (!p2.getTaskInstances().contains(instancePlan)) {
        return false;
      }
    }
    return true;
  }

  private WorkerPlan createWorkPlan(int workers) {
    WorkerPlan plan = new WorkerPlan();
    for (int i = 0; i < workers; i++) {
      plan.addWorker(new Worker(i));
    }
    return plan;
  }

  private WorkerPlan createWorkPlan2(int workers) {
    WorkerPlan plan = new WorkerPlan();
    for (int i = workers - 1; i >= 0; i--) {
      plan.addWorker(new Worker(i));
    }
    return plan;
  }

  private ComputeGraph createGraph(int parallel) {
    TaskSchedulerClassTest.TestSource testSource = new TaskSchedulerClassTest.TestSource();
    TaskSchedulerClassTest.TestSink testSink = new TaskSchedulerClassTest.TestSink();

    ComputeGraphBuilder builder = ComputeGraphBuilder.newBuilder(Config.newBuilder().build());
    builder.addSource("source", testSource, parallel);
    ComputeConnection c = builder.addCompute("sink", testSink, parallel);
    c.reduce("source")
        .viaEdge("edge")
        .withOperation(Op.SUM, MessageTypes.INTEGER_ARRAY);
    builder.setMode(OperationMode.STREAMING);
    return builder.build();
  }

  private ComputeGraph createGraphWithGraphConstraints(int parallel) {
    TaskSchedulerClassTest.TestSource testSource = new TaskSchedulerClassTest.TestSource();
    TaskSchedulerClassTest.TestSink testSink = new TaskSchedulerClassTest.TestSink();

    ComputeGraphBuilder builder = ComputeGraphBuilder.newBuilder(Config.newBuilder().build());
    builder.addSource("source", testSource, parallel);
    ComputeConnection c = builder.addCompute("sink", testSink, parallel);
    c.reduce("source").viaEdge("edge").withOperation(Op.SUM, MessageTypes.INTEGER_ARRAY);
    builder.setMode(OperationMode.STREAMING);

    builder.addGraphConstraints(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER, "16");
    ComputeGraph graph = builder.build();
    return graph;
  }

  private ComputeGraph createGraphWithComputeTaskAndConstraints(int parallel) {

    TaskSchedulerClassTest.TestSource testSource = new TaskSchedulerClassTest.TestSource();
    TaskSchedulerClassTest.TestCompute testCompute = new TaskSchedulerClassTest.TestCompute();
    TaskSchedulerClassTest.TestSink testSink = new TaskSchedulerClassTest.TestSink();

    ComputeGraphBuilder computeGraphBuilder =
        ComputeGraphBuilder.newBuilder(Config.newBuilder().build());
    computeGraphBuilder.addSource("source", testSource, parallel);

    ComputeConnection computeConnection = computeGraphBuilder.addCompute(
        "compute", testCompute, parallel);
    ComputeConnection sinkComputeConnection = computeGraphBuilder.addCompute(
        "sink", testSink, parallel);

    computeConnection.direct("source")
        .viaEdge("cdirect-edge")
        .withDataType(MessageTypes.OBJECT);

    sinkComputeConnection.direct("compute")
        .viaEdge("sdirect-edge")
        .withDataType(MessageTypes.OBJECT);

    computeGraphBuilder.setMode(OperationMode.STREAMING);

    computeGraphBuilder.addGraphConstraints(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER, "24");
    ComputeGraph taskGraph = computeGraphBuilder.build();
    return taskGraph;
  }
}
