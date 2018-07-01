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
package edu.iu.dsc.tws.rsched.schedulers.mesos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Inet4Address;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.config.ConfigLoader;
import edu.iu.dsc.tws.common.discovery.WorkerNetworkInfo;
import edu.iu.dsc.tws.proto.system.job.JobAPI;
import edu.iu.dsc.tws.rsched.bootstrap.ZKContext;
import edu.iu.dsc.tws.rsched.utils.JobUtils;
import edu.iu.dsc.tws.rsched.utils.ProcessUtils;


public class MesosDockerWorker {

  public static final Logger LOG = Logger.getLogger(MesosDockerWorker.class.getName());
  private Config config;
  private String jobName;


  public static void main(String[] args) throws Exception {


    //gets the docker home directory
    String homeDir = System.getenv("HOME");
    int workerId = Integer.parseInt(System.getenv("WORKER_ID"));
    String jobName = System.getenv("JOB_NAME");

    int id = workerId;
    MesosDockerWorker worker = new MesosDockerWorker();

    String twister2Home = Paths.get("").toAbsolutePath().toString();
    String configDir = "twister2-job/mesos/";
    worker.config = ConfigLoader.loadConfig(twister2Home, configDir);
    worker.jobName = jobName;

    MesosWorkerLogger logger = new MesosWorkerLogger(worker.config,
        "/persistent-volume/logs", "worker" + workerId);
    logger.initLogging();


    /*
    String containerClass = SchedulerContext.containerClass(worker.config);
    IWorker container;
    try {
      Object object = ReflectionUtils.newInstance(containerClass);
      container = (IWorker) object;
      LOG.info("loaded container class: " + containerClass);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      LOG.log(Level.SEVERE, String.format("failed to load the container class %s",
          containerClass), e);
      throw new RuntimeException(e);

    }
    */


    MesosWorkerController workerController = null;
    List<WorkerNetworkInfo> workerNetworkInfoList = new ArrayList<>();
    try {
      JobAPI.Job job = JobUtils.readJobFile(null, "twister2-job/"
          + jobName + ".job");
      workerController = new MesosWorkerController(worker.config, job,
          Inet4Address.getLocalHost().getHostAddress(), 22, id);
      LOG.info("Initializing with zookeeper");
      workerController.initializeWithZooKeeper();
      LOG.info("Waiting for all workers to join");
      workerNetworkInfoList = workerController.waitForAllWorkersToJoin(
          ZKContext.maxWaitTimeForAllWorkersToJoin(worker.config));
      LOG.info("Everyone has joined");
      //container.init(worker.config, id, null, workerController, null);

    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("Worker id " + id);
    StringBuilder outputBuilder = new StringBuilder();
    int workerCount = workerController.getNumberOfWorkers();
    System.out.println("worker count " + workerCount);


    //docker master has the id equals to zero
    if (id == 0) {

      File hostFile = new File(homeDir + "/.ssh/config");

      hostFile.getParentFile().mkdirs();

      Writer writer = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(homeDir + "/.ssh/config", true)));

      writer.write("Host *\n\tStrictHostKeyChecking no\n\tUserKnownHostsFile /dev/null\n"
          + "\tIdentityFile ~/.ssh/id_rsa\n");


      String hosts = "";

      for (int i = 0; i < workerCount; i++) {

        writer.write("Host w" + workerNetworkInfoList.get(i).getWorkerID() + "\n"
            + "\tHostname " + workerNetworkInfoList.get(i).getWorkerIP().getHostAddress() + "\n"
            + "\tPort " + workerNetworkInfoList.get(i).getWorkerPort() + "\n");

        System.out.println("Host w" + workerNetworkInfoList.get(i).getWorkerID() + "\n"
            + "\tHostname " + workerNetworkInfoList.get(i).getWorkerIP().getHostAddress() + "\n"
            + "\tPort " + workerNetworkInfoList.get(i).getWorkerPort() + "\n");

        hosts += "w" + workerNetworkInfoList.get(i).getWorkerID() + ",";
      }


      writer.close();

      //remove final comma
      hosts = hosts.substring(0, hosts.lastIndexOf(','));


      System.out.println("Before mpirun");
      System.out.println("hosts " + hosts);
      String[] command = {"mpirun", "-allow-run-as-root", "-np",
          workerController.getNumberOfWorkers() + "",
          "--host", hosts, "java", "-cp",
          "twister2-job/libexamples-java.jar",
          "edu.iu.dsc.tws.examples.basic.BasicMpiJob", ">mpioutfile"};

      System.out.println("command:" + String.join(" ", command));

      ProcessUtils.runSyncProcess(false, command, outputBuilder,
          new File("."), true);
      workerController.close();
      System.out.println("Finished");
    }


  }


}
