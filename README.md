### [Last updated: August 1, 2022]
------
**Learn how to build JVM and Native Java images with Spring Native and GraalVM, then deploy them on the Google CloudRun serverless compute platform.**


The sample app provides a simple `Hello` web app based on Spring Boot and Spring Cloud Functions. 
The focus is on building and deploying native images to a serverless platform. The update to this repo follows the latest release of GraalVM 22.2.0 with Java 17 support, which brings a series of significant improvements.

Intel and Apple Silicon builds are supported for the different build methods at this time.
[GraalVM 22.2: Smaller JDK size, improved memory usage, better library support, and more!](https://medium.com/graalvm/graalvm-22-2-smaller-jdk-size-improved-memory-usage-better-library-support-and-more-cb34b5b68ec0)

[Full details - GraalVM 22.1: Developer experience improvements, Apple Silicon builds, and more](https://medium.com/graalvm/graalvm-22-1-developer-experience-improvements-apple-silicon-builds-and-more-b7ac9a0f6066)

### `Dive into`
1. Build 
    * JVM & Native `Application image` with the Spring Boot Maven plugin and GraalVM
    * JVM & Native `Docker image` using Java/Native Java Paketo Buildpacks and Compression
2. Generate 
   * Native Tests
3. Deploy
    * Deploy to Google Cloud Run
4. Develop
   * How to use the new Quick Build Mode for Developers!
5. Analyze 
   * App & Container image sizes  
   * Start-up latency
   * RSS memory consumption

### `Java and library versions in use`
* Spring Boot 2.7.2
* Spring Native 0.12.1 
* Native Buildtools 0.9.13
* Spring Cloud 2021.0.3
* OpenJDK
  * openjdk version "17.0.4" 2022-07-19
* GraalVM CE
  * OpenJDK Runtime Environment GraalVM CE 22.2.0 (build 17.0.4+8-jvmci-22.2-b06)
  * OpenJDK 64-Bit Server VM GraalVM CE 22.2.0 (build 17.0.4+8-jvmci-22.2-b06, mixed mode, sharing)
* Java compatibility level: Java 17

### `Known Issues`
* Java Native Buildpacks are failing on M1 Macs as of today; Native Java App executables can be built cleanly though
* UPX compression for M1 Macs is a known issue with a pending patch

# Installation
Install GraalVM from:
* [from GraalVM Github repo](https://github.com/graalvm/graalvm-ce-builds/releases)
* [using Homebrew](https://github.com/graalvm/homebrew-tap)
* [SDKMan](https://sdkman.io/): `sdk install java 22.2.0.r17-grl`

Install the native-image builder before building native executables: 
```shell
gu install native-image
```

# Build

## Build application images
Building an executable application with the GraalVM compiler requires the installation of the GraalVM and the native-image builder utility and leverages the following `Maven profile`:
* `jvm` (or noprofile specified, as default)
* `native`

### Build code as a JVM app using the Spring Boot Maven plugin
```bash 
# build and run code using
./mvnw clean package spring-boot:run

# test locally from a terminal window
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a JVM app"
```
### Build code as a Native Java app using the GraalVM compiler
```bash 
# switch to the GraalVM JDK for this build
# ex, when using SDKman, validate that you use the GraaLVM compiler
sdk use java 22.2.0.r17-grl

# build and run code using GraalVM
# generating native tests is skipped for reduced build latency
./mvnw clean package -Pnative -DskipTests

# start the native executable
./target/hello-function

# test locally from a terminal window
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Native app"
```
## Build Docker images

Building the code with the Spring Boot Maven wrapper uses the following `Maven profiles`:
* `native-image` - build a Native image leveraging GraalVM
* `jvm-image` - build a JVM-based image leveraging OpenJDK

### Build code as a JVM Docker image using the Spring Boot Maven plugin and Java Paketo Buildpacks
```bash 
# build image with default configuration
./mvnw clean spring-boot:build-image

# build image with the CNB Paketo buildpack of your choice
./mvnw clean spring-boot:build-image -Pjvm-image 

# start Docker image
docker run -p 8080:8080 hello-function-jvm:r17

# test Docker image locally
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a JVM app running in a container"
```

### Build code as a Spring Native Docker image using the Spring Boot Maven plugin and the Java Native Paketo Buildpacks
```bash 
# build image with the CNB Paketo buildpack of your choice
# generating native tests is skipped for reduced build latency
./mvnw clean spring-boot:build-image -Pnative-image -DskipTests

# start Docker image
docker run -p 8080:8080 hello-function-native:r17

# test Docker image locally
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Native app running in a container"
```

# Generate Native Tests
Testing Java code with JUnit 5 behaves exactly the same in native execution as with the JVM. 
Writing proper unit tests and generating native test images assists you in ensuring that the native image of the app will work in the same manner as on the JVM

The [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html) project provides plugins for different build tools to add support for building and testing native applications written in Java (or any other language compiled to JVM bytecode).

To build native tests
```bash 
# switch to the GraalVM JDK for this build
# ex, when using SDKman, validate that you use the GraaLVM compiler
sdk use java 22.2.0.r17-grl

# test the app with native tests
./mvnw -Pnative test

# start the native test executable
$ ./target/native-tests
```

Observe the significant latency reduction in test execution:
```text
JUnit JVM tests
...
2022-08-01 12:05:48.527  INFO 78674 --- [           main] e.h.SpringNativeFunctionApplicationTests : Started SpringNativeFunctionApplicationTests in 0.812 seconds (JVM running for 1.665)
...
```

Native Tests
```text
12:12:24.087 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper - Using TestExecutionListeners: []
com.example.hello.SpringNativeFunctionApplicationTests > contextLoads() SUCCESSFUL

Test run finished after 59 ms
[         2 containers found      ]
[         0 containers skipped    ]
[         2 containers started    ]
[         0 containers aborted    ]
[         2 containers successful ]
[         0 containers failed     ]
[         1 tests found           ]
[         0 tests skipped         ]
[         1 tests started         ]
[         0 tests aborted         ]
[         1 tests successful      ]
[         0 tests failed          ]
```
# Deploy

## Cloud Run Deploy

Build the image as a JVM or native image and deploy it to Cloud Run from the command-line. You can also deploy directly from the GCP console.

**Note**: Please note the Project ID of the project where you wish to deploy your service

#### Build
```shell
# authorize the user to GCP
gcloud auth list

# check if the project is set
gcloud config list

...
project = optimize-serverless-apps
...

# set project ID if not already set
gcloud config set project <project id>
# ex:
gcloud config set project optimize-serverless-apps

# tag the images and push them to the Container Registry 
docker tag hello-function-jvm:r17 gcr.io/optimize-serverless-apps/hello-function-jvm:r17
docker tag hello-function-native:r17-upx gcr.io/optimize-serverless-apps/hello-function-native:r17

# push the image(s) to the Container Registry
docker push gcr.io/optimize-serverless-apps/hello-function-jvm:r17
docker push gcr.io/optimize-serverless-apps/hello-function-native:r17
```

As a developer, the service can be deployed, for testing, unauthorized, while providing full access to the service.
However, developers, in addition to administrative actions such as creating, updating, and deleting services, often want to test services privately before releasing them.

You have to ensure that you grant permissions to access the services you are authenticating to. 

For development, the easiest way to test a service that requires authentication is to use a tool like curl and pass an auth token in the Authorization header.
This is NOT recommended outside of testing, and proper tokens should be generated during deployment.

#### Deploy with unauthenticated users:
```bash
# deploy the container to CloudRun
# Note that we are specifying:
#    app name - hello-function
#    region - us-central1
#    memory allocated for the process - 1Gi
#    simple apps allow all users, unauthenticated, best practice is to set service accounts up

# deploy a JVM image
gcloud run deploy hello-function-jvm \
  --image=gcr.io/optimize-serverless-apps/hello-function-jvm:r17 \
  --region us-central1 \
  --memory 1Gi --allow-unauthenticated

...
Deploying container to Cloud Run service [hello-function-jvm] in project [optimize-serverless-apps] region [us-central1]
✓ Deploying... Done.                                                                                                                                       
  ✓ Creating Revision...                                                                                                                                   
  ✓ Routing traffic...                                                                                                                                     
  ✓ Setting IAM Policy...                                                                                                                                  
Done.                                                                                                                                                      
Service [hello-function-jvm] revision [hello-function-jvm-00001-soj] has been deployed and is serving 100 percent of traffic.

# deploy a Native Java image
gcloud run deploy hello-function-native \
  --image=gcr.io/optimize-serverless-apps/hello-function-native:r17 \
  --region us-central1 \
  --memory 1Gi --allow-unauthenticated  

...
Service [hello-function-native] revision [hello-function-native-00001-xah] has been deployed and is serving 100 percent of traffic.
...

# check that the project has been successfully deployed 
# retrieve the URL where the app can be accessed and send a request
gcloud run services list
   SERVICE                   REGION       URL                                                     LAST DEPLOYED AT
✔  hello-function-jvm          us-central1   https://hello-function-jvm-...-uc.a.run.app          <email>  2022-08-01T16:04:13.864504Z
✔  hello-function-native       us-central1   https://hello-function-native-...-uc.a.run.app       <email>  2022-08-01T16:02:58.368234Z

# Test the JVM service
curl -w'\n' -H 'Content-Type: text/plain' https://hello-function-jvm-...-uc.a.run.app -d "from a JVM Image"
Hello: from a JVM Image, Source: a Spring function !

# Test the Native Java service
curl -w'\n' -H 'Content-Type: text/plain' https://hello-function-native-...-uc.a.run.app -d "from a Native Image"
Hello: from a Native Image, Source: a Spring function !

# delete the service 
gcloud run services delete hello-function-jvm --region us-central1
gcloud run services delete hello-function-native --region us-central1
```

#### Deploy with authenticated users:
```shell
# deploy the container to CloudRun
# Note that we are specifying:
#    app name - hello-function
#    region - us-central1
#    memory allocated for the process - 1Gi
#    simple apps allow all users, unauthenticated, best practice is to set service accounts up

# deploy JVM image
gcloud run deploy hello-function-jvm \
  --image=gcr.io/optimize-serverless-apps/hello-function-jvm:r17 \
  --region us-central1 \
  --memory 1Gi

# do not allow unathenticated users at the prompt
Allow unauthenticated invocations to [hello-function] (y/N)?  N

Deploying container to Cloud Run service [hello-function-jvm] in project [optimize-serverless-apps] region [us-central1]
...
Service [hello-function-jvm] revision [hello-function-jvm-00001-vel] has been deployed and is serving 100 percent of traffic.
Service URL: https://hello-function-jvm-...-uc.a.run.app

# deploy Native Java image
gcloud run deploy hello-function-native \
  --image=gcr.io/optimize-serverless-apps/hello-function-jvm:r17 \
  --region us-central1 \
  --memory 1Gi  

# do not allow unathenticated users at the prompt
Allow unauthenticated invocations to [hello-function] (y/N)?  N

Deploying container to Cloud Run service [hello-function-native] in project [optimize-serverless-apps] region [us-central1]
...
Service [hello-function-native] revision [hello-function-native-00001-fad] has been deployed and is serving 100 percent of traffic.
Service URL: https://hello-function-native-...-uc.a.run.app


# retrieve the URL where the app can be accessed and send a request
gcloud run services list
   SERVICE               REGION       URL                                                         LAST DEPLOYED AT
✔  hello-function-jvm          us-central1   https://hello-function-jvm-...-uc.a.run.app          <email>  2022-08-01T16:04:13.864504Z
✔  hello-function-native       us-central1   https://hello-function-native-...-uc.a.run.app       <email>  2022-08-01T16:02:58.368234Z

# grant the Cloud Run Invoker role to the developer
# substitute the developer email
$ gcloud run services add-iam-policy-binding hello-function \
    --member='user:<email>' \
    --role='roles/run.invoker'   \
    --region us-central1            

Updated IAM policy for service [hello-function].
bindings:
- members:
  - user:<email>
  role: roles/run.invoker
etag: BwXQv-XczNA=
version: 1
...

# developer can  print an identity token for the specified account to generate a token for development
# command: gcloud auth print-identity-token 

# for convenience, you can create an alias
alias gcurl='curl --header "Authorization: Bearer $(gcloud auth print-identity-token)"'

# you can use the new alias to curl the service
# invoke the JVM image service
gcurl -w'\n' -H 'Content-Type: text/plain' https://hello-function-jvm-...-uc.a.run.app -d "from a JVM Image"
Hello: from a JVM Image, Source: a Spring function !

# invoke the Native image service
gcurl -w'\n' -H 'Content-Type: text/plain' https://hello-function-native-...-uc.a.run.app -d "from a Native Image"
Hello: from a Native Image, Source: a Spring function !
```

#### Clean-up 
```shell
# delete the service 
gcloud run services delete hello-function-jvm --region us-central1
gcloud run services delete hello-function-native --region us-central1

```

# How to use Quick Build Mode for Development

Quick Build mode significantly improves build latency by running the compiler in economy mode, with fewer optimizations, resulting in much faster builds.

This is a Development feature, not recommended for Production !!!
In Production, use the default compilation mode, which provides the best runtime performance and memory efficiency!

To enable quick build mode, add `-Ob (capital “O”, lower case “b”)` when building with the native-image utility.
Ex.:
```xml
<profile>
    <id>native</id>
    ...
    <dependencies>
        <dependency>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>junit-platform-native</artifactId>
            <version>${native-buildtools.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <version>${native-buildtools.version}</version>
                <extensions>true</extensions>
                <!--Enable Quick Build mode -->
                <configuration>
                    <buildArgs>-Ob</buildArgs>
                </configuration>
                <!--end -->
                ...
```

When building an app, the GraalVM compiler will perform 7 steps,  from initialization to building an app image.
The output provides significantly more information than in previous versions.

Step `[6/7] Compiling methods ...` is the step when optimizations will be executed.

Excerpt from building the `optimized, production app`:
```text
========================================================================================================================
GraalVM Native Image: Generating 'hello-function' (executable)...
========================================================================================================================
[1/7] Initializing...                                                                                    (7.2s @ 0.19GB)
 Version info: 'GraalVM 22.2.0 Java 17 CE'
 Java version info: '17.0.4+8-jvmci-22.2-b06'
 C compiler: cc (apple, x86_64, 13.1.6)
 Garbage collector: Serial GC
[2/7] Performing analysis...  [********]                                                                (51.2s @ 1.95GB)
  14,442 (90.90%) of 15,888 classes reachable
  22,701 (66.51%) of 34,131 fields reachable
  68,679 (61.81%) of 111,106 methods reachable
     786 classes,   281 fields, and 3,805 methods registered for reflection
      63 classes,    69 fields, and    55 methods registered for JNI access
       5 native libraries: -framework CoreServices, -framework Foundation, dl, pthread, z
[3/7] Building universe...                                                                               (7.7s @ 4.66GB)
[4/7] Parsing methods...      [***]                                                                     (10.0s @ 5.14GB)
[5/7] Inlining methods...     [***]                                                                      (5.8s @ 3.46GB)
[6/7] Compiling methods...    [********]                                                                (63.2s @ 3.83GB)
[7/7] Creating image...                                                                                  (6.8s @ 2.68GB)
  30.27MB (50.00%) for code area:    45,406 compilation units
  29.99MB (49.54%) for image heap:  325,057 objects and 292 resources
 284.67KB ( 0.46%) for other data
  60.54MB in total
------------------------------------------------------------------------------------------------------------------------
Top 10 packages in code area:                               Top 10 object types in image heap:
   1.66MB sun.security.ssl                                     6.48MB byte[] for code metadata
   1.05MB java.util                                            3.56MB java.lang.Class
 732.48KB com.sun.crypto.provider                              3.06MB java.lang.String
 649.59KB org.apache.tomcat.util.net                           2.91MB byte[] for embedded resources
 566.29KB java.lang.invoke                                     2.61MB byte[] for java.lang.String
 551.31KB org.apache.catalina.core                             2.51MB byte[] for general heap data
 493.46KB org.apache.coyote.http2                              1.21MB com.oracle.svm.core.hub.DynamicHubCompanion
 487.28KB java.lang                                          805.55KB byte[] for reflection metadata
 478.34KB java.util.concurrent                               632.21KB java.lang.String[]
 469.79KB sun.security.x509                                  585.94KB java.util.HashMap$Node
  22.89MB for 587 more packages                                4.74MB for 2973 more object types
------------------------------------------------------------------------------------------------------------------------
                        6.0s (3.7% of total time) in 34 GCs | Peak RSS: 6.06GB | CPU load: 3.86
------------------------------------------------------------------------------------------------------------------------
Produced artifacts:
 /Users/ddobrin/work/dan/native-spring-cloud-run-with-graalvm/target/hello-function (executable)
 /Users/ddobrin/work/dan/native-spring-cloud-run-with-graalvm/target/hello-function.build_artifacts.txt (txt)
========================================================================================================================
```

When building the `developer app`, less optimizations will be performed, thus speeding the build up. The change is more noticable the larger the number of classes in your project is.

Excerpt from building the `developer, non-prod app` - notice the `warning` at the top of this snippet:
```text
You enabled -Ob for this image build. This will configure some optimizations to reduce image build time.
This feature should only be used during development and never for deployment.
...
========================================================================================================================
GraalVM Native Image: Generating 'hello-function' (executable)...
========================================================================================================================
[1/7] Initializing...                                                                                    (7.6s @ 0.20GB)
 Version info: 'GraalVM 22.2.0 Java 17 CE'
 Java version info: '17.0.4+8-jvmci-22.2-b06'
 C compiler: cc (apple, x86_64, 13.1.6)
 Garbage collector: Serial GC
[2/7] Performing analysis...  [********]                                                                (40.6s @ 3.08GB)
  14,442 (90.90%) of 15,888 classes reachable
  22,701 (66.51%) of 34,131 fields reachable
  68,679 (61.81%) of 111,106 methods reachable
     786 classes,   281 fields, and 3,805 methods registered for reflection
      63 classes,    69 fields, and    55 methods registered for JNI access
       5 native libraries: -framework CoreServices, -framework Foundation, dl, pthread, z
[3/7] Building universe...                                                                               (5.2s @ 2.32GB)
[4/7] Parsing methods...      [***]                                                                      (5.0s @ 2.78GB)
[5/7] Inlining methods...     [***]                                                                      (3.1s @ 1.19GB)
[6/7] Compiling methods...    [******]                                                                  (42.0s @ 1.24GB)
[7/7] Creating image...                                                                                  (8.5s @ 3.50GB)
  30.93MB (50.06%) for code area:    45,412 compilation units
  30.57MB (49.47%) for image heap:  325,090 objects and 292 resources
 293.46KB ( 0.46%) for other data
  61.79MB in total
------------------------------------------------------------------------------------------------------------------------
Top 10 packages in code area:                               Top 10 object types in image heap:
   1.73MB sun.security.ssl                                     7.06MB byte[] for code metadata
   1.07MB java.util                                            3.56MB java.lang.Class
 784.08KB com.sun.crypto.provider                              3.06MB java.lang.String
 640.49KB org.apache.tomcat.util.net                           2.91MB byte[] for embedded resources
 597.39KB java.lang.invoke                                     2.61MB byte[] for java.lang.String
 575.77KB org.apache.catalina.core                             2.51MB byte[] for general heap data
 501.10KB org.apache.coyote.http2                              1.21MB com.oracle.svm.core.hub.DynamicHubCompanion
 485.00KB java.lang                                          805.74KB byte[] for reflection metadata
 478.21KB sun.security.x509                                  632.36KB java.lang.String[]
 470.01KB java.util.concurrent                               585.94KB java.util.HashMap$Node
  23.38MB for 587 more packages                                4.74MB for 2974 more object types
------------------------------------------------------------------------------------------------------------------------
                        5.4s (4.4% of total time) in 35 GCs | Peak RSS: 6.12GB | CPU load: 3.60
------------------------------------------------------------------------------------------------------------------------
Produced artifacts:
 /Users/ddobrin/work/dan/native-spring-cloud-run-with-graalvm/target/hello-function (executable)
 /Users/ddobrin/work/dan/native-spring-cloud-run-with-graalvm/target/hello-function.build_artifacts.txt (txt)
========================================================================================================================
```

Please note the build latency reduction [Prod/Dev]:
```
GraalVM 22.2.0 - significant improvement vs 22.1.0
[6/7] Compiling methods...    [********]                                (63.2s @ 3.83GB)
[6/7] Compiling methods...    [******]                                  (42.0s @ 1.24GB)

GraalVM 22.1.0
[6/7] Compiling methods...    [******]                                  (44.8s @ 4.28GB)
[6/7] Compiling methods...    [*****]                                   (29.0s @ 3.49GB)
```
... as well as lower RSS memory usage and lower CPU load
```
GraalVM 22.2.0 - significant improvement vs 22.1.0
6.0s (3.7% of total time) in 34 GCs | Peak RSS: 6.06GB | CPU load: 3.86
5.4s (4.4% of total time) in 35 GCs | Peak RSS: 6.12GB | CPU load: 3.60

GraalVM 22.1.0
19.2s (12.3% of total time) in 37 GCs | Peak RSS: 7.29GB | CPU load: 3.82
18.3s (11.0% of total time) in 33 GCs | Peak RSS: 6.93GB | CPU load: 3.40
```

# Analyze

## App and Docker container image sizes

What can we learn from comparing JVM and native images? This chapter does not intend to go into the details of JVM vs Native, that area is left for another workshop.
In here the intention is to compare the numbers for the latest versions of Java LTS, GraalVM and Spring/

Native images are larger, however they are self-contained and do not require a JVM to run:
```text
-rw-r--r--   1 ddobrin  primarygroup  20513323  1 Aug 12:19 hello-function-1.0.0-exec.jar
-rwxr-xr-x   1 ddobrin  primarygroup  63480256  1 Aug 12:22 hello-function
```

Docker images for JVM based apps are quite large, while native images are significantly smaller, as the Docker container does not require a JRE to run the Java app.
The images shown below have been build with the [Paketo Java Buildpack](https://github.com/paketo-buildpacks/java) and the [Paketo Native Java Buildpack](https://github.com/paketo-buildpacks/native-image) respectively, withtou the need to build a Dockerfile.
```text
hello-function-native                                                                  r17-no-compression                                                 0bedca53cafb   42 years ago    92.5MB
hello-function-native                                                                  r17-upx                                                            ea422a1b1e07   42 years ago    41.9MB
hello-function-jvm                                                                     r17                                                                021a6af1060f   42 years ago    278MB
```

By default, Docker images for Native Java apps are not compressed, however you can compress them using `UPX` or `GZEXE`.
To compress the image, you must specify the compression method in the Maven profile:
```xml
<profile>
    <id>native-image</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <classifier>${repackage.classifier}</classifier>
                    <image>
                        <builder>paketobuildpacks/builder:tiny</builder>
                        <name>${project.artifactId}-${native-image-type}:${build.version}</name>
                        <env>
                            <BP_NATIVE_IMAGE>1</BP_NATIVE_IMAGE>
                            <BP_JVM_VERSION>17</BP_JVM_VERSION>
                            <!-- <none> is default -->
                            <!-- upx or gzexe are supported options -->
                            <BP_BINARY_COMPRESSION_METHOD>upx</BP_BINARY_COMPRESSION_METHOD>
                            <!-- end compression -->
                            <BP_NATIVE_IMAGE_BUILD_ARGUMENTS>
                                <removeSpelSupport>true</removeSpelSupport>
                                <removeYamlSupport>true</removeYamlSupport>
                            </BP_NATIVE_IMAGE_BUILD_ARGUMENTS>
                        </env>
                    </image>
                </configuration>
...
```

## Start-up latency
JVM applications start-up latency is significantly improved in native images.

JVM based app:
```shell
java -jar target/hello-function-1.0.0-exec.jar
...
2022-08-01 12:34:52.650  INFO 84442 --- [           main] c.e.h.SpringNativeFunctionApplication    : Started SpringNativeFunctionApplication in 2.047 seconds (JVM running for 2.464)
...
```

Native java app:
```shell
target/hello-function
...
2022-08-01 12:35:41.942  INFO 84640 --- [           main] c.e.h.SpringNativeFunctionApplication    : Started SpringNativeFunctionApplication in 0.064 seconds (JVM running for 0.067)
...
```

## RSS memory consumption
RSS memory consumption is significantly lower in Native Java apps

JVM based app:
```shell
# memory usage in MB
ps -o pid,rss,command | grep --color hello-function | awk '{$2=int($2/1024)"M";}{ print;}'

# before running an HTTP request
79764 245M /usr/bin/java -jar target/hello-function-1.0.0-exec.jar
# after running 5 HTTP requests
79764 271M /usr/bin/java -jar target/hello-function-1.0.0-exec.jar
```

Native Java app:
```shell
# memory usage in MB
ps -o pid,rss,command | grep --color hello-function | awk '{$2=int($2/1024)"M";}{ print;}'

# before running an HTTP request
80061 49M target/hello-function

# after running 5 HTTP requests
84640 54M ./target/hello-function
```

## Changelog
* August 1, 2022: Updated with GraalVM 22.2.0, Java 17, buildtools 0.9.13
* July 22, 2022: Updated with GraalVM 22.1.0, Java 17, Spring Boot 2.7.2. buildtools 0.9.11
* April 30, 2022: Updated with GraalVM 22.1.0, Java 17, Spring Boot 2.6.6
* May 2, 2022: Deploy to Cloud Run, analysis

