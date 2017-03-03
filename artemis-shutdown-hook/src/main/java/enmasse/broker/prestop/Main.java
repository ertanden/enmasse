/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.broker.prestop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.internal.restclient.model.Pod;
import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IContainer;
import enmasse.discovery.DiscoveryClient;
import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static void main(String [] args) throws Exception {
        boolean debug = System.getenv("PRESTOP_DEBUG") != null;

        Optional<Runnable> debugFn = Optional.empty();
        if (debug) {
            String kubeHost = System.getenv("KUBERNETES_SERVICE_HOST");
            String kubePort = System.getenv("KUBERNETES_SERVICE_PORT");
            String kubeUrl = String.format("https://%s:%s", kubeHost, kubePort);
            IClient client = new ClientBuilder(kubeUrl).usingToken(openshiftToken()).build();
            String namespace = openshiftNamespace();
            debugFn = Optional.of(() -> signalSuccess(client, namespace));
        }

        Host localHost = localHost();
        Vertx vertx = Vertx.vertx();

        if (System.getenv("TOPIC_NAME") != null) {
            String address = System.getenv("TOPIC_NAME");

            Map<String, String> filter = new LinkedHashMap<>();
            filter.put("role", "broker");
            filter.put("address", address);

            DiscoveryClient discoveryClient = new DiscoveryClient("podsense", filter, Optional.of("broker"));
            TopicMigrator migrator = new TopicMigrator(vertx, localHost);
            discoveryClient.addListener(migrator);
            vertx.deployVerticle(discoveryClient);
            migrator.migrate(address);
        } else {
            String messagingHost = System.getenv("MESSAGING_SERVICE_HOST");
            int messagingPort = Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT"));
            Endpoint to = new Endpoint(messagingHost, messagingPort);

            Optional<String> queueName = Optional.ofNullable(System.getenv("QUEUE_NAME"));
            QueueDrainer client = new QueueDrainer(vertx, localHost, debugFn);

            client.drainMessages(to, queueName);
        }
    }

    private static Host localHost() throws UnknownHostException {
        Map<String, Integer> portMap = new LinkedHashMap<>();
        portMap.put("amqp", 5673);
        portMap.put("core", 61616);

        return new Host(Inet4Address.getLocalHost().getHostAddress(), portMap);
    }

    private static void signalSuccess(IClient osClient, String namespace) {
        try {
            Pod pod = osClient.getResourceFactory().create("v1", ResourceKind.POD);
            pod.setName("mypod-test");
            pod.addContainer("containerfoo");
            IContainer cont = pod.getContainers().iterator().next();
            cont.setImage(new DockerImageURI("enmasseproject/artemis:latest"));
            cont.addEnvVar("QUEUE_NAME", "myqueue");

            osClient.create(pod, namespace);
            Thread.sleep(10000);
        } catch (InterruptedException e) {}
    }

    private static final String serviceAccountPath = "/var/run/secrets/kubernetes.io/serviceaccount";
    private static String openshiftNamespace() throws IOException {
        return readFile(new File(serviceAccountPath, "namespace"));
    }

    private static String openshiftToken() throws IOException {
        return readFile(new File(serviceAccountPath, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
