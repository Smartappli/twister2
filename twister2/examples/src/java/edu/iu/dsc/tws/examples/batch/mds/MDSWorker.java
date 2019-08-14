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
package edu.iu.dsc.tws.examples.batch.mds;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.Twister2Job;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.config.Context;
import edu.iu.dsc.tws.api.dataset.DataObject;
import edu.iu.dsc.tws.api.dataset.DataPartition;
import edu.iu.dsc.tws.api.resource.IPersistentVolume;
import edu.iu.dsc.tws.api.resource.IVolatileVolume;
import edu.iu.dsc.tws.api.resource.IWorker;
import edu.iu.dsc.tws.api.resource.IWorkerController;
import edu.iu.dsc.tws.api.scheduler.SchedulerContext;
import edu.iu.dsc.tws.api.task.IMessage;
import edu.iu.dsc.tws.api.task.executor.ExecutionPlan;
import edu.iu.dsc.tws.api.task.graph.DataFlowTaskGraph;
import edu.iu.dsc.tws.api.task.graph.OperationMode;
import edu.iu.dsc.tws.api.task.modifiers.Collector;
import edu.iu.dsc.tws.api.task.modifiers.Receptor;
import edu.iu.dsc.tws.api.task.nodes.BaseSink;
import edu.iu.dsc.tws.api.task.nodes.BaseSource;
import edu.iu.dsc.tws.data.utils.DataObjectConstants;
import edu.iu.dsc.tws.examples.Utils;
import edu.iu.dsc.tws.rsched.core.ResourceAllocator;
import edu.iu.dsc.tws.rsched.job.Twister2Submitter;
import edu.iu.dsc.tws.task.ComputeEnvironment;
import edu.iu.dsc.tws.task.impl.ComputeConnection;
import edu.iu.dsc.tws.task.impl.ComputeGraphBuilder;
import edu.iu.dsc.tws.task.impl.TaskExecutor;

public class MDSWorker implements IWorker {

  private static final Logger LOG = Logger.getLogger(MDSWorker.class.getName());

  @Override
  public void execute(Config config, int workerID, IWorkerController workerController,
                      IPersistentVolume persistentVolume, IVolatileVolume volatileVolume) {
    ComputeEnvironment cEnv = ComputeEnvironment.init(config, workerID,
        workerController, persistentVolume, volatileVolume);
    TaskExecutor taskExecutor = cEnv.getTaskExecutor();
    MDSWorkerParameters mdsWorkerParameters = MDSWorkerParameters.build(config);

    int parallel = mdsWorkerParameters.getParallelismValue();
    int datasize = mdsWorkerParameters.getDsize();
    int matrixColumLength = mdsWorkerParameters.getDimension();

    String dataInput = mdsWorkerParameters.getDataInput();
    String configFile = mdsWorkerParameters.getConfigFile();
    String directory = mdsWorkerParameters.getDatapointDirectory();
    String byteType = mdsWorkerParameters.getByteType();

    LOG.info("Data Points to be generated or read" + dataInput + "\t" + directory
        + "\t" + byteType + "\t" + configFile);

    /* Generate the Matrix for the MDS **/
    if (Context.TWISTER2_DATA_INPUT.equalsIgnoreCase(dataInput)) {
      MatrixGenerator matrixGen = new MatrixGenerator(config);
      matrixGen.generate(datasize, matrixColumLength, directory, byteType);
    }

    /** Task Graph to partition the generated matrix for MDS **/
    MDSDataObjectSource mdsDataObjectSource = new MDSDataObjectSource(Context.TWISTER2_DIRECT_EDGE,
        directory, datasize);
    MDSDataObjectSink mdsDataObjectSink = new MDSDataObjectSink(matrixColumLength);

    ComputeGraphBuilder mdsDataProcessingGraphBuilder = ComputeGraphBuilder.newBuilder(config);
    mdsDataProcessingGraphBuilder.setTaskGraphName("MDSDataProcessing");
    mdsDataProcessingGraphBuilder.addSource("dataobjectsource", mdsDataObjectSource, parallel);

    ComputeConnection dataObjectComputeConnection = mdsDataProcessingGraphBuilder.addSink(
        "dataobjectsink", mdsDataObjectSink, parallel);
    dataObjectComputeConnection.direct("dataobjectsource")
        .viaEdge(Context.TWISTER2_DIRECT_EDGE)
        .withDataType(MessageTypes.OBJECT);
    mdsDataProcessingGraphBuilder.setMode(OperationMode.BATCH);

    DataFlowTaskGraph dataObjectTaskGraph = mdsDataProcessingGraphBuilder.build();
    //Get the execution plan for the first task graph
    ExecutionPlan plan = taskExecutor.plan(dataObjectTaskGraph);

    //Actual execution for the first taskgraph
    taskExecutor.execute(dataObjectTaskGraph, plan);

    //Retrieve the output of the first task graph
    DataObject<Object> dataPointsObject = taskExecutor.getOutput(
        dataObjectTaskGraph, plan, "dataobjectsink");

    /** Task Graph to run the MDS **/
    MDSSourceTask generatorTask = new MDSSourceTask();
    MDSReceiverTask receiverTask = new MDSReceiverTask();

    ComputeGraphBuilder mdsComputeProcessingGraphBuilder = ComputeGraphBuilder.newBuilder(config);
    mdsComputeProcessingGraphBuilder.setTaskGraphName("MDSCompute");
    mdsComputeProcessingGraphBuilder.addSource("generator", generatorTask, parallel);
    ComputeConnection computeConnection = mdsComputeProcessingGraphBuilder.addSink("receiver",
        receiverTask, parallel);
    computeConnection.direct("generator")
        .viaEdge(Context.TWISTER2_DIRECT_EDGE)
        .withDataType(MessageTypes.OBJECT);
    mdsComputeProcessingGraphBuilder.setMode(OperationMode.BATCH);

    DataFlowTaskGraph mdsTaskGraph = mdsComputeProcessingGraphBuilder.build();

    //Get the execution plan for the first task graph
    ExecutionPlan executionPlan = taskExecutor.plan(mdsTaskGraph);

    //Actual execution for the first taskgraph
    taskExecutor.addInput(
        mdsTaskGraph, executionPlan, "generator", "points", dataPointsObject);
    taskExecutor.execute(mdsTaskGraph, executionPlan);
  }

  private static class MDSSourceTask extends BaseSource implements Receptor {

    private static final long serialVersionUID = -254264120110286748L;

    private DataObject<?> dataPointsObject = null;

    private short[] datapoints = null;

    @Override
    public void execute() {
      DataPartition<?> dataPartition = dataPointsObject.getPartition(context.taskIndex());
      datapoints = (short[]) dataPartition.getConsumer().next();
      context.writeEnd(Context.TWISTER2_DIRECT_EDGE, "MDS Execution");
    }

    @Override
    public void add(String name, DataObject<?> data) {
      LOG.log(Level.INFO, "Received input: " + name);
      if ("points".equals(name)) {
        this.dataPointsObject = data;
      }
    }
  }

  private static class MDSReceiverTask extends BaseSink implements Collector {
    private static final long serialVersionUID = -254264120110286748L;

    @Override
    public boolean execute(IMessage content) {
      LOG.info("Received message:" + content.getContent().toString());
      return false;
    }

    @Override
    public DataPartition<?> get() {
      return null;
    }
  }

  public static void main(String[] args) throws ParseException {
    // first load the configurations from command line and config files
    Config config = ResourceAllocator.loadConfig(new HashMap<>());

    // build JobConfig
    HashMap<String, Object> configurations = new HashMap<>();
    configurations.put(SchedulerContext.THREADS_PER_WORKER, 1);

    Options options = new Options();
    options.addOption(DataObjectConstants.WORKERS, true, "Workers");
    options.addOption(DataObjectConstants.PARALLELISM_VALUE, true, "parallelism");
    options.addOption(DataObjectConstants.DSIZE, true, "Size of the matrix rows");
    options.addOption(DataObjectConstants.DIMENSIONS, true, "dimension of the matrix");

    options.addOption(DataObjectConstants.BYTE_TYPE, true, "bytetype");
    options.addOption(DataObjectConstants.DATA_INPUT, true, "datainput");
    options.addOption(Utils.createOption(DataObjectConstants.DINPUT_DIRECTORY,
        true, "Matrix Input Creation directory", true));
    options.addOption(Utils.createOption(DataObjectConstants.FILE_SYSTEM,
        true, "file system", true));
    options.addOption(Utils.createOption(DataObjectConstants.CONFIG_FILE,
        true, "config File", true));

    @SuppressWarnings("deprecation")
    CommandLineParser commandLineParser = new DefaultParser();
    CommandLine cmd = commandLineParser.parse(options, args);

    int workers = Integer.parseInt(cmd.getOptionValue(DataObjectConstants.WORKERS));
    int dsize = Integer.parseInt(cmd.getOptionValue(DataObjectConstants.DSIZE));

    int dimension = Integer.parseInt(cmd.getOptionValue(DataObjectConstants.DIMENSIONS));
    int parallelismValue = Integer.parseInt(cmd.getOptionValue(
        DataObjectConstants.PARALLELISM_VALUE));

    String byteType = cmd.getOptionValue(DataObjectConstants.BYTE_TYPE);
    String dataDirectory = cmd.getOptionValue(DataObjectConstants.DINPUT_DIRECTORY);
    String fileSystem = cmd.getOptionValue(DataObjectConstants.FILE_SYSTEM);
    String configFile = cmd.getOptionValue(DataObjectConstants.CONFIG_FILE);
    String dataInput = cmd.getOptionValue(DataObjectConstants.DATA_INPUT);

    // build JobConfig
    JobConfig jobConfig = new JobConfig();
    jobConfig.put(DataObjectConstants.WORKERS, Integer.toString(workers));
    jobConfig.put(DataObjectConstants.PARALLELISM_VALUE, Integer.toString(parallelismValue));

    jobConfig.put(DataObjectConstants.DIMENSIONS, Integer.toString(dimension));
    jobConfig.put(DataObjectConstants.DSIZE, Integer.toString(dsize));

    jobConfig.put(DataObjectConstants.BYTE_TYPE, byteType);
    jobConfig.put(DataObjectConstants.DINPUT_DIRECTORY, dataDirectory);
    jobConfig.put(DataObjectConstants.FILE_SYSTEM, fileSystem);
    jobConfig.put(DataObjectConstants.CONFIG_FILE, configFile);
    jobConfig.put(DataObjectConstants.DATA_INPUT, dataInput);

    // build JobConfig
    Twister2Job.Twister2JobBuilder jobBuilder = Twister2Job.newBuilder();
    jobBuilder.setJobName("MatrixGenerator-job");
    jobBuilder.setWorkerClass(MDSWorker.class.getName());
    jobBuilder.addComputeResource(2, 2048, 1.0, workers);
    jobBuilder.setConfig(jobConfig);

    // now submit the job
    Twister2Submitter.submitJob(jobBuilder.build(), config);
  }
}
