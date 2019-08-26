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

import edu.iu.dsc.tws.api.tset.env.StreamingTSetEnvironment;
import edu.iu.dsc.tws.api.tset.fn.ComputeCollectorFunc;
import edu.iu.dsc.tws.api.tset.fn.ComputeFunc;
import edu.iu.dsc.tws.api.tset.fn.SinkFunc;
import edu.iu.dsc.tws.api.tset.link.BaseTLink;
import edu.iu.dsc.tws.api.tset.ops.ComputeCollectorOp;
import edu.iu.dsc.tws.api.tset.ops.ComputeOp;
import edu.iu.dsc.tws.api.tset.ops.SinkOp;
import edu.iu.dsc.tws.api.tset.sets.streaming.SComputeTSet;
import edu.iu.dsc.tws.api.tset.sets.streaming.SSinkTSet;

public abstract class SBaseTLink<T1, T0> extends BaseTLink<T1, T0>
    implements StreamingTLink<T1, T0> {

  SBaseTLink(StreamingTSetEnvironment env, String n, int sourceP, int targetP) {
    super(env, n, sourceP, targetP);
  }

  @Override
  public StreamingTSetEnvironment getTSetEnv() {
    return (StreamingTSetEnvironment) super.getTSetEnv();
  }

  public <P> SComputeTSet<P, T1> compute(String n, ComputeFunc<P, T1> computeFunction) {
    SComputeTSet<P, T1> set;
    if (n != null && !n.isEmpty()) {
      set = new SComputeTSet<>(getTSetEnv(), n, new ComputeOp<>(computeFunction),
          getTargetParallelism());
    } else {
      set = new SComputeTSet<>(getTSetEnv(), new ComputeOp<>(computeFunction),
          getTargetParallelism());
    }
    addChildToGraph(set);

    return set;
  }

  public <P> SComputeTSet<P, T1> compute(String n, ComputeCollectorFunc<P, T1> computeFunction) {
    SComputeTSet<P, T1> set;
    if (n != null && !n.isEmpty()) {
      set = new SComputeTSet<>(getTSetEnv(), n, new ComputeCollectorOp<>(computeFunction),
          getTargetParallelism());
    } else {
      set = new SComputeTSet<>(getTSetEnv(), new ComputeCollectorOp<>(computeFunction),
          getTargetParallelism());
    }
    addChildToGraph(set);

    return set;
  }

  @Override
  public <P> SComputeTSet<P, T1> compute(ComputeFunc<P, T1> computeFunction) {
    return compute(null, computeFunction);
  }

  @Override
  public <P> SComputeTSet<P, T1> compute(ComputeCollectorFunc<P, T1> computeFunction) {
    return compute(null, computeFunction);
  }


  @Override
  public void sink(SinkFunc<T1> sinkFunction) {
    SSinkTSet<T1> sinkTSet = new SSinkTSet<>(getTSetEnv(), new SinkOp<>(sinkFunction),
        getTargetParallelism());
    addChildToGraph(sinkTSet);
  }
}
