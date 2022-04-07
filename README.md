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

## FAQ

* What about directories?
  * What about 'em? 😆 But seriously - this application ignores them. My requirements are simple - watch a flat directory and upload to a bucket.