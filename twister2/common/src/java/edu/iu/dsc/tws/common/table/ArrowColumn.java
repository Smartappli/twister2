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
package edu.iu.dsc.tws.common.table;

import org.apache.arrow.vector.FieldVector;

/**
 * We are going to represent and arrow vector as a column to facilitate
 * easy access and building of vectors, this class is only for internal use
 * and should not be exposed to the users
 * @param <T>
 */
public interface ArrowColumn<T> {
  void addValue(T value);

  T get(int index);

  FieldVector getVector();

  long currentSize();
}
