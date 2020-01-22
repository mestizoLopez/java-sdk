/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.services;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.client.DefaultObjectSerializer;
//import io.dapr.springboot.DaprApplication;
import io.dapr.actors.services.springboot.DaprApplication;
/*
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service for Actor runtime.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the server:
 * dapr run --app-id demoactorservice --app-port 3000 --port 3005 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.actors.http.DemoActorService -Dexec.args="-p 3000"
 */
@SpringBootApplication
public class DemoActorService {

  public static void main(String[] args) throws Exception {
  /*
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port Dapr will listen to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
*/
    // ZZZ todo parse port
    // If port string is not valid, it will throw an exception.

    // this is the same as the --app-port passed into dapr run
    //int port = Integer.parseInt(cmd.getOptionValue("port"));
    //int port = 3000; // ZZZ temp to compile

    System.out.println("Hello from DemoActorService main()");
    System.out.println("...port is " + args[0]);
  int port = Integer.parseInt(args[0]);
    // Register the Actor class.
    ActorRuntime.getInstance().registerActor(DemoActorImpl.class, new DefaultObjectSerializer());

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);

    // Start application's endpoint.
    SpringApplication.run(DemoActorService.class);
  }
}
