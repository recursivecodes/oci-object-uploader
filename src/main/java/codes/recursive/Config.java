package codes.recursive;

import io.micronaut.context.annotation.ConfigurationProperties;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(value = "uploader")
public class Config {
    @NotNull
    private String localUploadDir;

    public String getLocalUploadDir() {
        return localUploadDir;
    }

    public void setLocalUploadDir(String localUploadDir) {
        this.localUploadDir = localUploadDir;
    }

    private Oci oci = new Oci();

    public Oci getOci() {
        return oci;
    }

    public void setOci(Oci oci) {
        this.oci = oci;
    }

    @ConfigurationProperties(value = "oci")
    public static class Oci {
        @NotNull
        private String namespace;
        @NotNull
        private String bucket;
        @NotNull
        private String region;
        @NotNull
        @Min(value = 24)
        private Integer parDurationHours = 24;

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public Integer getParDurationHours() {
            return parDurationHours;
        }

        public void setParDurationHours(Integer parDurationHours) {
            this.parDurationHours = parDurationHours;
        }
    }

}
