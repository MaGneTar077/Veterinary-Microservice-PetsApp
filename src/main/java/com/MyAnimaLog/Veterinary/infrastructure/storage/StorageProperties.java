package com.MyAnimaLog.Veterinary.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Data
public class StorageProperties {

    private String logosBucket = "veterinary-logos";
    private String verificationBucket = "verification-documents";
}
