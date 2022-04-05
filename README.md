## OCI Object Uploader 

The purpose of this project is to provide a tool to keep a local directory in sync with an Oracle Cloud Infrastructure Object Storage bucket. When running, this application watches the local directory for changes. 

* If a file was created or modified, then a `PutObjectRequest` is issued to upload the object.  
  * If the target bucket is private, a pre-authorized request (PAR) is issued (the PAR expiration duration is configurable)
* If a file is deleted, then a `DeleteObjectRequest` is issued to delete the object

Other things of note:

* Actions on hidden files are ignored
* Exceptions are caught and logged

What could cause an exception? Lots of things. For example, if you changed the name of a file in your local directory while this application was not running, and then tried to delete it, the delete would fail because the remote object would not be found via the new name.

Again, **this application only syncs while running**! It **will not sync any changes that happen offline**! It doesn't "catch up" to your offline changes. Ever.

It's **not** intended to be a full offline/backup/sync utility. It **is** intended to be a Proof of Concept and demo as well as an easy way to upload things like screenshots to a cloud bucket in an automated manner.

## Before You Get Started

You'll need a properly configured OCI CLI config file locally, installed in the default location. The OCI Java SDK will use that config file as your auth provider for the Object Storage Client. If you don't have the CLI installed, [please do so](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/cliinstall.htm) before you run this application.

## Running the App

[Download a release](https://github.com/recursivecodes/oci-object-uploader/releases/latest) for your OS, make it executable, and run it. 

### MacOS/Linux
You'll need to pass in some values (and you do have the [CLI configured](#before-you-get-started) already, right?). 

```shell
$ ./oci-object-uploader \
  -Duploader.local-upload-dir=/tmp/test \
  -Duploader.oci.namespace=toddrsharp \
  -Duploader.oci.bucket=screenshots \
  -Duploader.oci.region=us-phoenix-1 \
  -Duploader.oci.par-duration-hours=24
```

The application will log all actions to the console. If you'd prefer, you can run this application in the background and log to a file:

```shell
$ nohup ./oci-object-uploader \
  -Duploader.local-upload-dir=/tmp/test \
  -Duploader.oci.namespace=toddrsharp \
  -Duploader.oci.bucket=screenshots \
  -Duploader.oci.region=us-phoenix-1 \
  -Duploader.oci.par-duration-hours=24 > /tmp/oci-object-uploader.log &
```

Optionally run by setting environment variables (instead of passing arguments):

```shell
$ export UPLOADER_LOCAL_UPLOAD_DIR = "/tmp/test"
$ export UPLOADER_OCI_NAMESPACE = "toddrsharp"
$ export UPLOADER_OCI_BUCKET = "screenshots"
$ export UPLOADER_OCI_REGION = "us-phoenix-1"
$ export UPLOADER_OCI_PAR_DURATION_HOURS = 24

$ ./oci-object-uploader
```

### Windows (PowerShell)

```shell
$Env:UPLOADER_LOCAL_UPLOAD_DIR = "c:\Users\opc\AppData\Local\Temp"
$Env:UPLOADER_OCI_NAMESPACE = "toddrsharp"
$Env:UPLOADER_OCI_BUCKET = "screenshots"
$Env:UPLOADER_OCI_REGION = "us-phoenix-1"
$Env:UPLOADER_OCI_PAR_DURATION_HOURS = 24

.\oci-object-uploader-0.1-windows.exe
```

## Building Your Own Native Image

If you want to build your own native image, clone the repo and build with `./gradlew nativeCompile`. 

## Running as a Jar

If for some reason you'd prefer to run it as a Jar file, clone the repo and build it with `./gradlew build` and run the Jar.

```shell
$ java -jar \
  -Duploader.local-upload-dir=/tmp/test \
  -Duploader.oci.namespace=toddrsharp \
  -Duploader.oci.bucket=screenshots \
  -Duploader.oci.region=us-phoenix-1 \
  -Duploader.oci.par-duration-hours=24 \
  build/libs/oci-object-uploader-0.1-all.jar
```

## Sample Output

The application will log some general information at startup, and will log all actions as it runs.  

Here is a log file that illustrates startup, the addition of a new file to the folder, that file being changed (image resized), and finally the image being deleted.

```shell
/Users/trsharp/.sdkman/candidates/java/11.0.12-oracle/bin/java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:59846,suspend=y,server=n -Dmicronaut.environments=todd -javaagent:/Users/trsharp/Library/Caches/JetBrains/IntelliJIdea2021.2/groovyHotSwap/gragent.jar -javaagent:/Users/trsharp/Library/Caches/JetBrains/IntelliJIdea2021.2/captureAgent/debugger-agent.jar -Dfile.encoding=UTF-8 -classpath /Users/trsharp/Projects/micronaut/ociobjectuploader/build/classes/java/main:/Users/trsharp/Projects/micronaut/ociobjectuploader/build/resources/main:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.graalvm.nativeimage/svm/22.0.0.2/57c2d7627e8d574a989c327656e1344fa94a2553/svm-22.0.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-http-server-netty/3.4.0/371fb1a95bcf2dbcc46f6087538d1be72eda49e3/micronaut-http-server-netty-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-runtime/3.4.0/28d806131636e6a7887aa5c18ae75698f0b3091e/micronaut-runtime-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-jackson-databind/3.4.0/674463cab7780762c6bd81e7988dfc01f0d5a2a2/micronaut-jackson-databind-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut.oraclecloud/micronaut-oraclecloud-sdk/2.1.1/b1ad778e5fbf2d4932b0c84f2d846f26134e4630/micronaut-oraclecloud-sdk-2.1.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.oracle.oci.sdk/oci-java-sdk-objectstorage/2.20.0/201722c739d9d2aea768080e3ac8aff27bb95f44/oci-java-sdk-objectstorage-2.20.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.sshtools/two-slices/0.0.2-SNAPSHOT/4fbd4e89dd1916197d6fce01a6e7a29c53399b80/two-slices-0.0.2-SNAPSHOT.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-validation/3.4.0/937758ab06ca057b0c8f07889b1f3b72b698ffdc/micronaut-validation-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-inject/3.4.0/e726b09443a145d93cabb356bf4eb1f2637fb7ef/micronaut-inject-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.graalvm.nativeimage/objectfile/22.0.0.2/f348706cf5613cc3f5fb778857e449f91e40e557/objectfile-22.0.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.graalvm.nativeimage/pointsto/22.0.0.2/4a2bed2be5c8005c8b377be6a84b5dacf1644963/pointsto-22.0.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.graalvm.nativeimage/native-image-base/22.0.0.2/7f023ed7923573ff6a0ebc68849eced1637cefb2/native-image-base-22.0.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.graalvm.compiler/compiler/22.0.0.2/739e0c551d66c6a149738c0c8f407c2bd48b9b9d/compiler-22.0.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.graalvm.sdk/graal-sdk/22.0.0.2/3e6a30de6d2e3ea94ec5976ab37a850f96d78ee3/graal-sdk-22.0.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-http-server/3.4.0/b13e4325ae8503058bd782d0227759aba7e2cee6/micronaut-http-server-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-http-netty/3.4.0/56a53725799a9562df95061f14f8db49344ca792/micronaut-http-netty-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-http/4.1.73.Final/1ceeac4429b9bd517dc05e376a144bbe6b6bd038/netty-codec-http-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/1.7.29/e56bf4473a4c6b71c7dd397a833dce86d1993d9d/slf4j-api-1.7.29.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-context/3.4.0/7cff05e6f8f0bec9f3634232d64531e6a6b02a30/micronaut-context-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-aop/3.4.0/3a12bc1022a0c4b876b78999d2c10f25b1f436d3/micronaut-aop-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-http/3.4.0/35bbcde05d6febdd05af00a13d6537f42ea3c173/micronaut-http-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-jackson-core/3.4.0/6504deb773b035c2d56f18e684e77cbccb7e925f/micronaut-jackson-core-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.datatype/jackson-datatype-jsr310/2.13.2/cddd9380efd4b81ea01e98be8fbdc9765a81793b/jackson-datatype-jsr310-2.13.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.datatype/jackson-datatype-jdk8/2.13.2/95f59cf63c3aadc1549578254af839a9c42ae84f/jackson-datatype-jdk8-2.13.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut.oraclecloud/micronaut-oraclecloud-common/2.1.1/5028a54722c077d0ea3292a611ce110d13edcd68/micronaut-oraclecloud-common-2.1.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.oracle.oci.sdk/oci-java-sdk-objectstorage-extensions/2.20.0/91a079196384c7ad4e6b089ded26fc8e44c6b123/oci-java-sdk-objectstorage-extensions-2.20.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.oracle.oci.sdk/oci-java-sdk-objectstorage-generated/2.20.0/738ecb535b174cd99c6984a3d8f64a8d403698e7/oci-java-sdk-objectstorage-generated-2.20.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/jakarta.ws.rs/jakarta.ws.rs-api/2.1.6/1dcb770bce80a490dff49729b99c7a60e9ecb122/jakarta.ws.rs-api-2.1.6.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-core-reactive/3.4.0/a0485b9f4da56ae17923fb3ffa00a796d0cc058/micronaut-core-reactive-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/javax.validation/validation-api/2.0.1.Final/cb855558e6271b1b32e716d24cb85c7f583ce09e/validation-api-2.0.1.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/jakarta.annotation/jakarta.annotation-api/2.0.0/f3cd84cc45f583a0fdc42a8156d6c5b98d625c1a/jakarta.annotation-api-2.0.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/javax.annotation/javax.annotation-api/1.3.2/934c04d3cfef185a8008e7bf34331b79730a9d43/javax.annotation-api-1.3.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-core/3.4.0/eab5312ef7ab4d35d96126a70be803781fc6fdaa/micronaut-core-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.yaml/snakeyaml/1.30/8fde7fe2586328ac3c68db92045e1c8759125000/snakeyaml-1.30.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/jakarta.inject/jakarta.inject-api/2.0.1/4c28afe1991a941d7702fe1362c365f0a8641d1e/jakarta.inject-api-2.0.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.graalvm.truffle/truffle-api/22.0.0.2/cfa3a29ea8c5e1c5d710d71484374b411d6f17f8/truffle-api-22.0.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-websocket/3.4.0/1d897b869618028c1aa1744327969a06a8f5188c/micronaut-websocket-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-router/3.4.0/68d244984be58b5fbcd3b75d20c916a0d295d1db/micronaut-router-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-buffer-netty/3.4.0/30ecd7989504201f4facca377d84cb0b2a37b7ec/micronaut-buffer-netty-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-http2/4.1.73.Final/eb145bc31fd32a20fd2a3e8b30736d2e0248b0c/netty-codec-http2-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-handler/4.1.73.Final/1a2231c0074f88254865c3769a4b5842939ea04d/netty-handler-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec/4.1.73.Final/9496a30a349863a4c6fa10d5c36b4f3b495d3a31/netty-codec-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport/4.1.73.Final/abb155ddff196ccedfe85b810d4b9375ef85fcfa/netty-transport-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-buffer/4.1.73.Final/244a569c9aae973f6f485ac9801d79c1eca36daa/netty-buffer-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-common/4.1.73.Final/27731b58d741b6faa6a00fa3285e7a55cc47be01/netty-common-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-json-core/3.4.0/656d61bf238dbc4ffb121064d7795aa217b47927/micronaut-json-core-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-annotations/2.13.2/ec18851f1976d5b810ae1a5fcc32520d2d38f77a/jackson-annotations-2.13.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-core/2.13.2/a6a0e0620d51833feffc67bccb51937b2345763/jackson-core-2.13.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-databind/2.13.2/926e48c451166a291f1ce6c6276d9abbefa7c00f/jackson-databind-2.13.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.oracle.oci.sdk/oci-java-sdk-common/2.20.0/78fc957f26c40ae05f19f7e3e3e169df132a8b74/oci-java-sdk-common-2.20.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.reactivestreams/reactive-streams/1.0.3/d9fb7a7926ffa635b3dcaa5049fb2bfa25b3e7d0/reactive-streams-1.0.3.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.micronaut/micronaut-http-client-core/3.4.0/f1036586b402fd245ca636781dc8776558c8ad4f/micronaut-http-client-core-3.4.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-resolver/4.1.73.Final/bfe83710f0c1739019613e81a06101020ca65def/netty-resolver-4.1.73.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.netty/netty-tcnative-classes/2.0.46.Final/9937a832d9c19861822d345b48ced388b645aa5f/netty-tcnative-classes-2.0.46.Final.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.jersey.media/jersey-media-json-jackson/2.34/52c3dce0e910eb8f7ce649f44efada7e26d8996a/jersey-media-json-jackson-2.34.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.oracle.oci.sdk/oci-java-sdk-circuitbreaker/2.20.0/c4d60286c7c21353d9f6b562bb1f22166eab6563/oci-java-sdk-circuitbreaker-2.20.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.jersey.connectors/jersey-apache-connector/2.34/fec563e0fc3650846ba8d2a2b815569ffe0ab519/jersey-apache-connector-2.34.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.jersey.core/jersey-client/2.34/f9c6c7c6429a1ede8e73cf077bdca28ebff67681/jersey-client-2.34.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.jersey.inject/jersey-hk2/2.34/15bb093c57c0cac880ad7b474f7fd653cac840a1/jersey-hk2-2.34.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/31.0.1-jre/119ea2b2bc205b138974d351777b20f02b92704b/guava-31.0.1-jre.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.google.code.findbugs/jsr305/3.0.2/25ea2e8b0c338a877313bd4672d3fe056ea78f0d/jsr305-3.0.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.github.resilience4j/resilience4j-circuitbreaker/1.2.0/ebec09ed06111475434bc41d7b47bf51457e5d41/resilience4j-circuitbreaker-1.2.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.apache.httpcomponents/httpclient/4.5.13/e5f6cae5ca7ecaac1ec2827a9e2d65ae2869cada/httpclient-4.5.13.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/commons-codec/commons-codec/1.15/49d94806b6e3dc933dacbd8acb0fdbab8ebd1e5d/commons-codec-1.15.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/commons-io/commons-io/2.8.0/92999e26e6534606b5678014e66948286298a35c/commons-io-2.8.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-lang3/3.8.1/6505a72a097d9270f7a9e7bf42c4238283247755/commons-lang3-3.8.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.bouncycastle/bcpkix-jdk15on/1.70/f81e5af49571a9d5a109a88f239a73ce87055417/bcpkix-jdk15on-1.70.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.bouncycastle/bcprov-jdk15on/1.70/4636a0d01f74acaf28082fb62b317f1080118371/bcprov-jdk15on-1.70.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.nimbusds/nimbus-jose-jwt/9.20/2ebcf4e025309d64a27f5519aa8c8d3858fb0b3a/nimbus-jose-jwt-9.20.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/net.minidev/json-smart/2.4.7/8d7f4c1530c07c54930935f3da85f48b83b3c109/json-smart-2.4.7.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.module/jackson-module-jaxb-annotations/2.13.2/e2f198c512f0f0ccbd6d618baecc9dde9975eadf/jackson-module-jaxb-annotations-2.13.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.jersey.core/jersey-common/2.34/cea85e85b1c19657ec3ea41b26351a53a40caa3c/jersey-common-2.34.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.jersey.ext/jersey-entity-filtering/2.34/b3112e9edc3b1a138b31862ed4aaa48f60cf1f73/jersey-entity-filtering-2.34.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.hk2.external/jakarta.inject/2.6.1/8096ebf722902e75fbd4f532a751e514f02e1eb7/jakarta.inject-2.6.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.hk2/hk2-locator/2.6.1/9dedf9d2022e38ec0743ed44c1ac94ad6149acdd/hk2-locator-2.6.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.javassist/javassist/3.25.0-GA/442dc1f9fd520130bd18da938622f4f9b2e5fba3/javassist-3.25.0-GA.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.google.guava/failureaccess/1.0.1/1dcf1de382a0bf95a3d8b0849546c88bac1292c9/failureaccess-1.0.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.google.guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/b421526c5f297295adef1c886e5246c39d4ac629/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/3.12.0/d5692f0526415fcc6de94bb5bfbd3afd9dd3b3e5/checker-qual-3.12.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.google.errorprone/error_prone_annotations/2.7.1/458d9042f7aa6fa9a634df902b37f544e15aacac/error_prone_annotations-2.7.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.google.j2objc/j2objc-annotations/1.3/ba035118bc8bac37d7eff77700720999acd9986d/j2objc-annotations-1.3.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.github.resilience4j/resilience4j-core/1.2.0/82dce5ba9f0cca3bbb694581abae0124ecceb828/resilience4j-core-1.2.0.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.vavr/vavr/0.10.2/dfd5101b17da36c32ae024b984e0b72712f01a35/vavr-0.10.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.apache.httpcomponents/httpcore/4.4.13/853b96d3afbb7bf8cc303fe27ee96836a10c1834/httpcore-4.4.13.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/commons-logging/commons-logging/1.2/4bfc12adfe4842bf07b657f0369c4cb522955686/commons-logging-1.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.bouncycastle/bcutil-jdk15on/1.70/54280e7195a7430d7911ded93fc01e07300b9526/bcutil-jdk15on-1.70.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/com.github.stephenc.jcip/jcip-annotations/1.0-1/ef31541dd28ae2cefdd17c7ebf352d93e9058c63/jcip-annotations-1.0-1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/net.minidev/accessors-smart/2.4.7/3970cfc505e6657ca60f3aa57c849f6043000d7a/accessors-smart-2.4.7.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/jakarta.xml.bind/jakarta.xml.bind-api/2.3.3/48e3b9cfc10752fba3521d6511f4165bea951801/jakarta.xml.bind-api-2.3.3.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/jakarta.activation/jakarta.activation-api/1.2.2/99f53adba383cb1bf7c3862844488574b559621f/jakarta.activation-api-1.2.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.hk2/osgi-resource-locator/1.0.3/de3b21279df7e755e38275137539be5e2c80dd58/osgi-resource-locator-1.0.3.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.hk2/hk2-api/2.6.1/114bd7afb4a1bd9993527f52a08a252b5d2acac5/hk2-api-2.6.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.hk2/hk2-utils/2.6.1/396513aa96c1d5a10aa4f75c4dcbf259a698d62d/hk2-utils-2.6.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.glassfish.hk2.external/aopalliance-repackaged/2.6.1/b2eb0a83bcbb44cc5d25f8b18f23be116313a638/aopalliance-repackaged-2.6.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.vavr/vavr-match/0.10.2/68f1af10052713fda01bfb1e5b831dcf6d826ab2/vavr-match-0.10.2.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.1/a99500cf6eea30535eeac6be73899d048f8d12a8/asm-9.1.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/ch.qos.logback/logback-classic/1.2.10/f69d97ef3335c6ab82fc21dfb77ac613f90c1221/logback-classic-1.2.10.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/io.projectreactor/reactor-core/3.4.15/28ccf513fe64709c8ded30ea3f387fc718db9626/reactor-core-3.4.15.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/1.7.36/6c62681a2f655b49963a5983b8b0950a6120ae14/slf4j-api-1.7.36.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/ch.qos.logback/logback-core/1.2.10/5328406bfcae7bcdcc86810fcb2920d2c297170d/logback-core-1.2.10.jar:/Users/trsharp/.gradle/caches/modules-2/files-2.1/org.slf4j/jcl-over-slf4j/1.7.36/d877e195a05aca4a2f1ad2ff14bfec1393af4b5e/jcl-over-slf4j-1.7.36.jar:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar codes.recursive.Application
Connected to the target VM, address: '127.0.0.1:59846', transport: 'socket'
 __  __ _                                  _   
|  \/  (_) ___ _ __ ___  _ __   __ _ _   _| |_ 
| |\/| | |/ __| '__/ _ \| '_ \ / _` | | | | __|
| |  | | | (__| | | (_) | | | | (_| | |_| | |_ 
|_|  |_|_|\___|_|  \___/|_| |_|\__,_|\__,_|\__|
  Micronaut (v3.4.0)

15:34:35.276 [main] INFO  i.m.context.env.DefaultEnvironment - Established active environments: [todd]
15:34:36.156 [main] INFO  com.oracle.bmc.Services - Registering new service: Services.BasicService(serviceName=OBJECTSTORAGE, serviceEndpointPrefix=objectstorage, serviceEndpointTemplate=https://objectstorage.{region}.{secondLevelDomain}, endpointServiceName=null)
15:34:36.717 [main] INFO  c.oracle.bmc.http.ApacheConfigurator - Setting connector provider to ApacheConnectorProvider
15:34:36.916 [main] INFO  com.oracle.bmc.util.JavaRuntimeUtils - Determined JRE version as Java_11
15:34:36.960 [main] INFO  com.oracle.bmc.Region - Loaded service 'OBJECTSTORAGE' endpoint mappings: {US_PHOENIX_1=https://objectstorage.us-phoenix-1.oraclecloud.com}
15:34:36.960 [main] INFO  c.o.b.o.ObjectStorageClient - Setting endpoint to https://objectstorage.us-phoenix-1.oraclecloud.com
15:34:37.626 [main] INFO  com.oracle.bmc.ClientRuntime - Using SDK: Oracle-JavaSDK/2.20.0
15:34:37.627 [main] INFO  com.oracle.bmc.ClientRuntime - User agent set to: Oracle-JavaSDK/2.20.0 (Mac OS X/11.2; Java/11.0.12; Java HotSpot(TM) 64-Bit Server VM/11.0.12+8-LTS-237)
15:34:38.356 [main] INFO  codes.recursive.FileWatcherService - Watching: /Users/trsharp/Documents/OCI Uploads
15:34:38.356 [main] INFO  codes.recursive.FileWatcherService - Objects will be uploaded to the 'screenshots' bucket in 'us-phoenix-1' (namespace: 'toddrsharp')
15:34:38.356 [main] INFO  codes.recursive.FileWatcherService - Since this bucket is private, pre-authenticated requests will be created with a duration of 720 hours
15:34:54.280 [main] INFO  codes.recursive.FileWatcherService - Action (ENTRY_CREATE) was applied to 'apple copy 6.jpg' in 'screenshots'.
15:34:54.280 [main] INFO  codes.recursive.FileWatcherService - URL: https://objectstorage.us-phoenix-1.oraclecloud.com/p/K8VZTXco-ER39uNrwx072onljbWUU0DuJDdtk68IR8AKkmAeBNx1v72-K5cI1M1K/n/toddrsharp/b/screenshots/o/apple%20copy%206.jpg
15:35:09.303 [main] INFO  codes.recursive.FileWatcherService - Action (ENTRY_MODIFY) was applied to 'apple copy 6.jpg' in 'screenshots'.
15:35:09.303 [main] INFO  codes.recursive.FileWatcherService - URL: https://objectstorage.us-phoenix-1.oraclecloud.com/p/E3iZO7RgzN4pC7UuKc4iEhbyLPAbtU1THdOpoRkO3RAWohTjjbYwhTibCdzHp5VG/n/toddrsharp/b/screenshots/o/apple%20copy%206.jpg
15:35:18.746 [main] INFO  codes.recursive.FileWatcherService - Action (ENTRY_DELETE) was applied to 'apple copy 6.jpg' in 'screenshots'.
```