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
package edu.iu.dsc.tws.examples.task.streaming;

import java.util.List;

import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.data.api.DataType;
import edu.iu.dsc.tws.examples.task.BenchTaskWorker;
import edu.iu.dsc.tws.examples.task.TaskExamples;
import edu.iu.dsc.tws.task.streaming.BaseStreamSink;
import edu.iu.dsc.tws.task.streaming.BaseStreamSource;

public class STGatherExample extends BenchTaskWorker {

  @Override
  public TaskGraphBuilder buildTaskGraph() {
    List<Integer> taskStages = jobParameters.getTaskStages();
    int psource = taskStages.get(0);
    int psink = taskStages.get(1);
    DataType dataType = DataType.INTEGER;
    String edge = "edge";
    TaskExamples taskExamples = new TaskExamples();
    BaseStreamSource g = new SourceStreamTask(edge);
    BaseStreamSink r = taskExamples.getStreamSinkClass("gather");
    taskGraphBuilder.addSource(SOURCE, g, psource);
    computeConnection = taskGraphBuilder.addSink(SINK, r, psink);
    computeConnection.gather(SOURCE, edge, dataType);
    return taskGraphBuilder;
  }
}