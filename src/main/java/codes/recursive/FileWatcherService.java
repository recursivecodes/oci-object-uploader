package codes.recursive;

import com.oracle.bmc.model.BmcException;
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
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Singleton
public class FileWatcherService {
    private static final Logger LOG = LoggerFactory.getLogger(FileWatcherService.class);
    public final ObjectStorageClient objectStorageClient;
    private final Config config;
    private final String uploadDir;
    private final String bucket;
    private final String namespace;
    private final String region;
    private final Integer parDurationHours;
    private final Boolean isPrivateBucket;

    public FileWatcherService(
            ObjectStorageClient objectStorageClient,
            Config config
    ){
        this.objectStorageClient = objectStorageClient;
        this.config = config;
        this.uploadDir = config.getLocalUploadDir();
        this.bucket = config.getOci().getBucket();
        this.namespace = config.getOci().getNamespace();
        this.region = config.getOci().getRegion();
        this.parDurationHours = config.getOci().getParDurationHours();

        // cache bucket visibility
        GetBucketResponse getBucketResponse = objectStorageClient.getBucket(GetBucketRequest.builder().bucketName(bucket).namespaceName(namespace).build());
        this.isPrivateBucket = getBucketResponse.getBucket().getPublicAccessType().equals(Bucket.PublicAccessType.NoPublicAccess);
    }

    public void watch() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(uploadDir);
        LOG.info("Watching: {}", uploadDir);
        LOG.info("Objects will be uploaded to the '{}' bucket in '{}' (namespace: '{}')", bucket, region, namespace);
        if(isPrivateBucket) LOG.info("Since this bucket is private, pre-authenticated requests will be created with a duration of {} hours", parDurationHours);
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
                String objectUrl = "";
                Boolean hasException = false;
                if (!changedFile.isHidden() && changedFile.exists()) {
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
                        try{
                            PutObjectResponse putObjectResponse = objectStorageClient.putObject(putObjectRequest);
                        }
                        catch (BmcException e) {
                            hasException = true;
                            LOG.error("Update Object Exception: {}", e.getMessage());
                        }

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
                            try {
                                CreatePreauthenticatedRequestResponse preAuthenticatedRequestResponse = objectStorageClient.createPreauthenticatedRequest(preAuthenticatedRequest);
                                objectUrl = baseUrl + preAuthenticatedRequestResponse.getPreauthenticatedRequest().getAccessUri();
                            }
                            catch (BmcException e) {
                                hasException = true;
                                LOG.error("Create PAR Exception: {}, ", e.getMessage());
                            }
                        }
                        else {
                            objectUrl = baseUrl +  "/n/" + namespace + "/b/" + bucket + "/o/" + objectName.replaceAll(" ", "%20");
                        }
                        ProcessBuilder processBuilder = new ProcessBuilder();
                        String message = "Action (" + event.kind() + ") was " + (hasException ? "NOT " : "") + "applied to '" + objectName + "' in '" + bucket + "'. \n URL copied to clipboard.";
                        LOG.info(message.replaceAll("\\R", ""));
                        LOG.info("URL: {}", objectUrl);
                        String[] args = {
                                "echo " + objectUrl + " | pbcopy && ",
                                "/Users/trsharp/bin/alerter",
                                "-timeout 5",
                                "-title \"Object Uploaded!\"",
                                "-message \"" + message + "\""
                        };
                        processBuilder.command("bash", "-c", String.join(" ",args));
                        Process process = processBuilder.start();
                    }
                    else {
                        LOG.info("Action ({}) was intentionally IGNORED (Delete sync is disabled in this version).", event.kind());
                    }
                }
                else {
                    LOG.info("Action ({}) was intentionally IGNORED ({} is hidden).", event.kind(), objectName);
                }
            }
            key.reset();
        }
    }
}
