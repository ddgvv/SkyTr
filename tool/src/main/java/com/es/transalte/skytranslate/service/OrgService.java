package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.resourcemanager.v3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OrgService {
    final Logger logger = LoggerFactory.getLogger(OrgService.class);
    private final CredentialsProvider credentialsProvider;

    public OrgService(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public String getOrgName() throws IOException {
        logger.info("Running getOrgName() function...");
        try (OrganizationsClient client = OrganizationsClient.create(
                OrganizationsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build())) {
            SearchOrganizationsRequest request = SearchOrganizationsRequest.newBuilder().build();
            OrganizationsClient.SearchOrganizationsPagedResponse response = client.searchOrganizations(request);
            logger.info("getOrgName() response: ");
            for (Organization organization : response.iterateAll()) {
                logger.info(organization.toString());
                return organization.getName();
            }
        } catch (IOException e) {
            logger.error("Error occurred while fetching organization name", e);
            throw e;
        }
        return null;
    }
}