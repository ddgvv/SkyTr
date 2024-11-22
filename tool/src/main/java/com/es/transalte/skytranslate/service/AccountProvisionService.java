package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.serviceusage.v1.*;
import com.google.cloud.iam.admin.v1.IAMClient;
import com.google.cloud.iam.admin.v1.IAMSettings;
import com.google.cloud.resourcemanager.v3.*;
import com.google.iam.admin.v1.CreateServiceAccountKeyRequest;
import com.google.iam.admin.v1.CreateServiceAccountRequest;
import com.google.iam.admin.v1.ServiceAccount;
import com.google.iam.admin.v1.ServiceAccountKey;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@Service
public class AccountProvisionService {
    private final Random random = new Random();
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHANUMERIC_AND_DASH = LOWERCASE + "0123456789-";
    final Logger logger = LoggerFactory.getLogger(AccountProvisionService.class);
    private final CredentialsProvider credentialsProvider;

    public AccountProvisionService(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }


    public String generateServiceAccountId() {
        logger.info("Running generateServiceAccountId...");
        int length = random.nextInt(25) + 6;
        char firstChar = LOWERCASE.charAt(random.nextInt(LOWERCASE.length()));
        StringBuilder middleChars = new StringBuilder();
        for (int i = 0; i < length - 2; i++) {
            middleChars.append(ALPHANUMERIC_AND_DASH.charAt(random.nextInt(ALPHANUMERIC_AND_DASH.length())));
        }
        char lastChar;
        int randomIndex = random.nextInt(LOWERCASE.length() + 10); // Generate random index for letters + digits
        if (randomIndex < LOWERCASE.length()) {
            lastChar = LOWERCASE.charAt(randomIndex); // Pick from lowercase letters
        } else {
            lastChar = Character.forDigit(randomIndex - LOWERCASE.length(), 10); // Pick from digits 0-9
        }
        return firstChar + middleChars.toString() + lastChar;
    }

    public ServiceAccount createServiceAccount(String projectId, String displayName) {
        try {
            logger.info("Running createServiceAccount with projectId: " + projectId + " and displayName: " + displayName);
            IAMSettings iamSettings = IAMSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            try (IAMClient iamClient = IAMClient.create(iamSettings)) {
                String accountId = generateServiceAccountId();
                CreateServiceAccountRequest request = CreateServiceAccountRequest.newBuilder()
                        .setName(ProjectName.of(projectId).toString())
                        .setAccountId(accountId)
                        .setServiceAccount(ServiceAccount.newBuilder()
                                .setDisplayName(displayName)
                                .build())
                        .build();
                ServiceAccount serviceAccount = iamClient.createServiceAccount(request);
                logger.info("createServiceAccount response: " + serviceAccount);
                return serviceAccount;
            }
        } catch (NotFoundException e) {
            logger.warn("Project not found: " + e.getMessage());
        } catch (IOException e) {
            logger.warn("Error initializing IAM client or creating service account: " + e.getMessage());
        }
        return null;
    }

    public void enableService(String projectId, String service) {
        logger.info("Running enableService...");
        try {
            ServiceUsageSettings settings = ServiceUsageSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            try (ServiceUsageClient client = ServiceUsageClient.create(settings)) {
                String serviceName = String.format("projects/%s/%s", projectId, service);
                EnableServiceRequest request = EnableServiceRequest.newBuilder()
                        .setName(serviceName)
                        .build();
                OperationFuture<EnableServiceResponse, OperationMetadata> future = client.enableServiceAsync(request);
                EnableServiceResponse response = future.get();
                logger.info("Enable service response: " + response);

            } catch (ExecutionException | InterruptedException e) {
                logger.warn("Error enabling service: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            logger.warn("Failed to create ServiceUsageClient: " + e.getMessage());
        }
    }

    public String createKey(String projectId, String serviceAccountEmail) throws IOException {
        IAMSettings iamSettings = IAMSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (IAMClient iamClient = IAMClient.create(iamSettings)) {
            String resourceName = String.format("projects/%s/serviceAccounts/%s", projectId, serviceAccountEmail);
            CreateServiceAccountKeyRequest request = CreateServiceAccountKeyRequest.newBuilder()
                    .setName(resourceName)
                    .build();
            ServiceAccountKey serviceAccountKey = iamClient.createServiceAccountKey(request);
            ByteString privateKeyData = serviceAccountKey.getPrivateKeyData();
            String convertedString = new String(privateKeyData.toByteArray(), StandardCharsets.ISO_8859_1);
            logger.info("privateKeyData " + convertedString);
            return convertedString;
        }
    }

}