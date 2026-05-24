package com.nahora.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = getClass().getClassLoader()
                    .getResourceAsStream("firebase-credentials.json");

            if (serviceAccount == null) {
                log.warn("Arquivo firebase-credentials.json não encontrado! Push Notifications não funcionarão.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase ativado com sucesso para Push Notifications!");
            }
        } catch (Exception e) {
            log.error("Erro ao inicializar o Firebase: ", e);
        }
    }
}