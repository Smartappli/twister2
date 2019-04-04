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
package edu.iu.dsc.tws.examples.tset;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.tset.BaseSink;
import edu.iu.dsc.tws.api.tset.TwisterBatchContext;
import edu.iu.dsc.tws.api.tset.fn.OneToOnePartitioner;
import edu.iu.dsc.tws.api.tset.link.PartitionTLink;
import edu.iu.dsc.tws.api.tset.sets.BatchSourceTSet;
import edu.iu.dsc.tws.examples.verification.VerificationException;
import edu.iu.dsc.tws.executor.core.OperationNames;

public class TSetPartitionExample extends BaseTSetBatchWorker {
  private static final Logger LOG = Logger.getLogger(TSetPartitionExample.class.getName());

  @Override
  public void execute(TwisterBatchContext tc) {
    super.execute(tc);

    // set the parallelism of source to task stage 0
    List<Integer> taskStages = jobParameters.getTaskStages();
    int sourceParallelism = taskStages.get(0);
    int sinkParallelism = taskStages.get(1);
    BatchSourceTSet<int[]> source = tc.createSource(new TestBaseSource(),
        sourceParallelism).setName("Source");
    PartitionTLink<int[]> partition = source.partition(new OneToOnePartitioner<>());

    partition.sink(new BaseSink<int[]>() {
      @Override
      public boolean add(int[] value) {
        LOG.info("Task Id : " + context.getIndex() + " Results " + Arrays.toString(value));
        experimentData.setOutput(value);
        try {
          verify(OperationNames.PARTITION);
        } catch (VerificationException e) {
          LOG.info("Exception Message : " + e.getMessage());
        }
        return true;
      }

      @Override
      public void prepare() {
      }
    }, sinkParallelism);
  }

}
