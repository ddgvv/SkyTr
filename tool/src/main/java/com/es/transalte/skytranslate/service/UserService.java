package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.resourcemanager.v3.*;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    final Logger logger = LoggerFactory.getLogger(UserService.class);
    private ProjectsClient client;
    private final CredentialsProvider credentialsProvider;

    public UserService(CredentialsProvider credentialsProvider) {
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

    public void addUserRole(String projectId, String serviceAccountEmail) {
        logger.info("Running addUserRole Project ID: " + projectId + ", Service Account Email: " + serviceAccountEmail);
        String resource = "projects/" + projectId;
        String role = "roles/cloudtranslate.user";
        String member = "serviceAccount:" + serviceAccountEmail;
        Policy policy = client.getIamPolicy(resource);
        List<Binding> updatedBindings = new ArrayList<>(policy.getBindingsList());
        boolean bindingFound = false;
        for (int i = 0; i < updatedBindings.size(); i++) {
            Binding binding = updatedBindings.get(i);
            if (role.equals(binding.getRole())) {
                if (!binding.getMembersList().contains(member)) {
                    Binding updatedBinding = Binding.newBuilder(binding)
                            .addMembers(member)
                            .build();
                    updatedBindings.set(i, updatedBinding);
                }
                bindingFound = true;
                break;
            }
        }
        if (!bindingFound) {
            Binding newBinding = Binding.newBuilder()
                    .setRole(role)
                    .addMembers(member)
                    .build();
            updatedBindings.add(newBinding);
        }
        Policy updatedPolicy = Policy.newBuilder(policy)
                .clearBindings()
                .addAllBindings(updatedBindings)
                .build();
        client.setIamPolicy(resource, updatedPolicy);
    }

}