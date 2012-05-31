# Clustermeister

**DISCLAIMER: Clustermeister is still under heavy development.**
**It is in a working state, but might not yet be ready to be used by externs.**

## Introduction

### What is Clustermeister?

Clustermeister provides a framework for easy code execution and testing on remote and distributed 
Java Virtual Machines (JVM). Specifically it provides utilities to facilitate remote code deployment 
scenarios and an API to execute code on remote JVMs.

### Why would I use Clustermeister?

Testing code on a dynamically provisioned cluster or in the cloud is in most cases a hassle for Java/Scala developers. 
The code needs to be packaged, cluster nodes have to be allocated, the allocated nodes have to be found, the 
packaged code needs to be deployed to all the nodes and usually the JVM on the node has to be restarted. 
This process is often managed using a variety of tools, ranging from SSH scripts to custom cloud APIs and 
clustering frameworks. The whole process is time-consuming to set up, manage and run. Another issue is that 
usually the connection from the developer machine to the cluster is slow, so transferring files from the 
developer machine to all the nodes is potentially slow. 

Clustermeister tries to solve this issue by providing tools to set up nodes easily and fast.

### Main Features

* Deployment of [JPPF](http://www.jppf.org/) nodes on (virtual) machines requiring only minimal configuration.
* Provisioning of Amazon EC2 instances provided by [jClouds](http://www.jclouds.org/).
* Parallel and distributed code execution via a Java ExecutorService interface or a native JPPF interface.
* Dynamic classloading allowing for rapid re-execution of client code without manual re-deployment.
* Addressable nodes for code execution on specific nodes.
* Easy deployment of dependencies using maven repository dependency resolution provided by 
[Sonatype Aether](http://www.sonatype.org/aether).

### Main Components

Clustermeister consist of 

* a command line client to set up nodes on either PBS/TORQUE or Amazon EC2
and 
* an API to either access those nodes individually or via an _ExecutorService_.

### Usage

This is just a quick-start guide to provide you with an overview on how Clustermeister works. 
For a more in-depth explanation, check out the [tutorial](https://github.com/nethad/clustermeister/wiki/Tutorial).

#### Start CLI and start up nodes (TORQUE example)

To set up the infrastructure (start nodes, etc.) we need to start the command line client.

Right now, Clustermeister does not have any releases, so we need to check out the sources and build it ourselves.

`$ git checkout git://github.com/nethad/clustermeister.git`

`$ cd clustermeister`

`$ mvn clean install`

After that, we are able to start the command line client.

`$ java -jar cli/target/cli-0.1-SNAPSHOT.jar -p local`

The `-p local` argument starts the Clustermeister CLI with the local provider. This is a good choice to get familiar with Clustermeister. It deploys node to the local machine and no configuration is necessary. If no configuration file is specified (with the `-c` flag), the command line client looks for a configuration file 
in `~/.clustermeister/configuration.yml`. If no configuration is found there, a sample configuration is 
generated.

In the command line output, we see:

```
[INFO ][provisioning.cli.ProvisioningCLI]: Using configuration in /home/username/.clustermeister/configuration.yml
[INFO ][provisioning.cli.ProvisioningCLI]: Using provider LOCAL
...
```

and we are returned into the Clustermeister shell, starting with `cm$`. Type `help` to see the commands available:

`cm$ help`

There we will see (among others):

`addnodes [number of nodes]  [processing threads per node]`

So, to start 2 nodes with 4 processing threads each, we type:

`cm$ addnodes 2 4`

This deploys nodes to the temp directory (`/tmp` for most Linux setups) and starts them up. Once these 
nodes are started up (it takes a few seconds), they will connect to a driver on the local machine. We see this in the log output:

```
[PROVISIONING]: Node connected 942E27562263A81E3369B2EB28B6885E
[PROVISIONING]: Node connected 61D1AD22D7C0ED76BE65FD4ADFE85DE9
```

Now, the nodes are started up and we're ready to execute code.

#### Execute code via the API

After we have started up the nodes, we can execute code on the nodes via the Clustermeister API. Because 
there is no Clustermeister release yet, we need to add the api.jar manually. The jar file is located at 
`api/target/api-0.1-SNAPSHOT.jar`.

To execute the simplest possible example, we need 2 Java classes:

* HelloWorldCallable.java

```java
import java.io.Serializable;
import java.util.concurrent.Callable;

public class HelloWorldCallable implements Callable<String>, Serializable {

    @Override
    public String call() throws Exception {
        return "Hello world!";
    }
    
}
```

* ClustermeisterExample.java

```java
import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

public class ClustermeisterExample {
    
    public static void main(String... args) {
        Clustermeister clustermeister = null;
        try {
            clustermeister = ClustermeisterFactory.create();
            for (ExecutorNode executorNode : clustermeister.getAllNodes()) {
                ListenableFuture<String> resultFuture = executorNode.execute(new HelloWorldCallable());
                String result = resultFuture.get();
                System.out.println("Node " + executorNode.getID() + ", result: " + result);
            }
        } catch (InterruptedException ex) {
            System.err.println("Exception while waiting for result: " + ex.getMessage());
        } catch (ExecutionException ex) {
            System.err.println("Exception while waiting for result: " + ex.getMessage());
        } finally {
            if (clustermeister != null) {
                clustermeister.shutdown();
            }
        }
    }

}
```

If we run the `ClustermeisterExample`, we should see the following output:

```
[API] - Provisioning returned 2 nodes.
Node 35B5171A8579193139B0CE95D7B97A66, result: Hello world!
Node 2EE63E38E94A7AD549927C73E2296C34, result: Hello world!
```

And that's it. Now we can run the same example again or build a new one. When we're finished, we 
return to the command line client and shut down our nodes:

`cm$ shutdown`

and exit the command line:

`cm$ exit`

And that's it. In this quick example, we saw how to set up Clustermeister to add nodes to our TORQUE 
infrastructure and run code on them via the Clustermeister API. As mentioned above, for a more in-depth 
explanation, check out the [tutorial](https://github.com/nethad/clustermeister/wiki/Tutorial).
