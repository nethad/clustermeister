import java.io.Serializable;
import java.util.concurrent.Callable;

public class HelloWorldCallable implements Callable<String>, Serializable {

    @Override
    public String call() throws Exception {
        return "Hello world!";
    }
    
}

