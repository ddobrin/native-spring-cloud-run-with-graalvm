[Last updated: April 30, 2022]

Learn how to build JVM and Native Java images with Spring Native and GraalVM, then deploy them on the CloudRun serverless compute platform.

The sample app provides a simple `Hello` web app based on Spring Boot and Spring Cloud Functions. 
The focus is on building and deploying native images to a serverless platform. The update to this repo follows the latest release of GraalVM 22.1.0 with Java 17 support, which brings a series of major improvements.

Intel and Apple Silicon builds are supported for the different build methods.

[Full details - GraalVM 22.1: Developer experience improvements, Apple Silicon builds, and more](https://medium.com/graalvm/graalvm-22-1-developer-experience-improvements-apple-silicon-builds-and-more-b7ac9a0f6066)

`Dive into`:
1. Build 
    * JVM & Native `Application image` with the Spring Boot Maven plugin and GraalVM
    * JVM & Native `Docker image` using Java & Java Native Paketo Buildpacks 
    * JVM & Native `Docker image` using Dockerfiles 
2. Generate 
   * Native Tests
3. Deploy
    * Deploy to Google Cloud Run
4. Develop
   * First look at Quick Build Mode for Developers
5. Compare
   * Start-up latency
   * App & Container image sizes 

`Tools and versions in use:`
* Spring Boot 2.6.6
* Spring Native 0.11.4 
* Native Buildtools 0.9.11
* Spring Cloud 2021.1.0
* OpenJDK
  * openjdk version "17.0.3" 2022-04-19
* GraalVM CE
  * OpenJDK Runtime Environment GraalVM CE 22.1.0 (build 17.0.3+7-jvmci-22.1-b06)
  * OpenJDK 64-Bit Server VM GraalVM CE 22.1.0 (build 17.0.3+7-jvmci-22.1-b06, mixed mode, sharing)
* Java compatibility level: Java 17

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
* 
### Build code as a JVM Docker image using the Spring Boot Maven plugin and Java Paketo Buildpacks
```bash 
# build image with default configuration
./mvnw clean spring-boot:build-image

# build image with the CNB Paketo buildpack of your choice
./mvnw clean spring-boot:build-image -Pjvm-image 

# start Docker image
docker run -p 8080:8080 hello-function-jvm:latest

# test Docker image locally
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a JVM app running in a container"
```

### Build code as a Spring Native Docker image using the Spring Boot Maven plugin and the Java Native Paketo Buildpacks
```bash 
# build image with the CNB Paketo buildpack of your choice
# generating native tests is skipped for reduced build latency
./mvnw clean spring-boot:build-image -Pnative-image -DskipTests

# start Docker image
docker run -p 8080:8080 hello-function-native:latest

# test Docker image locally
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Native app running in a container"
```

### Build code as a JVM Docker image using a Dockerfile
Use the `Dockerfile` provided in the repo.
```bash
# build image with a Dockerfile
./mvnw clean package -Pjvm -DskipTests 
docker build -f Dockerfile . -t hello-function:dockerfile

# start Docker image
docker run -p 8080:8080 hello-function:dockerfile

# test Docker image locally
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a JVM app running in a container"
```

### Build code as a Native Java Docker image using a Dockerfile
Use the `Dockerfile-native-mvn` file provided in the repo.
```text
FROM ghcr.io/graalvm/graalvm-ce:22.1.0 as builder

WORKDIR /app
COPY . /app

RUN gu install native-image

RUN ./mvnw package -Pnative -DskipTests

COPY /target/hello-function /hello-function

FROM scratch

COPY --from=builder /hello-function /hello-function

ENTRYPOINT ["/hello-function"]
```
Build the Native Java Docker image
```bash
# build image with a Dockerfile
docker build -f Dockerfile-native-mvn . -t hello-function-native:dockerfile-mvn 

# start Docker image
docker run -p 8080:8080 hello-function-native:dockerfile-mvn 

# test Docker image locally
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Native app running in a container"
```

### Build code as a Native Java Docker image using a Dockerfile and compressing the image with UPX 
Use the `Dockerfile-native-mvn-upx` file provided in the repo.
```text
FROM ghcr.io/graalvm/graalvm-ce:latest as builder

WORKDIR /app
COPY . /app

RUN gu install native-image

RUN ./mvnw package -Pnative -DskipTests

COPY /target/hello-function /hello-function

# Note the usage of UPX to compress the image
RUN curl -L -o xz.rpm https://www.rpmfind.net/linux/centos/8-stream/BaseOS/x86_64/os/Packages/xz-5.2.4-3.el8.x86_64.rpm
RUN rpm -iv xz.rpm

RUN curl -L -o upx-3.96-amd64_linux.tar.xz https://github.com/upx/upx/releases/download/v3.96/upx-3.96-amd64_linux.tar.xz
RUN tar -xvf upx-3.96-amd64_linux.tar.xz
RUN upx-3.96-amd64_linux/upx -7 /hello-function
# 

FROM scratch

COPY --from=builder /hello-function /hello-function

ENTRYPOINT ["/hello-function"]
```
Build the Native Java Docker image
```bash
# build image with a Dockerfile
docker build -f Dockerfile-native-mvn-upx . -t hello-function-native:dockerfile-mvn-upx 

# start Docker image
docker run -p 8080:8080 hello-function-native:dockerfile-mvn-upx 

# test Docker image locally
curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Native app running in a container and compressed with UPX"
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
PROJECT_ID           NAME           PROJECT_NUMBER
dans-project-xyz  Dan's Project     123456789

# set it if not already set
gcloud config set project dans-project-xyz

# push the image(s) to the Container Registry
docker push gcr.io/dans-project-331502/hello-function-jvm:latest
docker push gcr.io/dans-project-331502/hello-function-native:latest
```

As a developer, the service can be deployed, for testing, unauthorized, while providing full access to the service.
However, developers, in addition to administrative actions such as creating, updating, and deleting services, often want to test services privately before releasing them.

You have to ensure that you grant permissions to access the services you are authenticating to. 

For development, the easiest way to test a service that requires authentication is to use a tool like curl and pass an auth token in the Authorization header.
This is NOT recommended outside of testing, and proper tokens should be generated during deployment.

#### Deploy with unauthenticated users:
```shell
# deploy the container to CloudRun
# Note that we are specifying:
#    app name - hello-function
#    region - us-central1
#    memory allocated for the process - 1Gi
#    simple apps allow all users, unauthenticated, best practice is to set service accounts up
gcloud run deploy hello-function \
     --image=gcr.io/dans-project-331502/hello-function-jvm \ 
     --region us-central1 \
     --memory 1Gi \
     --allow-unauthenticated
..
Deploying container to Cloud Run service [hello-function] in project [dans-project-331502] region [us-central1]
✓ Deploying... Done.                                                                                                                                       
  ✓ Creating Revision...                                                                                                                                   
  ✓ Routing traffic...                                                                                                                                     
  ✓ Setting IAM Policy...                                                                                                                                  
Done.                                                                                                                                                      
Service [hello-function] revision [hello-function-00002-ceb] has been deployed and is serving 100 percent of traffic.
...

# check that the project has been successfully deployed 
# retrieve the URL where the app can be accessed and send a request
$ gcloud run services list
   SERVICE         REGION       URL                                             LAST DEPLOYED BY      LAST DEPLOYED AT
✔  hello-function  us-central1  https://hello-function-v6qqi65qxq-uc.a.run.app  abc@gmail.com  2021-11-13T20:32:37.995100Z

$ curl -w'\n' -H 'Content-Type: text/plain' https://hello-function-v6qqi65qxq-uc.a.run.app -d "from a Function"
Hello: from a Function, Source: from-function

# delete the service 
$ gcloud run services delete hello-function --region us-central1
```

#### Deploy with authenticated users:
```shell
# deploy the container to CloudRun
# Note that we are specifying:
#    app name - hello-function
#    region - us-central1
#    memory allocated for the process - 1Gi
#    simple apps allow all users, unauthenticated, best practice is to set service accounts up
$ gcloud run deploy hello-function \
     --image=gcr.io/dans-project-331502/hello-function-jvm \ 
     --region us-central1 \
     --memory 1Gi 
     
# do not allow unathenticated users at the prompt
$ Allow unauthenticated invocations to [hello-function] (y/N)?  N
     
...
Deploying container to Cloud Run service [hello-function] in project [dans-project-331502] region [us-central1]
✓ Deploying new service... Done.                                                                                                                           
  ✓ Creating Revision... Deploying Revision.                                                                                                               
  ✓ Routing traffic...                                                                                                                                     
Done.                                                                                                                                                      
Service [hello-function] revision [hello-function-00001-duz] has been deployed and is serving 100 percent of traffic.
Service URL: https://hello-function-v6qqi65qxq-uc.a.run.app
...

# retrieve the URL where the app can be accessed and send a request
gcloud run services list
   SERVICE         REGION       URL                                             LAST DEPLOYED BY      LAST DEPLOYED AT
✔  hello-function  us-central1  https://hello-function-v6qqi65qxq-uc.a.run.app  abc@gmail.com  2021-11-14T13:37:19.848539Z

# grant the Cloud Run Invoker role to the developer
$ gcloud run services add-iam-policy-binding hello-function \
    --member='user:abc@gmail.com' \
    --role='roles/run.invoker'   \
    --region us-central1            

Updated IAM policy for service [hello-function].
bindings:
- members:
  - user:abc@gmail.com
  role: roles/run.invoker
etag: BwXQv-XczNA=
version: 1
...

# developer can  print an identity token for the specified account to generate a token for development
# command: gcloud auth print-identity-token 

# for convenience, you can create an alias
$ alias gcurl='curl --header "Authorization: Bearer $(gcloud auth print-identity-token)"'

# you can use the new alias to curl the service
$ gcurl -w'\n' -H 'Content-Type: text/plain' https://hello-function-v6qqi65qxq-uc.a.run.app -d "from an authenticated Function"
Hello: from an authenticated Function, Source: from-function
```

#### Clean-up 
```shell
# delete the service 
$ gcloud run services delete hello-function --region us-central1

```

## Changelog
April 30, 2022: Updated with GraalVM 22.1.0, Java 17, Spring Boot 2.6.6

----------

## [Optional - How to use Quick Build Mode for Development

Quick Build mode significantly improves build latency by running the compiler in economy mode, with fewer optimizations, resulting in much faster builds.

This is a Development feature, not recommended for Production.
In Production, use the default compilation mode, which provides the best runtime performance and memory efficiency !

To enable quick build mode, add `-Ob (capital “O”, lower case “b”)` when building with the native-image utility.




## [Optional - CI/CD integration - Build a JVM / Native Docker image with KPACK OSS

To build an image with Java or Java Native Paketo Buildpacks with kpack, you can use the commands listed below.

To start, install the tools as follows:
* `kpack CLI` - https://github.com/vmware-tanzu/kpack-cli 
  * kpack commands - https://github.com/vmware-tanzu/kpack-cli/blob/master/docs/kp.md 
* `kpack` - https://github.com/pivotal/kpack 

## Building JVM Docker images
To build the JVM image with the Java Paketo Buildpack, please run:
```shell
$ kp image save hello-function-jvm \ 
    --tag <your-repo-prefix>/hello-function:jvm \ 
    --git https://github.com/ddobrin/native-spring-cloud-run-with-graalvm.git \
    --git-revision main \
    --cluster-builder base \ 
    --env BP_JVM_VERSION=11 \
    --env BP_MAVEN_BUILD_ARGUMENTS="-Dmaven.test.skip=true package spring-boot:repackage" \
    --wait 

* your-repo-prefix - prefix for your Container Registry. Ex. Docker-desktop hello-function:jvm, GCR gcr.io/pa-ddobrin/hello-function:jvm
* tag - image tag
* git - repo location 
* local-path - to build from a local download of the repo, replace "git" with "local-path"
        --local-path ~/native-spring-cloud-run-with-graalvm
* git-revision - the code branch in Git
* cluster-builder - the Paketo builder used to build the image
* BP_JVM_VERSION - Java version to build for, accepts 8, 11
* wait - if you wish to observe the build taking place
* BP_MAVEN_BUILD_ARGUMENTS - kpack/TBS works declaratively in K8s, therefore requires instructions for the `repackaging` goal to be triggered; local machine is imperative and `package` in pom.xml is sufficient. 
```

## Building Java Native Docker images
To build the JVM image with the Java Native Paketo Buildpack, please run:
```shell
$ kp image save hello-function-native \ 
    --tag <your-repo-prefix>/hello-function:native \ 
    --git https://github.com/ddobrin/native-spring-cloud-run-with-graalvm.git \
    --git-revision main \
    --cluster-builder tiny \ 
    --env BP_BOOT_NATIVE_IMAGE=1 \
    --env BP_JVM_VERSION=11 \
    --env BP_MAVEN_BUILD_ARGUMENTS="-Dmaven.test.skip=true package spring-boot:repackage" \
    --env BP_BOOT_NATIVE_IMAGE_BUILD_ARGUMENTS="-Dspring.spel.ignore=true -Dspring.xml.ignore=true -Dspring.native.remove-yaml-support=true --enable-all-security-services" \
    --wait 

* your-repo-prefix - prefix for your Container Registry. Ex. Docker-desktop hello-function:native, GCR gcr.io/pa-ddobrin/hello-function:native 
* tag - image tag
* git - repo location 
* local-path - to build from a local download of the repo, replace "git" with "local-path"
        --local-path ~/native-spring-cloud-run-with-graalvm
* git-revision - the code branch in Git
* cluster-builder - the Paketo builder used to build the image
* BP_BOOT_NATIVE_IMAGE - set to true builds a Spring Native image
* BP_JVM_VERSION - Java version to build for, accepts 8, 11
* wait - if you wish to observe the build taking place
* BP_MAVEN_BUILD_ARGUMENTS - kpack/TBS works declaratively in K8s, therefore requires instructions for the `repackaging` goal to be triggered; local machine is imperative and `package` in pom.xml is sufficient. 
* BP_BOOT_NATIVE_IMAGE_BUILD_ARGUMENTS - optimization arguments for the Native image to minimize image size
```