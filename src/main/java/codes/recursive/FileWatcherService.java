package codes.recursive;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.Bucket;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.GetBucketRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import com.oracle.bmc.objectstorage.responses.GetBucketResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Singleton
public class FileWatcherService {
    private static final Logger LOG = LoggerFactory.getLogger(FileWatcherService.class);
    public final ObjectStorageClient objectStorageClient;
    private final String uploadDir;
    private final String bucket;
    private final String namespace;
    private final String region;
    private final Integer parDurationHours;
    private final Boolean isPrivateBucket;

    public FileWatcherService(
            ObjectStorageClient objectStorageClient,
            @Property(name = "local-upload-dir") String uploadDir,
            @Property(name = "oci-bucket") String bucket,
            @Property(name = "oci-namespace") String namespace,
            @Property(name = "oci-region") String region,
            @Property(name = "oci-par-duration-hours") Integer parDurationHours
    ) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        this.objectStorageClient = objectStorageClient;
        this.uploadDir = uploadDir;
        this.bucket = bucket;
        this.namespace = namespace;
        this.region = region;
        this.parDurationHours = parDurationHours;

        // cache bucket visibility
        GetBucketResponse getBucketResponse = objectStorageClient.getBucket(GetBucketRequest.builder().bucketName(bucket).namespaceName(namespace).build());
        this.isPrivateBucket = getBucketResponse.getBucket().getPublicAccessType().equals(Bucket.PublicAccessType.NoPublicAccess);
    }

    public void watch() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(uploadDir);
        LOG.info("Watching: {}", uploadDir);
        path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                String objectName = event.context().toString();
                String filePath = uploadDir + "/" + objectName;
                File changedFile = new File(filePath);
                String contentType = Files.probeContentType(changedFile.toPath());
                String baseUrl = "https://objectstorage." + region + ".oraclecloud.com";
                String objectUrl;

                // if this is a modify/create event, upload the object
                if (!StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())){
                    // upload the object
                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                    .objectName(event.context().toString())
                                    .namespaceName(namespace)
                                    .bucketName(bucket)
                                    .putObjectBody(new FileInputStream(changedFile))
                                    .contentType(contentType)
                                    .build();
                    PutObjectResponse putObjectResponse = objectStorageClient.putObject(putObjectRequest);

                    if(isPrivateBucket) {
                        // get a pre-authenticated request for the uploaded object
                        CreatePreauthenticatedRequestDetails requestDetails = CreatePreauthenticatedRequestDetails.builder()
                                .name("PAR_" + UUID.randomUUID().toString())
                                .bucketListingAction(PreauthenticatedRequest.BucketListingAction.Deny)
                                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                                .objectName(objectName)
                                .timeExpires(Date.from(LocalDateTime.now().plusHours(parDurationHours).atZone(ZoneId.systemDefault()).toInstant()))
                                .build();
                        CreatePreauthenticatedRequestRequest preAuthenticatedRequest = CreatePreauthenticatedRequestRequest.builder()
                                .bucketName(bucket)
                                .namespaceName(namespace)
                                .createPreauthenticatedRequestDetails(requestDetails)
                                .build();
                        CreatePreauthenticatedRequestResponse preAuthenticatedRequestResponse = objectStorageClient.createPreauthenticatedRequest(preAuthenticatedRequest);
                        objectUrl = baseUrl + preAuthenticatedRequestResponse.getPreauthenticatedRequest().getAccessUri();
                    }
                    else {
                        objectUrl = baseUrl +  "/n/" + namespace + "/b/" + bucket + "/o/" + objectName.replaceAll(" ", "%20");
                    }

                    // copy URL to clipboard and pop desktop notification
                    // depends on https://github.com/vjeantet/alerter
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    String message = "'" + objectName + "' has been uploaded to '" + bucket + "'. URL has been copied to clipboard.";
                    LOG.info(message);
                    LOG.info("URL: {}", objectUrl);
                    String[] args = {
                            "echo " + objectUrl + " | pbcopy && ",
                            "/Users/trsharp/bin/alerter",
                            "-timeout 5",
                            "-title \"Object Uploaded!\"",
                            "-sender hubdocuments.oracle.webcenter.app",
                            "-message \"" + message + "\"",
                            "-appIcon https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/object-upload-demo-public/o/cloud.png",
                    };
                    processBuilder.command("bash", "-c", String.join(" ",args));
                    Process process = processBuilder.start();
                }
                else {
                    // the action is a file delete.
                    // I'm not going to do anything here,
                    // but you may want to construct and send a
                    // DeleteObjectRequest
                }
            }
            key.reset();
        }
    }
}
