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

package edu.iu.dsc.tws.api.tset.link.streaming;

import edu.iu.dsc.tws.api.comms.messaging.types.MessageType;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.tset.Constants;
import edu.iu.dsc.tws.api.tset.TSetGraph;
import edu.iu.dsc.tws.api.tset.fn.Sink;
import edu.iu.dsc.tws.api.tset.TSetEnvironment;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.fn.FlatMapFunction;
import edu.iu.dsc.tws.api.tset.fn.MapFunction;
import edu.iu.dsc.tws.api.tset.link.BaseTLink;
import edu.iu.dsc.tws.api.tset.sets.BaseTSet;
import edu.iu.dsc.tws.api.tset.sets.SinkTSet;
import edu.iu.dsc.tws.api.tset.sets.streaming.StreamingFlatMapTSet;
import edu.iu.dsc.tws.api.tset.sets.streaming.StreamingMapTSet;
import edu.iu.dsc.tws.task.impl.ComputeConnection;

/**
 * Represent a data set created by an all gather operation
 *
 * @param <T> type of data
 */
public class StreamingAllGatherTLink<T> extends BaseTLink<T> {

  public StreamingAllGatherTLink(TSetEnvironment tSetEnv, int sourceParalellism) {
    super(tSetEnv, TSetUtils.generateName("sallgather"), sourceParalellism);
  }

  public <P> StreamingMapTSet<T, P> map(MapFunction<T, P> mapFn) {
    StreamingMapTSet<T, P> set = new StreamingMapTSet<>(getTSetEnv(), mapFn,
        getSourceParallelism());
    addChildToGraph(set);
    return set;
  }

  public <P> StreamingFlatMapTSet<T, P> flatMap(FlatMapFunction<T, P> mapFn) {
    StreamingFlatMapTSet<T, P> set = new StreamingFlatMapTSet<T, P>(getTSetEnv(), mapFn,
        getSourceParallelism());
    addChildToGraph(set);
    return set;
  }

  public SinkTSet<T> sink(Sink<T> sink, int parallelism) {
//    SinkTSet<T> sinkTSet = new SinkTSet<>(config, tSetEnv, this, sink, parallelism);
//    addChildToGraph(sinkTSet);
//    tSetEnv.run();
//    return sinkTSet;
    return null;
  }

  @Override
  public void build(TSetGraph tSetGraph) {
//    MessageType dataType = TSetUtils.getDataType(getType());
//    connection.allgather(parent.getName()).viaEdge(Constants.DEFAULT_EDGE).withDataType(dataType);
  }

  @Override
  public BaseTLink<T> setName(String n) {
    rename(n);
    return this;
  }
}
