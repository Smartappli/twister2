#!/bin/bash

set -eu

readonly MVN_GOAL="$1"
readonly VERSION_NAME="$2"
shift 2
readonly EXTRA_MAVEN_ARGS=("$@")

bazel_output_file() {
  local library=$1
  local output_file=bazel-bin/$library
  if [[ ! -e $output_file ]]; then
    output_file=bazel-genfiles/$library
  fi
  if [[ ! -e $output_file ]]; then
    echo "Could not find bazel output file for $library"
    exit 1
  fi
  echo -n $output_file
}

deploy_library() {
  local library="$1/lib$2.jar"
  local pomfile="$1/pom.xml"
  local source="$1/lib$2-src.jar"
  local javadoc="$1/$2-javadoc.jar"
  bazel build --define=pom_version="$VERSION_NAME" \
    $library $pomfile $source $javadoc

  printf "\nGenerating %s %s\n\n" "$1" "$MVN_GOAL"

  mvn $MVN_GOAL \
    -Dfile=$(bazel_output_file $library) \
    -DpomFile=$(bazel_output_file $pomfile) \
    -Dsources=$(bazel_output_file $source) \
    -Djavadoc=$(bazel_output_file $javadoc) \
    "${EXTRA_MAVEN_ARGS[@]:+${EXTRA_MAVEN_ARGS[@]}}"
}

#todo due to an unknown reason proto libraries generated by native rules has a suffix speed. Hence taking two args to address this issue temporary
deploy_proto_library() {
  local library=$1
  local pomfile=$2
  local buildfile=$3
  bazel build --define=pom_version="$VERSION_NAME" \
    $library $pomfile --action_env=JAVA_HOME

  mvn $MVN_GOAL \
    -Dfile=$(bazel_output_file $buildfile) \
    -DpomFile=$(bazel_output_file $pomfile) \
    "${EXTRA_MAVEN_ARGS[@]:+${EXTRA_MAVEN_ARGS[@]}}"
}

# APIs

deploy_library \
  twister2/api/src/java \
  api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/checkpointing \
  checkpointing-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/comms \
  comms-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/config \
  config-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/data \
  data-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/dataset \
  dataset-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/exceptions \
  exceptions-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/net \
  network-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/resource \
  resource-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/scheduler \
  scheduler-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/compute \
  task-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/tset \
  tset-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/util \
  api-utils-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/faulttolerance \
  fault-tolerance-api-java

deploy_library \
  twister2/api/src/java/edu/iu/dsc/tws/api/driver \
  driver-api-java

# End of APIs

deploy_library \
  twister2/common/src/java \
  common-java

deploy_library \
  twister2/comms/src/java \
  comms-java

deploy_library \
  twister2/connectors/src/java \
  connector-java

deploy_library \
  twister2/data/src/main/java \
  data-java

deploy_library \
  twister2/examples/src/java \
  examples-java

deploy_library \
  twister2/executor/src/java \
  executor-java

deploy_library \
  twister2/master/src/java \
  master-java

deploy_library \
  twister2/resource-scheduler/src/java \
  resource-scheduler-java

deploy_library \
  twister2/task/src/main/java \
  task-java

deploy_library \
  twister2/taskscheduler/src/java \
  taskscheduler-java

deploy_library \
  twister2/compatibility/storm \
  twister2-storm

deploy_library \
  twister2/compatibility/beam \
  twister2-beam

deploy_library \
  twister2/checkpointing/src/java \
  checkpointing-java

deploy_library \
  twister2/proto \
  jproto-java

deploy_library \
  twister2/proto/utils \
  proto-utils-java

deploy_library \
  twister2/compatibility/harp \
  twister2-harp

deploy_library \
  twister2/tools/local-runner/src/java \
  local-runner-java

deploy_library \
  twister2/tset/src/java \
  tset-java
