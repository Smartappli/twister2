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
package edu.iu.dsc.tws.deeplearning.process;

import mpi.Info;
import mpi.Intercomm;
import mpi.MPI;
import mpi.MPIException;

public class ProcessManager implements IManager {
  @Override
  public Intercomm spawn(String executableName, String[] spawnArgs, int maxProcs, Info info,
                         int i1, int[] errorCode) throws MPIException {
    Intercomm intercomm = MPI.COMM_WORLD.spawn(executableName, spawnArgs, maxProcs,
        MPI.INFO_NULL, 0, errorCode);
    return intercomm;
  }
}

