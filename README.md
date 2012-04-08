# Clustermeister

## Introduction / What is Clustermeister?

Testing code on a dynamically provisioned cluster or in the cloud is in most cases a hassle for Java/Scala developers. The code needs to be packaged, cluster nodes have to be allocated, the allocated nodes have to be found, the packaged code needs to be deployed to all the nodes and usually the JVM on the node has to be restarted. This process is often managed using a variety of tools, ranging from SSH scripts to custom cloud APIs and clustering frameworks. The whole process is time-consuming to set up, manage and run. Another issue is that usually the connection from the developer machine to the cluster is slow, so transferring files from the developer machine to all the nodes is potentially slow. 

Clustermeister tries to solve this issue by providing tools to set up nodes easily and fast.

### Main Features

* Dynamic cluster provisioning for PBS/TORQUE and Amazon EC2
* Execute code on nodes either by directly addressing nodes or via an _ExecutorService_
* Transparent class loading / serialization

### Main Components

Clustermeister consist of 

* a command line client to set up nodes on either PBS/TORQUE or Amazon EC2
and 
* an API to either access those nodes individually or via an _ExecutorService_.

### Usage

_will follow in the near future_
