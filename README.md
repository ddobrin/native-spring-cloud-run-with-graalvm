[Last updated: May 2, 2022]

**Learn how to build JVM and Native Java images with Spring Native and GraalVM, then deploy them on the Google CloudRun serverless compute platform.**


The sample app provides a simple `Hello` web app based on Spring Boot and Spring Cloud Functions. 
The focus is on building and deploying native images to a serverless platform. The update to this repo follows the latest release of GraalVM 22.1.0 with Java 17 support, which brings a series of significant improvements.

Intel and Apple Silicon builds are supported for the different build methods at this time.

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
   * First look at Quick Build Mode for Developers !
5. Analyze
   * Start-up latency
   * App & Container image sizes 

### `Java and library versions in use`
* Spring Boot 2.6.6
* Spring Native 0.11.4 
* Native Buildtools 0.9.11
* Spring Cloud 2021.1.0
* OpenJDK
  * Openjdk version "17.0.3" 2022-04-19
* GraalVM CE
  * OpenJDK Runtime Environment GraalVM CE 22.1.0 (build 17.0.3+7-jvmci-22.1-b06)
  * OpenJDK 64-Bit Server VM GraalVM CE 22.1.0 (build 17.0.3+7-jvmci-22.1-b06, mixed mode, sharing)
* Java compatibility level: Java 17

### `Known Issues`
* Java Native Buildpacks are failing on M1 Macs as of today; Native Java App images can be built cleanly
* UPX compression for M1 Macs is a known issue with a pending patch

# Installation
Install GraalVM from:
* [from GraalVM Github repo](https://github.com/graalvm/graalvm-ce-builds/releases)
* [using Homebrew](https://github.com/graalvm/homebrew-tap)
* [SDKMan](https://sdkman.io/): `sdk install java 22.1.0.r17-grl`

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
sdk use java 22.1.0.r17-grl

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
sdk use java 22.1.0.r17-grl

# test the app with native tests
./mvnw -Pnative test

# start the native test executable
$ ./target/native-tests
```

Observe the significant latency reduction in test execution:
```text
JUnit Platform on Native Image - report
----------------------------------------
...
2022-04-30 10:32:05.474  INFO 32772 --- [           main] e.h.SpringNativeFunctionApplicationTests : Started SpringNativeFunctionApplicationTests in 0.442 seconds (JVM running for 0.786)
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.616 s - in com.example.hello.SpringNativeFunctionApplicationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

Native Tests
```text
2022-04-30 10:27:12.439  INFO 32639 --- [           main] e.h.SpringNativeFunctionApplicationTests : Started SpringNativeFunctionApplicationTests in 0.022 seconds (JVM running for 0.035)
com.example.hello.SpringNativeFunctionApplicationTests > contextLoads() SUCCESSFUL

Test run finished after 30 ms
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
gcloud projects list
PROJECT_ID                    NAME              PROJECT_NUMBER
dan-workshop-project-332213   Dans Project      123456789

# set project ID if not already set
gcloud config set project <project id>
# ex:
gcloud config set project dan-workshop-project-332213

# tag the images and push them to the Container Registry 
docker tag hello-function-jvm:r17 gcr.io/dan-workshop-project-332213/hello-function-jvm:r17
docker tag hello-function-native:r17 gcr.io/dan-workshop-project-332213/hello-function-native:r17

# push the image(s) to the Container Registry
docker push gcr.io/dan-workshop-project-332213/hello-function-jvm:r17
docker push gcr.io/dan-workshop-project-332213/hello-function-native:r17
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
  --image=gcr.io/dan-workshop-project-332213/hello-function-jvm:r17 \
  --region us-central1 \
  --memory 1Gi --allow-unauthenticated

...
Deploying container to Cloud Run service [hello-function-jvm] in project [dan-workshop-project-332213] region [us-central1]
✓ Deploying... Done.                                                                                                                                       
  ✓ Creating Revision...                                                                                                                                   
  ✓ Routing traffic...                                                                                                                                     
  ✓ Setting IAM Policy...                                                                                                                                  
Done.                                                                                                                                                      
Service [hello-function-jvm] revision [hello-function-jvm-00001-soj] has been deployed and is serving 100 percent of traffic.

# deploy a Native Java image
gcloud run deploy hello-function-native \
  --image=gcr.io/dan-workshop-project-332213/hello-function-native:r17 \
  --region us-central1 \
  --memory 1Gi --allow-unauthenticated  

...
Service [hello-function-native] revision [hello-function-native-00001-xah] has been deployed and is serving 100 percent of traffic.
...

# check that the project has been successfully deployed 
# retrieve the URL where the app can be accessed and send a request
gcloud run services list
   SERVICE                   REGION       URL                                                       LAST DEPLOYED AT
✔  hello-function-jvm        us-central1  https://hello-function-jvm-ieuwkt6jkq-uc.a.run.app        2022-05-02T15:54:59.071861Z
✔  hello-function-native     us-central1  https://hello-function-native-ieuwkt6jkq-uc.a.run.app     2022-05-02T15:55:39.381280Z

# Test the JVM service
curl -w'\n' -H 'Content-Type: text/plain' https://hello-function-jvm-ieuwkt6jkq-uc.a.run.app -d "from a JVM Image"
Hello: from a JVM Image, Source: a Spring function !

# Test the Native Java service
curl -w'\n' -H 'Content-Type: text/plain' https://hello-function-native-ieuwkt6jkq-uc.a.run.app -d "from a Native Image"
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
  --image=gcr.io/dan-workshop-project-332213/hello-function-jvm:r17 \
  --region us-central1 \
  --memory 1Gi

# do not allow unathenticated users at the prompt
Allow unauthenticated invocations to [hello-function] (y/N)?  N

Deploying container to Cloud Run service [hello-function-jvm] in project [dan-workshop-project-332213] region [us-central1]
...
Service [hello-function-jvm] revision [hello-function-jvm-00001-vel] has been deployed and is serving 100 percent of traffic.
Service URL: https://hello-function-jvm-ieuwkt6jkq-uc.a.run.app

# deploy Native Java image
gcloud run deploy hello-function-native \
  --image=gcr.io/dan-workshop-project-332213/hello-function-jvm:r17 \
  --region us-central1 \
  --memory 1Gi  

# do not allow unathenticated users at the prompt
Allow unauthenticated invocations to [hello-function] (y/N)?  N

Deploying container to Cloud Run service [hello-function-native] in project [dan-workshop-project-332213] region [us-central1]
...
Service [hello-function-native] revision [hello-function-native-00001-fad] has been deployed and is serving 100 percent of traffic.
Service URL: https://hello-function-native-ieuwkt6jkq-uc.a.run.app


# retrieve the URL where the app can be accessed and send a request
gcloud run services list
   SERVICE               REGION       URL                                                     LAST DEPLOYED AT
✔  hello-function-jvm    us-central1  https://hello-function-jvm-ieuwkt6jkq-uc.a.run.app      2022-05-02T16:08:19.929918Z
✔  hello-function-native us-central1  https://hello-function-native-ieuwkt6jkq-uc.a.run.app   2022-05-02T16:10:24.242963Z

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
gcurl -w'\n' -H 'Content-Type: text/plain' https://hello-function-jvm-ieuwkt6jkq-uc.a.run.app -d "from a JVM Image"
Hello: from a JVM Image, Source: a Spring function !

# invoke the Native image service
gcurl -w'\n' -H 'Content-Type: text/plain' https://hello-function-native-ieuwkt6jkq-uc.a.run.app -d "from a Native Image"
Hello: from a Native Image, Source: a Spring function !
```

#### Clean-up 
```shell
# delete the service 
gcloud run services delete hello-function-jvm --region us-central1
gcloud run services delete hello-function-native --region us-central1

```

## Changelog
April 30, 2022: Updated with GraalVM 22.1.0, Java 17, Spring Boot 2.6.6
May 2, 2022: Deploy to Cloud Run

----------

## [Optional - How to use Quick Build Mode for Development

Quick Build mode significantly improves build latency by running the compiler in economy mode, with fewer optimizations, resulting in much faster builds.

This is a Development feature, not recommended for Production.
In Production, use the default compilation mode, which provides the best runtime performance and memory efficiency !

To enable quick build mode, add `-Ob (capital “O”, lower case “b”)` when building with the native-image utility.




