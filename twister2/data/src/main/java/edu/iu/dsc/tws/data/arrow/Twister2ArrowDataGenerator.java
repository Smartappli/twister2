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
package edu.iu.dsc.tws.data.arrow;

import java.util.List;
import java.util.logging.Logger;

import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.pojo.Field;

public class Twister2ArrowDataGenerator {

  private static final Logger LOG = Logger.getLogger(Twister2ArrowDataGenerator.class.getName());

  private List<Object> dataList;
  private FieldVector fieldVector;

  private boolean useNullValues;

  private ArrowDataGenerator arrowDataGenerator;

  public Twister2ArrowDataGenerator(List<Object> datalist, FieldVector fieldvector) {
    this.dataList = datalist;
    this.fieldVector = fieldvector;
  }

  public Twister2ArrowDataGenerator(VectorSchemaRoot root, List<Object> datalist) {
    this.dataList = datalist;
    for (Field field : root.getSchema().getFields()) {
      FieldVector vector = root.getVector(field.getName());
      if (vector.getMinorType().equals(Types.MinorType.INT)) {
        arrowDataGenerator = new IntVectorGenerator();
      } else if (vector.getMinorType().equals(Types.MinorType.BIGINT)) {
        arrowDataGenerator = new BigIntVectorGenerator();
      } else if (vector.getMinorType().equals(Types.MinorType.FLOAT4)) {
        arrowDataGenerator = new FloatVectorGenerator();
      }
    }
  }

  public <T> void vectorGeneration(T intVector, int from, int items) {
    arrowDataGenerator.vectorGeneration(intVector, from, items);
  }

  public <T> void intVectorGeneration(T intVector, int from, int items) {
    arrowDataGenerator.intVectorGeneration(intVector, from, items);
  }

  public <T> void bigIntVectorGeneration(T bigIntVector, int from, int items) {
    arrowDataGenerator.bigIntVectorGeneration(bigIntVector, from, items);
  }

  public <T> void floatVectorGeneration(T float4Vector, int from, int items) {
    arrowDataGenerator.floatVectorGeneration(float4Vector, from, items);
  }

  private interface ArrowDataGenerator {
    <T> void vectorGeneration(T vector, int from, int items);
    <T> void intVectorGeneration(T intVector, int from, int items);
    <T> void bigIntVectorGeneration(T bigIntVector, int from, int items);
    <T> void floatVectorGeneration(T float4Vector, int from, int items);
  }

  private class IntVectorGenerator implements ArrowDataGenerator {

    @Override
    public <T> void vectorGeneration(T intVector1, int from, int items) {
      IntVector intVector = (IntVector) intVector1;
      intVector.setInitialCapacity(items);
      intVector.allocateNew();
      for (int i = 0; i < items; i++) {
        intVector.setSafe(i, isSet(), (int) dataList.get(from + i));
      }
      intVector.setValueCount(items);
    }

    @Override
    public <T> void intVectorGeneration(T intVector, int from, int items) {
    }

    @Override
    public <T> void bigIntVectorGeneration(T bigIntVector, int from, int items) {
    }

    @Override
    public <T> void floatVectorGeneration(T float4Vector, int from, int items) {
    }
  }

  private class BigIntVectorGenerator implements ArrowDataGenerator {

    @Override
    public <T> void vectorGeneration(T bigIntVector1, int from, int items) {
      BigIntVector bigIntVector = (BigIntVector) bigIntVector1;
      bigIntVector.setInitialCapacity(items);
      bigIntVector.allocateNew();
      for (int i = 0; i < items; i++) {
        Long l = new Long(dataList.get(from + i).toString());
        bigIntVector.setSafe(i, isSet(), l);
      }
      bigIntVector.setValueCount(items);
    }

    @Override
    public <T> void intVectorGeneration(T intVector, int from, int items) {
    }

    @Override
    public <T> void bigIntVectorGeneration(T bigIntVector, int from, int items) {
    }

    @Override
    public <T> void floatVectorGeneration(T float4Vector, int from, int items) {
    }
  }

  private class FloatVectorGenerator implements ArrowDataGenerator {

    @Override
    public <T> void vectorGeneration(T floatVector1, int from, int items) {
      Float4Vector floatVector = (Float4Vector) floatVector1;
      floatVector.setInitialCapacity(items);
      floatVector.allocateNew();
      for (int i = 0; i < items; i++) {
        floatVector.setSafe(i, isSet(), (float) dataList.get(from + i));
      }
      floatVector.setValueCount(items);
    }

    @Override
    public <T> void intVectorGeneration(T intVector, int from, int items) {
    }

    @Override
    public <T> void bigIntVectorGeneration(T bigIntVector, int from, int items) {
    }

    @Override
    public <T> void floatVectorGeneration(T float4Vector, int from, int items) {
    }
  }

  private int isSet() {
    if (useNullValues) {
      return 0;
    }
    return 1;
  }
}