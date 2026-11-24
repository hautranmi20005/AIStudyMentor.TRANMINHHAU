package com.aistudymentor.question.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "app.storage", havingValue = "firebase")
public class FirebaseStorageConfiguration {

    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseApp(
            @Value("${app.storage-config.firebase.service-account-file}") String serviceAccountFile,
            @Value("${app.storage-config.firebase.project-id:}") String projectId,
            @Value("${app.storage-config.firebase.bucket}") String bucket) throws IOException {
        Path credentialsPath = Path.of(serviceAccountFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(credentialsPath)) {
            throw new IllegalStateException("Firebase service-account file is not available.");
        }
        GoogleCredentials credentials;
        try (var input = Files.newInputStream(credentialsPath)) {
            credentials = GoogleCredentials.fromStream(input)
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        }
        FirebaseOptions.Builder options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setStorageBucket(bucket);
        if (!projectId.isBlank()) {
            options.setProjectId(projectId);
        }
        return FirebaseApp.initializeApp(options.build(), "study-mentor-storage");
    }

    @Bean
    Storage firebaseCloudStorage(FirebaseApp firebaseApp) {
        return StorageClient.getInstance(firebaseApp).bucket().getStorage();
    }
}
