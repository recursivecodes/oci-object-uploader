package codes.recursive;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.http.*;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.*;
import com.oracle.bmc.objectstorage.requests.*;
import com.oracle.bmc.objectstorage.responses.*;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Singleton;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.CertificateUtils;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Singleton
public class FileWatcherService {
    private static final Logger LOG = LoggerFactory.getLogger(FileWatcherService.class);
    public final ObjectStorageClient objectStorageClient;
    private final String uploadDir;
    private final String bucket;
    private final String namespace;
    private final String region;

    public FileWatcherService(
            ObjectStorageClient objectStorageClient,
            @Property(name = "codes.recursive.upload-dir") String uploadDir,
            @Property(name = "codes.recursive.oci.bucket") String bucket,
            @Property(name = "codes.recursive.oci.namespace") String namespace,
            @Property(name = "codes.recursive.oci.region") String region
    ) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        //this.objectStorageClient = objectStorageClient;
        this.uploadDir = uploadDir;
        this.bucket = bucket;
        this.namespace = namespace;
        this.region = region;

        File crtFile = new File("/Users/trsharp/Library/Preferences/httptoolkit/ca.pem");
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(crtFile));
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("server", certificate);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        ApacheProxyConfig proxyConfig = ApacheProxyConfig.builder().uri("http://localhost:8000").build();
        ClientConfigDecorator proxyConfigDecorator = new ApacheProxyConfigDecorator(proxyConfig);
        ApacheConnectorProperties apacheConnectorProperties =
                ApacheConnectorProperties.builder()
                        .sslContext(sslContext)
                        .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();
        ApacheConfigurator configurator = new ApacheConfigurator.NonBuffering(
                apacheConnectorProperties,
                Collections.singletonList(proxyConfigDecorator)
        );
        this.objectStorageClient = ObjectStorageClient.builder()
                .clientConfigurator(configurator)
                .build(new ConfigFileAuthenticationDetailsProvider("DEFAULT"));
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
                String preAuthUrl = "https://objectstorage." + region + ".oraclecloud.com";

                // if this is a modify/create event, upload the object
                if (!StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())){

                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                    .objectName(event.context().toString())
                                    .namespaceName(namespace)
                                    .bucketName(bucket)
                                    .putObjectBody(new FileInputStream(changedFile))
                                    .build();
                    PutObjectResponse putObjectResponse = objectStorageClient.putObject(putObjectRequest);

                    RenameObjectResponse renameObjectResponse = objectStorageClient.renameObject(RenameObjectRequest.builder().bucketName(bucket).namespaceName(namespace).renameObjectDetails(RenameObjectDetails.builder().sourceName(objectName).newName("new.jpg").build()).build());
                    System.out.println(renameObjectResponse.toString());

                    // get a pre-authenticated request for the uploaded object
                    CreatePreauthenticatedRequestDetails requestDetails = CreatePreauthenticatedRequestDetails.builder()
                            .name("PAR_" + UUID.randomUUID().toString())
                            .bucketListingAction(PreauthenticatedRequest.BucketListingAction.Deny)
                            .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                            .objectName("new.jpg")
                            .timeExpires(Date.from( LocalDateTime.now().plusHours(24).atZone(ZoneId.systemDefault()).toInstant() ) )
                            .build();
                    System.out.println(requestDetails);
                    CreatePreauthenticatedRequestRequest preAuthenticatedRequest = CreatePreauthenticatedRequestRequest.builder()
                            .bucketName(bucket)
                            .namespaceName(namespace)
                            .createPreauthenticatedRequestDetails(requestDetails)
                            .build();
                    System.out.println(preAuthenticatedRequest);
                    CreatePreauthenticatedRequestResponse preAuthenticatedRequestResponse = objectStorageClient.createPreauthenticatedRequest(preAuthenticatedRequest);
                    preAuthUrl += preAuthenticatedRequestResponse.getPreauthenticatedRequest().getAccessUri();

                    // copy URL to clipboard and pop desktop notification
                    // depends on https://github.com/vjeantet/alerter
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    String[] args = {
                            "echo " + preAuthUrl + " | pbcopy && ",
                            "/Users/trsharp/bin/alerter",
                            "-timeout 5",
                            "-title \"Object Uploaded!\"",
                            "-sender hubdocuments.oracle.webcenter.app",
                            "-message \"" + objectName + " has been uploaded to '" + bucket + "'. URL has been copied to clipboard.\"",
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
