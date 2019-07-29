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
package edu.iu.dsc.tws.comms.dfw.io.join;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.iu.dsc.tws.api.comms.BulkReceiver;
import edu.iu.dsc.tws.api.comms.CommunicationContext;
import edu.iu.dsc.tws.api.comms.DataFlowOperation;
import edu.iu.dsc.tws.api.comms.messaging.MessageReceiver;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.comms.dfw.io.partition.DPartitionBatchFinalReceiver;
import edu.iu.dsc.tws.comms.shuffle.RestorableIterator;
import edu.iu.dsc.tws.comms.utils.JoinUtils;
import edu.iu.dsc.tws.comms.utils.KeyComparatorWrapper;

public class DJoinBatchFinalReceiver2 implements MessageReceiver {

  private final KeyComparatorWrapper comparator;
  /**
   * The left receiver
   */
  private DPartitionBatchFinalReceiver leftReceiver;

  /**
   * The right receiver
   */
  private DPartitionBatchFinalReceiver rightReceiver;

  /**
   * The user provided receiver
   */
  private BulkReceiver bulkReceiver;
  private CommunicationContext.JoinType joinType;

  /**
   * The iterators returned by left
   */
  private Map<Integer, Iterator<Object>> leftValues;

  /**
   * The iterators return by right
   */
  private Map<Integer, Iterator<Object>> rightValues;

  public DJoinBatchFinalReceiver2(BulkReceiver bulkReceiver,
                                  List<String> shuffleDirs,
                                  Comparator<Object> com,
                                  CommunicationContext.JoinType joinType) {
    this.bulkReceiver = bulkReceiver;
    this.joinType = joinType;
    this.leftReceiver = new DPartitionBatchFinalReceiver(new InnerBulkReceiver(0),
        true, shuffleDirs, com, false);
    this.rightReceiver = new DPartitionBatchFinalReceiver(new InnerBulkReceiver(1),
        true, shuffleDirs, com, false);
    this.leftValues = new HashMap<>();
    this.rightValues = new HashMap<>();
    this.comparator = new KeyComparatorWrapper(com);
  }

  @Override
  public void init(Config cfg, DataFlowOperation op, Map<Integer, List<Integer>> expectedIds) {
    leftReceiver.init(cfg, op, expectedIds);
    rightReceiver.init(cfg, op, expectedIds);
    bulkReceiver.init(cfg, expectedIds.keySet());
  }

  @Override
  public boolean onMessage(int source, int path, int target, int flags, Object object) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void onFinish(int source) {
    leftReceiver.onFinish(source);
    rightReceiver.onFinish(source);
  }

  @Override
  public void close() {
    leftReceiver.clean();
    rightReceiver.clean();
  }

  @Override
  public void clean() {
    leftReceiver.clean();
    rightReceiver.clean();

    // clean the maps
    leftValues.clear();
    rightValues.clear();
  }

  @Override
  public void onSyncEvent(int target, byte[] value) {
    bulkReceiver.sync(target, value);
  }

  @Override
  public boolean onMessage(int source, int path, int target, int flags, int tag, Object object) {
    if (tag == 0) {
      return leftReceiver.onMessage(source, path, target, flags, object);
    } else {
      return rightReceiver.onMessage(source, path, target, flags, object);
    }
  }

  @Override
  public boolean progress() {
    return leftReceiver.progress() | rightReceiver.progress();
  }

  private class InnerBulkReceiver implements BulkReceiver {
    private int tag;

    InnerBulkReceiver(int tag) {
      this.tag = tag;
    }

    @Override
    public void init(Config cfg, Set<Integer> targets) {
    }

    @Override
    public boolean receive(int target, Iterator<Object> it) {
      if (tag == 0) {
        leftValues.put(target, it);

        if (rightValues.containsKey(target)) {
          bulkReceiver.receive(target, JoinUtils.join(
              (RestorableIterator) it,
              (RestorableIterator) rightValues.get(target),
              comparator, joinType)
          );
        }
      } else {
        rightValues.put(target, it);

        if (leftValues.containsKey(target)) {
          bulkReceiver.receive(target, JoinUtils.join(
              (RestorableIterator) leftValues.get(target),
              (RestorableIterator) it,
              comparator, joinType)
          );
        }
      }
      return true;
    }

    @Override
    public boolean sync(int target, byte[] message) {
      if (rightValues.containsKey(target) && leftValues.containsKey(target)) {
        return bulkReceiver.sync(target, message);
      }
      return false;
    }
  }
}