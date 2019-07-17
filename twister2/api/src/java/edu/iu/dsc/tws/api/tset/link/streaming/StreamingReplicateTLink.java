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
import edu.iu.dsc.tws.api.tset.Constants;
import edu.iu.dsc.tws.api.tset.TSetEnvironment;
import edu.iu.dsc.tws.api.tset.TSetGraph;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.fn.FlatMapFunction;
import edu.iu.dsc.tws.api.tset.fn.MapFunction;
import edu.iu.dsc.tws.api.tset.link.BaseTLink;
import edu.iu.dsc.tws.api.tset.sets.streaming.StreamingFlatMapTSet;
import edu.iu.dsc.tws.api.tset.sets.streaming.StreamingMapTSet;

public class StreamingReplicateTLink<T> extends BaseTLink<T> {
  public StreamingReplicateTLink(TSetEnvironment tSetEnv, int reps) {
    super(tSetEnv, TSetUtils.generateName("sreplicate"), 1, reps);
  }

  public <P> StreamingMapTSet<T, P> map(MapFunction<T, P> mapFn) {
    StreamingMapTSet<T, P> set = new StreamingMapTSet<>(getTSetEnv(), mapFn,
        getTargetParallelism());
    addChildToGraph(set);
    return set;
  }

  public <P> StreamingFlatMapTSet<T, P> flatMap(FlatMapFunction<T, P> mapFn) {
    StreamingFlatMapTSet<T, P> set = new StreamingFlatMapTSet<>(getTSetEnv(), mapFn,
        getTargetParallelism());
    addChildToGraph(set);
    return set;
  }

  @Override
  public void build(TSetGraph tSetGraph) {
//    MessageType dataType = TSetUtils.getDataType(getType());
//
//    connection.broadcast(parent.getName()).viaEdge(Constants.DEFAULT_EDGE)
//        .withDataType(dataType).connect();
  }

  @Override
  public StreamingReplicateTLink<T> setName(String n) {
    rename(n);
    return this;
  }
}
