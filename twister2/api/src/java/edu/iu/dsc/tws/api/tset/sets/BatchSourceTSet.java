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

package edu.iu.dsc.tws.api.tset.sets;

import edu.iu.dsc.tws.api.task.nodes.ICompute;
import edu.iu.dsc.tws.api.tset.TSetEnvironment;
import edu.iu.dsc.tws.api.tset.TSetGraph;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.fn.IterableFlatMapFunction;
import edu.iu.dsc.tws.api.tset.fn.IterableMapFunction;
import edu.iu.dsc.tws.api.tset.fn.Sink;
import edu.iu.dsc.tws.api.tset.fn.Source;
import edu.iu.dsc.tws.api.tset.link.DirectTLink;
import edu.iu.dsc.tws.api.tset.ops.SourceOp;

public class BatchSourceTSet<T> extends BatchBaseTSet<T> {
  private Source<T> source;

  public BatchSourceTSet(TSetEnvironment tSetEnv, Source<T> src, int parallelism) {
    super(tSetEnv, TSetUtils.generateName("source"), parallelism);
    this.source = src;
  }

  public <P> IterableMapTSet<T, P> map(IterableMapFunction<T, P> mapFn) {
    DirectTLink<T> direct = new DirectTLink<>(getTSetEnv(), getParallelism());
    addChildToGraph(direct);
    return direct.map(mapFn);
  }

  public <P> IterableFlatMapTSet<T, P> flatMap(IterableFlatMapFunction<T, P> mapFn) {
    DirectTLink<T> direct = new DirectTLink<>(getTSetEnv(), getParallelism());
    addChildToGraph(direct);
    return direct.flatMap(mapFn);
  }

  public SinkTSet<T> sink(Sink<T> sink) {
    DirectTLink<T> direct = new DirectTLink<>(getTSetEnv(), getParallelism());
    addChildToGraph(direct);
    return direct.sink(sink);
  }

  @Override
  public void build(TSetGraph tSetGraph) {
    SourceOp<T> sourceOp = new SourceOp<>(source);
    tSetGraph.getDfwGraphBuilder().addSource(getName(), sourceOp, getParallelism());
  }

  @Override
  protected ICompute getTask() {
    throw new UnsupportedOperationException("source would not have an icompute task");
  }

  @Override
  public BatchBaseTSet<T> setName(String name) {
    rename(name);
    return this;
  }

}
