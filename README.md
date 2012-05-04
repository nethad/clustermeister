# Clustermeister

**DISCLAIMER: Clustermeister is still under heavy development. It is in a working state, but might not yet be ready to be used by externs.**

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

#### Start CLI and start up nodes (TORQUE example)

To set up the infrastructure (start nodes, etc.) we need to start the command line client.

Right now, Clustermeister does not have any releases, so we need to check out the sources and build it ourselves.

`$ git checkout git://github.com/nethad/clustermeister.git`

`$ cd clustermeister`

`$ mvn clean install`

After that, we are able to start the command line client.

`$ java -jar cli/target/cli-0.1-SNAPSHOT.jar --help`

This prints out:

```
usage: cli.jar
 -c,--config <arg>     define the path to your configuration.yml,
                       default:
                       /home/username/.clustermeister/configuration.yml
 -h,--help             show this help text.
 -p,--provider <arg>   specify the provider to use, either 'amazon' or
                       'torque', default: torque
```

As you can see, we can choose a provider with the `-p` flag (either `torque` or `amazon`) and provide an configuration file. If no configuration file is specified (with the `-c` flag), the command line client looks for a configuration file in `~/.clustermeister/configuration.yml`. So we need to prepare a configuration file with the following configuration keys:

```yaml
torque:
  ssh_user: username
  ssh_privatekey: /path/to/your/private/key
  ssh_host: ssh.example.com
  ssh_port: 22
  email_notify: mail@example.com
```

After that, we're able to start the command line client.

`$ java -jar cli/target/cli-0.1-SNAPSHOT.jar`

This will print out:

```
[INFO ][provisioning.cli.ProvisioningCLI]: Using configuration in /home/username/.clustermeister/configuration.yml
[INFO ][provisioning.cli.ProvisioningCLI]: Using provider TORQUE
...
```

And return us to a shell, starting with `cm$`. Type `help` to see the commands available:

`cm$ help`

There we will see (among others):

`addnodes [number of nodes]  [processing threads per node]`

So, to start 2 nodes with 4 processing threads each, we type:

`cm$ addnodes 2 4`

This uploads the clustermeister resources to the job submission server and starts the nodes. Once these nodes are started up, they will connect to a driver on the local machine. We see this in the log output:

```
[provisioning.rmi.RmiServerForDriver]: Node connected 942E27562263A81E3369B2EB28B6885E
[provisioning.rmi.RmiServerForDriver]: Node connected 61D1AD22D7C0ED76BE65FD4ADFE85DE9
```

Now, the nodes are started up and we're ready to execute code.

#### Execute code via the API

After we have started up the nodes, we can execute code on the nodes via the Clustermeister API. Because there is no Clustermeister release yet, we need to add the api.jar manually. The jar file is located at `api/target/api-0.1-SNAPSHOT.jar`.

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

If we run the _ClustermeisterExample_, we should see the following output:

```
[main] INFO com.github.nethad.clustermeister.api.impl.ClustermeisterImpl - Provisioning returned 2 nodes.
Node 35B5171A8579193139B0CE95D7B97A66, result: Hello world!
Node 2EE63E38E94A7AD549927C73E2296C34, result: Hello world!
```

And that's it. Now we can run the same example again or build a new one. When we're finished, we return to the command line client and shut down our nodes:

`cm$ shutdown`

and exit the command line:

`cm$ exit`

And that's it. In this quick example, we saw how to set up Clustermeister to add nodes to our TORQUE infrastructure and run code on them via the Clustermeister API.
