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
package edu.iu.dsc.tws.examples.ml.svm.integration.test;


import edu.iu.dsc.tws.api.compute.modifiers.Collector;
import edu.iu.dsc.tws.api.dataset.DataPartition;

public interface ICollector<T> extends Collector {
  /**
   * get the collected valued
   *
   * @return get the default output
   */
  DataPartition<T> get();

  /**
   * get the collected valued
   *
   * @param name name of the value to collect
   * @return the partition of the data
   */
  default DataPartition<T> get(String name) {
    return null;
  }
}
