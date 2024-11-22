package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.billing.budgets.v1.*;
import com.google.cloud.billing.v1.*;
import com.google.type.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class BillingService {

    private final CredentialsProvider credentialsProvider;
    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);
    private final InputStream credInputStream;

    public BillingService(JsonLoader jsonLoader, CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        try {
            credInputStream = new ByteArrayInputStream(jsonLoader.loadJson().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getBillingAccount() throws IOException {
        logger.info("Running getBillingAccount function ");

            Credentials credentials = ServiceAccountCredentials.fromStream(credInputStream);
            CloudBillingSettings billingSettings = CloudBillingSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (CloudBillingClient client = CloudBillingClient.create(billingSettings)) {
                ListBillingAccountsRequest request = ListBillingAccountsRequest.newBuilder().build();
                String billingAccountName = "";
                for (BillingAccount response : client.listBillingAccounts(request).iterateAll()) {
                    logger.info("Billing Account: " + response.getName());
                    billingAccountName = response.getName();
                }
                return billingAccountName;
            } catch (Exception e) {
                return null;
            }
    }

    public String getBillingAccountName() {
        try {
            String accountName = getBillingAccount();
            int chIndex = accountName.lastIndexOf("/");
            if (chIndex != -1) {
                return accountName.substring(chIndex + 1).replace('-', '_');
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    public Budget createBudget(String billingAccountId, String projectId, long budgetAmount) {
        logger.info("Running createBudget...");
        try {
            BudgetServiceSettings budgetServiceSettings = BudgetServiceSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            try (BudgetServiceClient budgetClient = BudgetServiceClient.create(budgetServiceSettings)) {
                String parent = String.format(billingAccountId);
                Budget budget = Budget.newBuilder()
                        .setDisplayName("Budget for " + projectId)
                        .setBudgetFilter(Filter.newBuilder()
                                .addProjects("projects/" + projectId)
                                .build())
                        .setAmount(BudgetAmount.newBuilder()
                                .setSpecifiedAmount(Money.newBuilder()
                                        .setCurrencyCode("USD")
                                        .setUnits(budgetAmount)
                                        .build())
                                .build())
                        .addThresholdRules(ThresholdRule.newBuilder()
                                .setThresholdPercent(0.5)
                                .setSpendBasis(ThresholdRule.Basis.CURRENT_SPEND)
                                .build())
                        .addThresholdRules(ThresholdRule.newBuilder()
                                .setThresholdPercent(0.9)
                                .setSpendBasis(ThresholdRule.Basis.CURRENT_SPEND)
                                .build())
                        .addThresholdRules(ThresholdRule.newBuilder()
                                .setThresholdPercent(0.95)
                                .setSpendBasis(ThresholdRule.Basis.CURRENT_SPEND)
                                .build())
                        .build();
                CreateBudgetRequest request = CreateBudgetRequest.newBuilder()
                        .setParent(parent)
                        .setBudget(budget)
                        .build();
                Budget response = budgetClient.createBudget(request);
                logger.info("Budget created successfully: " + response.getName());
                return response;
            }
        } catch (IOException e) {
            logger.warn("Failed to create BudgetServiceClient: " + e.getMessage());
        }
        return null;
    }

    public void linkBillingAccount(String projectId, String billingAccount) throws IOException {
        logger.info("Running linkBillingAccount...");
        logger.info("Trying to link project_id: " + projectId + " to billing_account: " + billingAccount);

        CloudBillingSettings billingSettings = CloudBillingSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (CloudBillingClient client = CloudBillingClient.create(billingSettings)) {
            ProjectBillingInfo projectBillingInfo = ProjectBillingInfo.newBuilder()
                    .setBillingAccountName(billingAccount)
                    .build();
            String name = "projects/" + projectId;
            UpdateProjectBillingInfoRequest request = UpdateProjectBillingInfoRequest.newBuilder()
                    .setName(name)
                    .setProjectBillingInfo(projectBillingInfo)
                    .build();
            ProjectBillingInfo response = client.updateProjectBillingInfo(request);
            logger.info("linkBillingAccount response: " + response);
        } catch (IOException e) {
            logger.warn("Failed to link billing account: " + e.getMessage());
            throw e;
        }
    }

}
