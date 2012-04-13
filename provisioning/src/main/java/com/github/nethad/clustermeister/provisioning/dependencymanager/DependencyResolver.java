/*
 * Copyright 2012 The Clustermeister Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nethad.clustermeister.provisioning.dependencymanager;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.transfer.AbstractTransferListener;
import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.transfer.TransferResource;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 *
 * @author daniel
 */
public class DependencyResolver {
    
    public static void main(String... args) throws DependencyCollectionException {
//        File projectDirectory = new File("/home/daniel/workspace/clustermeister/integration-tests");
//        File settings = new File("/home/daniel/.m2/settings.xml");
        
        RepositorySystem system = newRepositorySystem();
        RepositorySystemSession session = newRepositorySystemSession(system);
//        Artifact artifact = new DefaultArtifact( "com.typesafe.akka:akka-actor:2.0.1" );
//        Artifact artifact = new DefaultArtifact( "org.jppf:server:3.0.1" );
//        Artifact artifact = new DefaultArtifact( "org.apache.maven:maven-aether-provider:3.0.2" );
        Artifact artifact = new DefaultArtifact(args[0]);
        RemoteRepository repo = newCentralRepository();
        
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.addRepository(repo);
        collectRequest.addRepository(
                new RemoteRepository("typesafe", "default", "http://repo.typesafe.com/typesafe/releases/"));
        collectRequest.addRepository(
                new RemoteRepository("ifi", "default", "https://maven.ifi.uzh.ch/maven2/content/groups/public/"));

        CollectResult collectResult = system.collectDependencies(session, collectRequest);

        collectResult.getRoot().accept( new ConsoleDependencyGraphDumper() );
    }
    
    public static RepositorySystem newRepositorySystem() {
        /*
         * Aether's components implement org.sonatype.aether.spi.locator.Service to ease manual wiring and using the
         * prepopulated DefaultServiceLocator, we only need to register the repository connector factories.
         */
        MavenServiceLocator locator = new MavenServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class );
        locator.addService( RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class );
        locator.setServices( WagonProvider.class, new ManualWagonProvider() );

        return locator.getService( RepositorySystem.class );
    }
    
    public static RepositorySystemSession newRepositorySystemSession( RepositorySystem system )
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository( "target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( localRepo ) );

        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    public static RemoteRepository newCentralRepository() {
        return new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    }

    /**
     * A simplistic provider for wagon instances when no Plexus-compatible IoC
     * container is used.
     */
    public static class ManualWagonProvider
            implements WagonProvider {

        @Override
        public Wagon lookup(String roleHint)
                throws Exception {
            System.out.println("*---------------------------------------------------------------------------------------");
            System.out.println("hint: " + roleHint);
            System.out.println("*---------------------------------------------------------------------------------------");
            if ("http".equals(roleHint)) {
                return new LightweightHttpWagon();
            } else if("https".equals(roleHint)) {
                return new LightweightHttpsWagon();
            }
            return null;
        }

        @Override
        public void release(Wagon wagon) {
        }
    }

    /**
     * A simplistic repository listener that logs events to the console.
     */
    public static class ConsoleRepositoryListener
            extends AbstractRepositoryListener {

        private PrintStream out;

        public ConsoleRepositoryListener() {
            this(null);
        }

        public ConsoleRepositoryListener(PrintStream out) {
            this.out = (out != null) ? out : System.out;
        }

        @Override
        public void artifactDeployed(RepositoryEvent event) {
            out.println("Deployed " + event.getArtifact() + " to " + event.getRepository());
        }

        @Override
        public void artifactDeploying(RepositoryEvent event) {
            out.println("Deploying " + event.getArtifact() + " to " + event.getRepository());
        }

        @Override
        public void artifactDescriptorInvalid(RepositoryEvent event) {
            out.println("Invalid artifact descriptor for " + event.getArtifact() + ": "
                    + event.getException().getMessage());
        }

        @Override
        public void artifactDescriptorMissing(RepositoryEvent event) {
            out.println("Missing artifact descriptor for " + event.getArtifact());
        }

        @Override
        public void artifactInstalled(RepositoryEvent event) {
            out.println("Installed " + event.getArtifact() + " to " + event.getFile());
        }

        @Override
        public void artifactInstalling(RepositoryEvent event) {
            out.println("Installing " + event.getArtifact() + " to " + event.getFile());
        }

        @Override
        public void artifactResolved(RepositoryEvent event) {
            out.println("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        @Override
        public void artifactDownloading(RepositoryEvent event) {
            out.println("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        @Override
        public void artifactDownloaded(RepositoryEvent event) {
            out.println("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        @Override
        public void artifactResolving(RepositoryEvent event) {
            out.println("Resolving artifact " + event.getArtifact());
        }

        @Override
        public void metadataDeployed(RepositoryEvent event) {
            out.println("Deployed " + event.getMetadata() + " to " + event.getRepository());
        }

        @Override
        public void metadataDeploying(RepositoryEvent event) {
            out.println("Deploying " + event.getMetadata() + " to " + event.getRepository());
        }

        @Override
        public void metadataInstalled(RepositoryEvent event) {
            out.println("Installed " + event.getMetadata() + " to " + event.getFile());
        }

        @Override
        public void metadataInstalling(RepositoryEvent event) {
            out.println("Installing " + event.getMetadata() + " to " + event.getFile());
        }

        @Override
        public void metadataInvalid(RepositoryEvent event) {
            out.println("Invalid metadata " + event.getMetadata());
        }

        @Override
        public void metadataResolved(RepositoryEvent event) {
            out.println("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
        }

        @Override
        public void metadataResolving(RepositoryEvent event) {
            out.println("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
        }
    }

    /**
     * A simplistic transfer listener that logs uploads/downloads to the
     * console.
     */
    public static class ConsoleTransferListener
            extends AbstractTransferListener {

        private PrintStream out;
        private Map<TransferResource, Long> downloads = new ConcurrentHashMap<TransferResource, Long>();
        private int lastLength;

        public ConsoleTransferListener() {
            this(null);
        }

        public ConsoleTransferListener(PrintStream out) {
            this.out = (out != null) ? out : System.out;
        }

        @Override
        public void transferInitiated(TransferEvent event) {
            String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

            out.println(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
        }

        @Override
        public void transferProgressed(TransferEvent event) {
            TransferResource resource = event.getResource();
            downloads.put(resource, Long.valueOf(event.getTransferredBytes()));

            StringBuilder buffer = new StringBuilder(64);

            for (Map.Entry<TransferResource, Long> entry : downloads.entrySet()) {
                long total = entry.getKey().getContentLength();
                long complete = entry.getValue().longValue();

                buffer.append(getStatus(complete, total)).append("  ");
            }

            int pad = lastLength - buffer.length();
            lastLength = buffer.length();
            pad(buffer, pad);
            buffer.append('\r');

            out.print(buffer);
        }

        private String getStatus(long complete, long total) {
            if (total >= 1024) {
                return toKB(complete) + "/" + toKB(total) + " KB ";
            } else if (total >= 0) {
                return complete + "/" + total + " B ";
            } else if (complete >= 1024) {
                return toKB(complete) + " KB ";
            } else {
                return complete + " B ";
            }
        }

        private void pad(StringBuilder buffer, int spaces) {
            String block = "                                        ";
            while (spaces > 0) {
                int n = Math.min(spaces, block.length());
                buffer.append(block, 0, n);
                spaces -= n;
            }
        }

        @Override
        public void transferSucceeded(TransferEvent event) {
            transferCompleted(event);

            TransferResource resource = event.getResource();
            long contentLength = event.getTransferredBytes();
            if (contentLength >= 0) {
                String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
                String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

                String throughput = "";
                long duration = System.currentTimeMillis() - resource.getTransferStartTime();
                if (duration > 0) {
                    DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                    double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
                    throughput = " at " + format.format(kbPerSec) + " KB/sec";
                }

                out.println(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len
                        + throughput + ")");
            }
        }

        @Override
        public void transferFailed(TransferEvent event) {
            transferCompleted(event);

            event.getException().printStackTrace(out);
        }

        private void transferCompleted(TransferEvent event) {
            downloads.remove(event.getResource());

            StringBuilder buffer = new StringBuilder(64);
            pad(buffer, lastLength);
            buffer.append('\r');
            out.print(buffer);
        }

        @Override
        public void transferCorrupted(TransferEvent event) {
            event.getException().printStackTrace(out);
        }

        protected long toKB(long bytes) {
            return (bytes + 1023) / 1024;
        }
    }
 
    /**
     * A dependency visitor that dumps the graph to the console.
     */
    public static class ConsoleDependencyGraphDumper
            implements DependencyVisitor {

        private PrintStream out;
        private String currentIndent = "";

        public ConsoleDependencyGraphDumper() {
            this(null);
        }

        public ConsoleDependencyGraphDumper(PrintStream out) {
            this.out = (out != null) ? out : System.out;
        }

        public boolean visitEnter(DependencyNode node) {
            out.println(currentIndent + node);
            if (currentIndent.length() <= 0) {
                currentIndent = "+- ";
            } else {
                currentIndent = "|  " + currentIndent;
            }
            return true;
        }

        public boolean visitLeave(DependencyNode node) {
            currentIndent = currentIndent.substring(3, currentIndent.length());
            return true;
        }
    }

}
