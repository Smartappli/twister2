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

package edu.iu.dsc.tws.tset.links.batch;

import edu.iu.dsc.tws.api.comms.CommunicationContext;
import edu.iu.dsc.tws.api.compute.OperationNames;
import edu.iu.dsc.tws.api.compute.graph.Edge;
import edu.iu.dsc.tws.api.tset.schema.Schema;
import edu.iu.dsc.tws.tset.env.BatchEnvironment;
import edu.iu.dsc.tws.tset.links.TLinkUtils;

/**
 * Represent a data set created by an all gather operation
 *
 * @param <T> type of data
 */
public class AllGatherTLink<T> extends BatchGatherLink<T> {
  private boolean useDisk = false;

  private AllGatherTLink() {
    //non arg constructor for kryo
  }

  public AllGatherTLink(BatchEnvironment tSetEnv, int sourceParallelism, Schema schema) {
    super(tSetEnv, "allgather", sourceParallelism, schema);
  }

  @Override
  public Edge getEdge() {
    Edge e = new Edge(getId(), OperationNames.ALLGATHER, this.getSchema().getDataType());
    e.addProperty(CommunicationContext.USE_DISK, this.useDisk);
    TLinkUtils.generateCommsSchema(getSchema(), e);
    return e;
  }

  @Override
  public AllGatherTLink<T> setName(String n) {
    rename(n);
    return this;
  }

  public AllGatherTLink<T> useDisk() {
    this.useDisk = true;
    return this;
  }
}
