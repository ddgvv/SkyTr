package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.resourcemanager.v3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@Service
public class CustomerService {
    private final Random random = new Random();
    final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    private ProjectsClient client;
    private final CredentialsProvider credentialsProvider;

    public CustomerService(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        try {
            ProjectsSettings foldersSettings = ProjectsSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();

            this.client = ProjectsClient.create(foldersSettings);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean customerExists(String dealerName, String customerName) throws IOException {
        logger.info("Running customerExists({}, {}) function...", dealerName, customerName);
        ProjectsSettings projectsSettings = ProjectsSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (ProjectsClient client = ProjectsClient.create(projectsSettings)) {
            ListProjectsRequest request = ListProjectsRequest.newBuilder().setParent(customerName)
                    .build();
            for (Project response : client.listProjects(request).iterateAll()) {
                if (response.getDisplayName().equals(dealerName)) {
                    logger.info("customerExists() function returned True");
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public Project createCustomer(String parent, String dealerName, String customerName) {
        logger.info("Creating customer: " + customerName + " in parent: " + dealerName);

        try {
            if (customerExists(parent, customerName)) {
                logger.warn("Customer " + customerName + " already exists under Dealer: " + dealerName);
                return null;
            } else {
                String projectId = generateProjectId(customerName);
                Project project = Project.newBuilder()
                        .setParent(parent)
                        .setProjectId(projectId)
                        .setDisplayName(customerName)
                        .putLabels("dealer", dealerName)
                        .build();

                CreateProjectRequest request = CreateProjectRequest.newBuilder()
                        .setProject(project)
                        .build();

                try {
                    OperationFuture<Project, CreateProjectMetadata> operation = client.createProjectAsync(request);
                    Project response = operation.get();
                    logger.info("createCustomer " + customerName + " in parent " + dealerName + " returned: " + response);
                    return response;
                } catch (ApiException | InterruptedException e) {
                    logger.warn("Error creating customer project: " + e.getMessage());
                    return null;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateProjectId(String customerName) {
        int length = 6 + random.nextInt(5);
        StringBuilder projectId = new StringBuilder(customerName.toLowerCase().replaceAll(" ", "-").substring(0, Math.min(customerName.length(), 20)) + "-");
        for (int i = 0; i < length - 2; i++) {
            projectId.append(randomChar());
        }
        projectId.append(randomChar(true));
        return projectId.toString();
    }

    private char randomChar() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789-";
        return chars.charAt(random.nextInt(chars.length()));
    }

    private char randomChar(boolean lettersOnly) {
        String chars = lettersOnly ? "abcdefghijklmnopqrstuvwxyz0123456789" : "abcdefghijklmnopqrstuvwxyz0123456789-";
        return chars.charAt(random.nextInt(chars.length()));
    }

}