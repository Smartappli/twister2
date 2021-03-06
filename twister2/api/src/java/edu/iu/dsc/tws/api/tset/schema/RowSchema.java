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
package edu.iu.dsc.tws.api.tset.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;

import edu.iu.dsc.tws.api.comms.messaging.types.MessageType;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.exceptions.Twister2RuntimeException;
import edu.iu.dsc.tws.common.table.TField;
import edu.iu.dsc.tws.common.table.arrow.ArrowTypes;

public class RowSchema implements Schema {
  private List<TField> types;

  public static RowSchema make(TField... t) {
    return new RowSchema(new ArrayList<>(Arrays.<TField>asList(t)));
  }

  public RowSchema(List<TField> types) {
    this.types = types;
  }

  public MessageType get(int column) {
    return types.get(column).getType();
  }

  public int getNumberOfColumns() {
    return types.size();
  }

  public List<TField> getFields() {
    return new ArrayList<>(types);
  }

  public org.apache.arrow.vector.types.pojo.Schema toArrowSchema() {
    List<Field> fields = new ArrayList<>();
    for (TField f : types) {
      Field field;
      if (f.getType().equals(MessageTypes.INTEGER)) {
        field = new Field(f.getName(),
                new FieldType(false,
                    new ArrowType.Int(32, true), null), null);
      } else if (f.getType().equals(MessageTypes.LONG)) {
        field = new Field(f.getName(),
            new FieldType(false,
                new ArrowType.Int(64, true), null), null);
      } else if (f.getType().equals(MessageTypes.SHORT)) {
        field = new Field(f.getName(),
            new FieldType(false,
                new ArrowType.Int(16, true), null), null);
      } else if (f.getType().equals(MessageTypes.FLOAT)) {
        field = new Field(f.getName(),
            new FieldType(false,
                new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE), null), null);
      } else if (f.getType().equals(MessageTypes.DOUBLE)) {
        field = new Field(f.getName(),
            new FieldType(false,
                new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE), null), null);
      } else if (f.getType().equals(MessageTypes.STRING)) {
        field = new Field(f.getName(),
            new FieldType(false,
                new ArrowType.Binary(), null), null);
      } else if (f.getType().equals(MessageTypes.BYTE)) {
        field = new Field(f.getName(),
            new FieldType(false,
                new ArrowType.Binary(), null), null);
      } else {
        throw new Twister2RuntimeException("Un-known type");
      }
      fields.add(field);
    }
    return new org.apache.arrow.vector.types.pojo.Schema(fields);
  }

  public static RowSchema fromArrow(org.apache.arrow.vector.types.pojo.Schema schema) {
    List<Field> fields = schema.getFields();
    List<TField> tFields = new ArrayList<>();
    for (Field f : fields) {
      TField tField;
      if (f.getFieldType().equals(ArrowTypes.INT_FIELD_TYPE)) {
        tField = new TField(f.getName(), MessageTypes.INTEGER);
      } else if (f.getFieldType().equals(ArrowTypes.LONG_FIELD_TYPE)) {
        tField = new TField(f.getName(), MessageTypes.LONG);
      } else if (f.getFieldType().equals(ArrowTypes.SHORT_FIELD_TYPE)) {
        tField = new TField(f.getName(), MessageTypes.SHORT);
      } else if (f.getFieldType().equals(ArrowTypes.FLOAT_FIELD_TYPE)) {
        tField = new TField(f.getName(), MessageTypes.FLOAT);
      } else if (f.getFieldType().equals(ArrowTypes.DOUBLE_FIELD_TYPE)) {
        tField = new TField(f.getName(), MessageTypes.DOUBLE);
      } else if (f.getFieldType().equals(ArrowTypes.STRING_FIELD_TYPE)) {
        tField = new TField(f.getName(), MessageTypes.STRING);
      } else if (f.getFieldType().equals(ArrowTypes.BINARY_FILED_TYPE)) {
        tField = new TField(f.getName(), MessageTypes.BYTE);
      } else {
        throw new Twister2RuntimeException("Unknown type");
      }
      tFields.add(tField);
    }
    return new RowSchema(tFields);
  }

  @Override
  public MessageType getDataType() {
    return MessageTypes.ARROW_TABLE;
  }

  @Override
  public int getDataSize() {
    return 0;
  }

  @Override
  public int getTotalSize() {
    return 0;
  }

  @Override
  public boolean isLengthsSpecified() {
    return false;
  }
}
