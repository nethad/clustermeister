#! /bin/sh

java -cp config:lib/* -Xmx32m -Djppf.config=jppf-driver.properties -Dlog4j.configuration=log4j-driver.properties -Djava.util.logging.config.file=config/logging-driver.properties com.github.nethad.clustermeister.node.common.ClustermeisterDriverLauncher $1 $2 $3
