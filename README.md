This sample app provides a simple `Hello` web app based on Spring Boot and Spring Cloud Functions.

`Instructions are provided`:
* to build JVM or Native images for the same app
* provide multiple deployment options on Cloud Run

`Currently Tracked Versions:`
* Spring Boot 2.5.5 - as of September 2021
* Spring Native 0.10.4 (Spring Native Beta) - as of September 2021
* Native Buildtools 0.9.3 - as of September 2021
* OpenJDK
    * openjdk version "11.0.12" 2021-07-20
* GraalVM CE
    *  OpenJDK Runtime Environment GraalVM CE 21.2.0 (build 11.0.11+8-jvmci-21.1-b05)
    *  OpenJDK 64-Bit Server VM GraalVM CE 21.1.0 (build 11.0.12+6-jvmci-21.2-b08, mixed mode, sharing)
    
`This repo addresses the following topics`:
1. Build 
  * Build a JVM / Native app image with the Spring Boot plugin and GraalVM
  * Build a JVM / Native Docker image with Java and Java Native Paketo Buildpacks 
2. CI/CD integration - Build a JVM / Native Docker image with kpack
3. Deploy 
  * Run locally
  * Deploy to Google Cloud Run

Build Options:
* JVM application, leveraging OpenJDK
* Native Application, leveraging GraalVM

Deployment Models:
* Standalone web app
* Google Cloud Run

`Source code tree`:
```
src
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── hello
│   │               └── SpringNativeFunctionApplication.java
│   └── resources
│       ├── application.properties
│       ├── static
│       └── templates
└── test
    └── java
        └── com
            └── example
                └── hello
                    └── SpringNativeFunctionApplicationTests.java

# The function used in this app is available in SpringNativeFunctionApplication.java
```

# Build

Building the code with the Spring Boot Maven wrapper leverages the following Maven profiles:
* `native-image` - build a Spring Native image leveraging GraalVM
* `jvm-image` - build a Spring JVM-based image leveraging OpenJDK

Building an executable application with the GraalVM compiler leverages the following Maven profile and requires the installation of the GraalVM and the native-image builder utility:
* `native`

### Build code as a JVM app using the Spring Boot Maven plugin with embedded Netty HTTP server
```bash 
# build and run code using
$ ./mvnw clean package spring-boot:run

# test locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
```
### Build code as a Native JVM app using the GraalVM compiler with embedded Netty HTTP server
```bash 
# switch to the GraalVM JDK for this build
# ex, when using SDKman
$ sdk use java 21.2.0.r11-grl

# build and run code using
$ ./mvnw clean package -Pnative

# start the native executable
$ ./target/hello-function

# test locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
```

### Build code as a JVM image using the Spring Boot Maven plugin and Java Paketo Buildpacks
```bash 
# build image with default configuration
$ ./mvnw clean spring-boot:build-image

# build image with the CNB Paketo buildpack of your choice
$ ./mvnw clean spring-boot:build-image -Pjvm-image

# start Docker image
$ docker run -p 8080:8080 hello-function-jvm:latest

# test Docker image locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
```

### Build code as a Spring Native image using the Spring Boot Maven plugin and the Java Native Paketo Buildpacks
```bash 
# build image with the CNB Paketo buildpack of your choice
$ ./mvnw clean spring-boot:build-image -Pnative-image

# start Docker image
$ docker run -p 8080:8080 hello-function-native:latest

# test Docker image locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
```
# CI/CD integration - Build a JVM / Native Docker image with KPACK

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

# Deploy

## Cloud Run Deploy

Build the image as a JVM or native image and deploy it to Cloud Run from the command-line. You can also deploy directly from the GCP console.

**Note**: Please note the Project ID of the project where you wish to deploy your service

#### Build and Deploy
```shell
# authorize the user to GCP
$ gcloud auth list

# check if the project is set
$ gcloud projects list
PROJECT_ID           NAME           PROJECT_NUMBER
dans-project-331502  Dan's Project  1066531842407

# set it if not already set
$ gcloud config set project dans-project-331502

# push the image(s) to the Container Registry
$ docker push gcr.io/dans-project-331502/hello-function-jvm:latest
$ docker push gcr.io/dans-project-331502/hello-function-native:latest
```

As a developer, the service can be deployed, for testing, unauthorized, while providing full access to the service.
However, developer, in addition to administrative actions such as creating, updating, and deleting services, often want to test services privately before releasing them.

You have to ensure that you grant permissions to access the services you are authenticating to. 

For development, 
the easiest way to test a service that requires authentication is to use a tool like curl and pass an auth token in the Authorization header.
This is NOT recommended outside of testing, and proper tokens should be generated during deployment.

#### Deploy with unauthenticated users:
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
