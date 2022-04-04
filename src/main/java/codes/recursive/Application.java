package codes.recursive;

import com.oracle.bmc.objectstorage.model.Bucket;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetBucketResponse;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.runtime.Micronaut;

@TypeHint(
        value = {
                CreatePreauthenticatedRequestDetails.class,
                PutObjectRequest.class,
                PutObjectRequest.Builder.class,
                PreauthenticatedRequest.class,
                PreauthenticatedRequest.Builder.class,
                GetBucketResponse.class,
                Bucket.Builder.class
        },
        accessType = {
                TypeHint.AccessType.ALL_DECLARED_CONSTRUCTORS,
                TypeHint.AccessType.ALL_PUBLIC_METHODS,
                TypeHint.AccessType.ALL_DECLARED_FIELDS
        }
)
public class Application {

    private final FileWatcherService fileWatcherService;

    public Application(FileWatcherService fileWatcherService) {
        this.fileWatcherService = fileWatcherService;
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

}
