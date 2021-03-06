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
package edu.iu.dsc.tws.tsched.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.compute.graph.Vertex;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.config.Context;
import edu.iu.dsc.tws.tsched.spi.common.TaskSchedulerContext;

/**
 * This class is responsible for calculating the required ram, disk, and cpu values. Also,
 * it calculates the total number of task instances in the task graph for each task vertex.
 * And, it is responsible for calculating the parallel task map for the task vertices in
 * the task graph.
 */
public class TaskAttributes {

  private static final Logger LOG = Logger.getLogger(TaskAttributes.class.getName());

  public int getInstancesPerWorker(Vertex taskVertex, Map<String, String> vertexConstraintsMap) {
    int instancesPerWorker;
    Config config = taskVertex.getConfig();
    if (vertexConstraintsMap != null) {
      if (vertexConstraintsMap.containsKey(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)) {
        instancesPerWorker = Integer.valueOf(String.valueOf(vertexConstraintsMap.get(
            Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)));
      } else {
        instancesPerWorker = TaskSchedulerContext.defaultTaskInstancesPerContainer(config);
      }
    } else {
      instancesPerWorker = TaskSchedulerContext.defaultTaskInstancesPerContainer(config);
    }
    return instancesPerWorker;
  }

  public int getInstancesPerWorker(Map<String, String> graphConstraintsMap) {
    int instancesPerWorker = 0;
    if (graphConstraintsMap.containsKey(Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)) {
      instancesPerWorker = Integer.valueOf(String.valueOf(graphConstraintsMap.get(
          Context.TWISTER2_MAX_TASK_INSTANCES_PER_WORKER)));
    }
    return instancesPerWorker;
  }

  /**
   * This method is to calculate the total number of task instances in the task graph which is based
   * on the parallelism specified in the task graph or else from the task configuration
   * default values.
   */
  public int getTotalNumberOfInstances(Set<Vertex> iTaskSet) {
    Map<String, Integer> parallelTaskMap = getParallelTaskMap(iTaskSet);
    int totalNumberOfInstances = 0;
    for (int instances : parallelTaskMap.values()) {
      totalNumberOfInstances += instances;
    }
    return totalNumberOfInstances;
  }

  public int getTotalNumberOfInstances(Vertex taskVertex) {
    Map<String, Integer> parallelTaskMap = getParallelTaskMap(taskVertex);
    int totalNumberOfInstances = 0;
    for (int instances : parallelTaskMap.values()) {
      totalNumberOfInstances += instances;
    }
    return totalNumberOfInstances;
  }

  public int getTotalNumberOfInstances(Set<Vertex> iTaskSet,
                                       Map<String, Map<String, String>> nodeConstraintsMap) {
    Map<String, Integer> parallelTaskMap = getParallelTaskMap(iTaskSet, nodeConstraintsMap);
    int totalNumberOfInstances = 0;
    for (int instances : parallelTaskMap.values()) {
      totalNumberOfInstances += instances;
    }
    return totalNumberOfInstances;
  }

  public int getTotalNumberOfInstances(Vertex taskVertex,
                                       Map<String, Map<String, String>> nodeConstraintsMap) {
    Map<String, Integer> parallelTaskMap = getParallelTaskMap(taskVertex, nodeConstraintsMap);
    int totalNumberOfInstances = 0;
    for (int instances : parallelTaskMap.values()) {
      totalNumberOfInstances += instances;
    }
    return totalNumberOfInstances;
  }

  /**
   * This method is mainly to generate the parallel task map (maintain order) for the task vertex.
   * If the user specifies the parallelism value greater than or equal "1" will be considered as a
   * parallelism value. Otherwise, the system assign the default parallelism value to the task
   * vertex from the task scheduling configuration file.
   */
  public Map<String, Integer> getParallelTaskMap(Set<Vertex> iTaskSet, Map<String,
      Map<String, String>> nodeConstraintsMap) {
    Map<String, Integer> parallelTaskMap = new LinkedHashMap<>();
    for (Vertex taskVertex : iTaskSet) {
      Config config = taskVertex.getConfig();
      String taskName = taskVertex.getName();
      int parallelTaskCount;
      if (!nodeConstraintsMap.get(taskName).isEmpty()) {
        Map<String, String> vertexMap = nodeConstraintsMap.get(taskName);
        if (vertexMap.containsKey(Context.TWISTER2_TASK_INSTANCE_ODD_PARALLELISM)) {
          parallelTaskCount = Integer.valueOf(String.valueOf(vertexMap.get(
              Context.TWISTER2_TASK_INSTANCE_ODD_PARALLELISM)));
        } else {
          parallelTaskCount = taskVertex.getParallelism();
        }
      } else {
        if (taskVertex.getParallelism() >= 1) {
          parallelTaskCount = taskVertex.getParallelism();
        } else {
          parallelTaskCount = TaskSchedulerContext.taskParallelism(config);
        }
      }
      parallelTaskMap.put(taskName, parallelTaskCount);
    }
    return parallelTaskMap;
  }

  /**
   * This method is to generate the parallel task map for the task vertex. If the user specifies the
   * parallelism value greater than or equal "1" will be considered as a parallelism value.
   * Otherwise, the system assign the default parallelism value to the task vertex from the task
   * scheduling configuration file.
   */
  public Map<String, Integer> getParallelTaskMap(Vertex taskVertex,
                                                 Map<String,
                                                     Map<String, String>> nodeConstraintsMap) {
    Map<String, Integer> parallelTaskMap = new LinkedHashMap<>();
    Config config = taskVertex.getConfig();
    String taskName = taskVertex.getName();
    int parallelTaskCount = 0;
    if (nodeConstraintsMap.get(taskName) != null) {
      Map<String, String> vertexConstraintMap = nodeConstraintsMap.get(taskName);
      if (vertexConstraintMap.containsKey(Context.TWISTER2_TASK_INSTANCE_ODD_PARALLELISM)) {
        parallelTaskCount = Integer.valueOf(String.valueOf(vertexConstraintMap.get(
            Context.TWISTER2_TASK_INSTANCE_ODD_PARALLELISM)));
      } else {
        parallelTaskCount = TaskSchedulerContext.taskParallelism(config);
      }
    }
    parallelTaskMap.put(taskName, parallelTaskCount);
    return parallelTaskMap;
  }

  public Map<String, Integer> getParallelTaskMap(Set<Vertex> iTaskSet) {
    Map<String, Integer> parallelTaskMap = new LinkedHashMap<>();
    for (Vertex taskVertex : iTaskSet) {
      Config config = taskVertex.getConfig();
      String taskName = taskVertex.getName();
      int parallelTaskCount;
      if (taskVertex.getParallelism() >= 1) {
        parallelTaskCount = taskVertex.getParallelism();
      } else {
        parallelTaskCount = TaskSchedulerContext.taskParallelism(config);
      }
      parallelTaskMap.put(taskName, parallelTaskCount);
    }
    return parallelTaskMap;
  }

  public Map<String, Integer> getParallelTaskMap(Vertex taskVertex) {
    Map<String, Integer> parallelTaskMap = new LinkedHashMap<>();
    Config config = taskVertex.getConfig();
    String taskName = taskVertex.getName();
    int parallelTaskCount;
    if (taskVertex.getParallelism() >= 1) {
      parallelTaskCount = taskVertex.getParallelism();
    } else {
      parallelTaskCount = TaskSchedulerContext.taskParallelism(config);
    }
    parallelTaskMap.put(taskName, parallelTaskCount);
    return parallelTaskMap;
  }


  /**
   * This method retrieve the set of task vertices and check if the task vertex has the user
   * specified ram value. If the user doesn't specify the required ram configuration it will
   * assign the default ram value from the task configuration file and store it in the map.
   */
  public Map<String, Double> getTaskRamMap(Set<Vertex> taskVertices) {
    Map<String, Double> taskRamMap = new HashMap<>();
    Object ram;
    double requiredRam;
    for (Vertex task : taskVertices) {
      Config config = task.getConfig();
      if (config.get("Ram") != null) {
        ram = config.get("Ram");
        requiredRam = (double) ((Integer) ram);
      } else {
        requiredRam = TaskSchedulerContext.taskInstanceRam(config);
      }
      taskRamMap.put(task.getName(), requiredRam);
    }
    return taskRamMap;
  }

  /**
   * This method retrieve the set of task vertices and check if the task vertex has the user
   * specified disk value. If the user doesn't specify the required disk configuration it will
   * assign the default disk value from the task configuration file and store it in the map.
   */
  public Map<String, Double> getTaskDiskMap(Set<Vertex> taskVertices) {
    Map<String, Double> taskDiskMap = new LinkedHashMap<>();
    Object disk;
    double requiredDisk;
    for (Vertex task : taskVertices) {
      Config config = task.getConfig();
      if (config.get("Disk") != null) {
        disk = config.get("Disk");
        requiredDisk = (double) ((Integer) disk);
      } else {
        requiredDisk = TaskSchedulerContext.taskInstanceDisk(config);
      }
      taskDiskMap.put(task.getName(), requiredDisk);
    }
    return taskDiskMap;
  }

  /**
   * This method retrieve the set of task vertices and check if the task vertex has the user
   * specified cpu value. If the user doesn't specify the required cpu configuration it will assign
   * the default cpu value from the task configuration file and store it in the map.
   */
  public Map<String, Double> getTaskCPUMap(Set<Vertex> taskVertices) {
    Map<String, Double> taskCPUMap = new LinkedHashMap<>();
    Object cpu;
    double requiredCpu;
    for (Vertex task : taskVertices) {
      Config config = task.getConfig();
      if (config.get("Cpu") != null) {
        cpu = config.get("Cpu");
        requiredCpu = (double) ((Integer) cpu);
      } else {
        requiredCpu = TaskSchedulerContext.taskInstanceCpu(config);
      }
      taskCPUMap.put(task.getName(), requiredCpu);
    }
    return taskCPUMap;
  }

  /**
   * This method retrieve the set of task vertices and check if the task vertex has the user
   * specified network value. If the user doesn't specify the required network configuration it will
   * assign the default network value from the task configuration file and store it in the map.
   */
  public Map<String, Double> getTaskNetworkMap(Set<Vertex> taskVertices) {
    Map<String, Double> taskNetworkMap = new LinkedHashMap<>();
    Object network;
    double requiredNetwork;
    for (Vertex task : taskVertices) {
      Config config = task.getConfig();
      if (config.get("Network") != null) {
        network = config.get("Network");
        requiredNetwork = (double) ((Integer) network);
      } else {
        requiredNetwork = TaskSchedulerContext.taskInstanceNetwork(config);
      }
      taskNetworkMap.put(task.getName(), requiredNetwork);
    }
    return taskNetworkMap;
  }
}
