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
package edu.iu.dsc.tws.api.tset.ops;

import edu.iu.dsc.tws.api.task.IMessage;
import edu.iu.dsc.tws.api.tset.fn.ComputeFunction;

/**
 * Performs the compute function on the value received for the imessage and write it to edges
 */
public class ComputeOp<O, I> extends BaseComputeOp<O, I> {

  private ComputeFunction<O, I> computeFunction;

  public ComputeOp(ComputeFunction<O, I> computeFunction) {
    this.computeFunction = computeFunction;
  }

  @Override
  public boolean execute(IMessage<I> content) {
    O output = computeFunction.compute(content.getContent());
    writeToEdges(output);
    writeEndToEdges();
    return true;
  }
}
