package com.aistudymentor.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "app.push", havingValue = "fcm")
public class PushFirebaseConfiguration {

    @Bean(destroyMethod = "delete")
    FirebaseApp pushFirebaseApp(
            @Value("${app.push-config.service-account-file}") String serviceAccountFile,
            @Value("${app.push-config.project-id:}") String projectId
    ) throws IOException {
        Path credentialsPath = Path.of(serviceAccountFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(credentialsPath)) {
            throw new IllegalStateException("Firebase push service-account file is not available.");
        }
        GoogleCredentials credentials;
        try (var input = Files.newInputStream(credentialsPath)) {
            credentials = GoogleCredentials.fromStream(input)
                    .createScoped("https://www.googleapis.com/auth/firebase.messaging");
        }
        FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials);
        if (!projectId.isBlank()) {
            options.setProjectId(projectId);
        }
        return FirebaseApp.initializeApp(options.build(), "study-mentor-push");
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp pushFirebaseApp) {
        return FirebaseMessaging.getInstance(pushFirebaseApp);
    }
}
