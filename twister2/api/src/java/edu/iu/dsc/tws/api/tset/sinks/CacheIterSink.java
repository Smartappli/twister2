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

package edu.iu.dsc.tws.api.tset.sinks;

import java.util.Iterator;

import edu.iu.dsc.tws.api.dataset.DataPartition;
import edu.iu.dsc.tws.api.tset.TSetContext;
import edu.iu.dsc.tws.api.tset.fn.BaseSinkFunc;
import edu.iu.dsc.tws.dataset.partition.CollectionPartition;

public class CacheIterSink<T> extends BaseSinkFunc<Iterator<T>> {

  private CollectionPartition<T> partition;

  @Override
  public void prepare(TSetContext ctx) {
    super.prepare(ctx);

    this.partition = new CollectionPartition<>(getTSetContext().getIndex());
  }

  @Override
  public boolean add(Iterator<T> value) {
    while (value.hasNext()) {
      partition.add(value.next());
    }
    return true;
  }

  @Override
  public DataPartition<T> get() {
    return partition;
  }
}
