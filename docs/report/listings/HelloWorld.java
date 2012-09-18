import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

public class HelloWorld {
    
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
