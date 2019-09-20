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

package edu.iu.dsc.tws.examples.tset.batch;

import java.util.HashMap;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.rsched.core.ResourceAllocator;
import edu.iu.dsc.tws.tset.env.BatchTSetEnvironment;
import edu.iu.dsc.tws.tset.sets.batch.CachedTSet;
import edu.iu.dsc.tws.tset.sets.batch.SourceTSet;


public class CacheIterationsExample extends BatchTsetExample {
  private static final Logger LOG = Logger.getLogger(CacheIterationsExample.class.getName());
  private static final long serialVersionUID = -2753072757838198105L;

  @Override
  public void execute(BatchTSetEnvironment env) {
    SourceTSet<Integer> src = dummySource(env, COUNT, PARALLELISM);

    SourceTSet<Integer> src1 = dummySourceOther(env, COUNT, PARALLELISM);


    boolean isIter = false;

    // test direct().cache() which has IterLink semantics
    CachedTSet<Integer> cache = src.direct().cache(isIter);
    CachedTSet<Integer> cache1 = src1.direct().cache(isIter);


//    ComputeTSet<Integer, Iterator<Integer>> map = cache.direct().map(k -> k + 1);

    for (int i = 0; i < 10; i++) {
      cache = cache.direct().map(k -> k + 1).cache(isIter);
    }

    cache.direct().forEach(l -> LOG.info(l.toString()));

  }


  public static void main(String[] args) {
    Config config = ResourceAllocator.loadConfig(new HashMap<>());

    JobConfig jobConfig = new JobConfig();
    BatchTsetExample.submitJob(config, PARALLELISM, jobConfig,
        CacheIterationsExample.class.getName());
  }
}
