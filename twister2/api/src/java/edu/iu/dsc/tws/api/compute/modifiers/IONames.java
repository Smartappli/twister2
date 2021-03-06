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
package edu.iu.dsc.tws.api.compute.modifiers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public final class IONames extends HashSet<String> {

  private IONames(String... variables) {
    this.addAll(Arrays.asList(variables));
  }

  public static IONames declare(String... variables) {
    return new IONames(variables);
  }

  public static IONames declare(Collection<String> variables) {
    IONames strings = new IONames();
    strings.addAll(variables);
    return strings;
  }
}
