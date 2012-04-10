#! /bin/sh

java -cp config:lib/* -Xmx32m -Djppf.config=$1 -Dlog4j.configuration=log4j-node.properties -Djava.util.logging.config.file=config/logging-node.properties com.github.nethad.clustermeister.node.common.ClustermeisterNodeLauncher $2 $3
