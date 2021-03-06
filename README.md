## Dapr SDK for Java

This is the Dapr SDK for Java, based on the auto-generated proto client.<br>

For more info on Dapr and gRPC, visit [this link](https://github.com/dapr/docs/tree/master/howto/create-grpc-app).

### Installing

Clone this repository including the submodules:

```sh
git clone https://github.com/dapr/java-sdk.git
```

Then head over to build the [Maven](https://maven.apache.org/install.html) (Apache Maven version 3.x) project:

```sh
# make sure you are in the `java-sdk` directory.
mvn install
```

### Running the examples
Try the following examples to learn more about Dapr's Java SDK:
* [Invoking a service via Grpc](./examples/src/main/java/io/dapr/examples/invoke/grpc)
* [State management over Grpc](./examples/src/main/java/io/dapr/examples/state/grpc)
* [State management over HTTP](./examples/src/main/java/io/dapr/examples/state/http)

### Creating and publishing the artifacts to Nexus Repository
From the root directory:

```sh
mvn package
mvn deploy:deploy-file -DgeneratePom=false -DrepositoryId=nexus -Durl=http://localhost:8081/repository/maven-releases -DpomFile=pom.xml -Dfile=target/client-0.1.0-preview.jar
```
For more documentation reference :

https://maven.apache.org/plugins/maven-deploy-plugin

https://help.sonatype.com/repomanager3/user-interface/uploading-components

### Maven Module version management
To increase the version of all modules and pom files, run the following commands:

```sh
mvn versions:set -DnewVersion="0.1.0-preview02"
mvn versions:commit
```

### Debug Java application or Dapr's Java SDK

If you have a Java application or an issue on this SDK that needs to be debugged, follow the steps below:

Install [Pen Load Balancer](https://sourceforge.net/projects/penloadbalancer/) or your preferred load balancer utility:
```sh
sudo apt-get install pen
```
Note: Pen is also available on Windows in the link above. For MacOS, you will need to [build it from source code](https://github.com/UlricE/pen/wiki/Building-Pen-from-Git).

Then run Dapr with the load balancer process listening on port 3001 and forwarding to port 3000. For Pen Load Balancer, it would be:
```sh
dapr run --app-id testapp --app-port 3001 --port 3500 -- pen -b 99999999 -f localhost:3001 localhost:3000
```

The command below will start a load balancer listening on port `3001` that forwards connections to port `3000`, while Dapr's app identifier is `testapp` and listening port is `3500`. If you try to make a HTTP call to any URL on `localhost:3001`, it will fail until you have an application listening on `localhost:3000`.

Now you can go to your IDE (like IntelliJ, for example) and debug your Java application, using port `3500` to call Dapr while also listening to port `3000` to expose Dapr's callback endpoint.

Calls to Dapr's APIs on `http://localhost:3500/*` should work now and trigger breakpoints in your code.

**If your application needs to suscribe to topics or register Actors in Dapr, for example, then start debugging your app first and run dapr with load balancer last.**

Reminder: for Dapr, your application is listening to port `3001` and not `3000` since it can only see the load balancer's port.

**If using Visual Studio Code, also consider [this solution as well](https://github.com/dapr/docs/tree/master/howto/vscode-debugging-daprd).**
