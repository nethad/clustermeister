#! /bin/sh

java -cp config:lib/* -Xmx32m -Djppf.config=jppf-driver.properties -Dlog4j.configuration=log4j-driver.properties -Djava.util.logging.config.file=config/logging-driver.properties com.github.nethad.clustermeister.driver.ClustermeisterDriverLauncher $1
#java -cp config:lib/* -Xmx32m -Djppf.config=jppf-driver.properties -Dlog4j.configuration=log4j-driver.properties -Djava.util.logging.config.file=config/logging-driver.properties org.jppf.server.DriverLauncher
