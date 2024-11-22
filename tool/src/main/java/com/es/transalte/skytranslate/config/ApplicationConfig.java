package com.es.transalte.skytranslate.config;

import com.es.transalte.skytranslate.handler.UserReportHandler;
import com.es.transalte.skytranslate.handler.ProvisionalHandler;
import com.es.transalte.skytranslate.service.*;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class ApplicationConfig {

    @Bean
    public JsonLoader jsonLoader(){
        return new JsonLoader();
    }

    @Bean
    public CredentialsProvider credentialsProvider(JsonLoader jsonLoader) throws IOException {
        String credentialsPath = jsonLoader().loadJson();
        InputStream inputStream = new ByteArrayInputStream(credentialsPath.getBytes(StandardCharsets.UTF_8));
        GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
        return FixedCredentialsProvider.create(credentials);
    }

    @Bean
    public UserReportHandler bigQueryDatasetFetcher(JsonLoader jsonLoader, BillingService billingService,
                                BigQueryService bigQueryService) {
        return new UserReportHandler(jsonLoader, billingService, bigQueryService);
    }

    @Bean
    public BillingService billingService(JsonLoader jsonLoader, CredentialsProvider credentialsProvider) {
        return new BillingService(jsonLoader, credentialsProvider);
    }

    @Bean
    public UserService userService(CredentialsProvider credentialsProvider) {
        return new UserService(credentialsProvider);
    }

    @Bean
    public AccountProvisionService accountProvisionService(CredentialsProvider credentialsProvider) {
        return new AccountProvisionService(credentialsProvider);
    }

    @Bean
    public OrgService orgService(CredentialsProvider credentialsProvider) {
        return new OrgService(credentialsProvider);
    }

    @Bean
    public DealerService dealerService(CredentialsProvider credentialsProvider) {
        return new DealerService(credentialsProvider);
    }

    @Bean
    public ProvisionalHandler provisionalHandler(BillingService billingService, OrgService orgService, DealerService dealerService,
                                                 CustomerService customerService, AccountProvisionService accountProvisionService, UserService userService) {
        return new ProvisionalHandler(billingService, orgService, dealerService, customerService, accountProvisionService, userService);
    }

    @Bean
    public BigQueryService bigQueryService(JsonLoader jsonLoader) {
        ServiceAccountCredentials credentials = null;
        try {
            InputStream inputStream = new ByteArrayInputStream(jsonLoader.loadJson().getBytes(StandardCharsets.UTF_8));
            credentials = ServiceAccountCredentials.fromStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BigQuery bigQuery = BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
        return new BigQueryService(bigQuery);
    }

}
