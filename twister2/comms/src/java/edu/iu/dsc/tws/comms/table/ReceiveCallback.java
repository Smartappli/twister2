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
package edu.iu.dsc.tws.comms.table;

import java.nio.ByteBuffer;

import edu.iu.dsc.tws.comms.table.channel.ChannelBuffer;

public interface ReceiveCallback {
  void onReceive(int source, ChannelBuffer buffer, int length);

  void onReceiveHeader(int source, boolean finished, int[] header, int length);

  boolean onSendComplete(int target, ByteBuffer buffer, int length);
}
