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
package edu.iu.dsc.tws.common.table.arrow;

import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.util.Text;

import edu.iu.dsc.tws.common.table.ArrowColumn;

public class StringColumn implements ArrowColumn<Text> {
  private VarCharVector vector;

  private int currentIndex;

  public StringColumn(VarCharVector stringVector) {
    this.vector = stringVector;
    this.currentIndex = 0;
  }

  @Override
  public void addValue(Text value) {
    vector.setSafe(currentIndex, value);
    currentIndex++;
    vector.setValueCount(currentIndex);
  }

  @Override
  public FieldVector getVector() {
    return vector;
  }

  @Override
  public Text get(int index) {
    return new Text(vector.get(index));
  }

  @Override
  public long currentSize() {
    return vector.sizeOfValueBuffer();
  }
}
