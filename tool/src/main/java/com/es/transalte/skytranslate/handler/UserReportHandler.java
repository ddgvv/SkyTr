package com.es.transalte.skytranslate.handler;

import com.es.transalte.skytranslate.service.BigQueryService;
import com.es.transalte.skytranslate.service.BillingService;
import com.es.transalte.skytranslate.service.JsonLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class UserReportHandler {
    private static final Logger logger = Logger.getLogger(UserReportHandler.class.getName());
    private BigQueryService bigQueryService;
    private JsonLoader jsonLoader;
    private BillingService billingService;

    public UserReportHandler(JsonLoader jsonLoader, BillingService billingService, BigQueryService bigQueryService) {
        this.jsonLoader = jsonLoader;
        this.billingService = billingService;
        this.bigQueryService = bigQueryService;
    }


    public  List<Map<String, Object>> getUserReports() {
        String billingAccountName = billingService.getBillingAccountName();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(jsonLoader.loadJson());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String projectId = jsonNode.get("project_id").asText();
        String bqDatasetName = bigQueryService.getBqDatasetName();
        return bigQueryService.getBqData(projectId+ "." + bqDatasetName+ ".gcp_billing_export_v1_" + billingAccountName);
    }

}
