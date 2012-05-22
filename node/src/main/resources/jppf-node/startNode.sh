#! /bin/sh

java -cp config:lib/* $4 -Djppf.config=$1 -Dlog4j.configuration=log4j-node.properties -Djava.util.logging.config.file=config/logging-node.properties com.github.nethad.clustermeister.node.common.ClustermeisterNodeLauncher $2 $3
