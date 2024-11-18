package com.es.transalte.skytranslate.service;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BigQueryDatasetFetcher {
    private static final Logger logger = Logger.getLogger(BigQueryDatasetFetcher.class.getName());
    BigQuery bigquery;

    public BigQueryDatasetFetcher() {
        String  credentialsPath = "{\n" +
                "  \"type\": \"service_account\",\n" +
                "  \"project_id\": \"tabs-super-admin\",\n" +
                "  \"private_key_id\": \"29c8d4bcaf035906386a239d8845d4936991afd9\",\n" +
                "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCuIEh4q4JABIY+\\nhF9xROnMGtOjV2mrk7VHUJPB7Vgo7Tqrf6qZcPmwaebXKaGDQejrpdbIYwn9mrCI\\nnKDs3KAu8SgTefSdjKcy96nj0w8Zy47jMI2jUgrcBarc80z2F+KbFPWqx1vZrC6X\\nhlsqQi882D/2s/ikHuIwtiWDQLLKsDyVhapj1tGOdvkhc/ufDgcCMj8TWlPgELWi\\nNySf5kS/l3lvBztxebprpT3Kpuqyr7Uqqp2as8y4xOT71CRXBaqpeeubql8FWUiK\\ncrJhUk1wbdljNj6HMRkKy98MsPkXk56ycQpm/D0HzMm9VCBPRyK4QYOlsiDxxUYn\\nCTMIfs7ZAgMBAAECggEAMaE69sXvrH59ouEI8RhyyP2Wd/aIxGIn56k0TDBxkcy4\\nDbyoK+7zBBDTPl17zxUgpdXRZ7Eu3k1SZsW56nkh4O9UfGSkz6kQ2OqVTj3Qjooh\\n8mha3oOaW8bzweFI/NWm/YvWT7Rd3ieA6ihd4KgXnbCHXrJCOskgsECiZYwxyapE\\nZTRBXVdIqy1mrGHIjLOm8Pn7gZQLA574Qetgzh0ooDwpezrmdDGF/MGU1bW1dg47\\nAcFlQNCUj4cF5ShF6ligSDdMDbg+BMyBkMpTRafj7J/Soer627LTxqklw0dK+TPV\\nGyn0X4qVvLMzvjMcp8SrL0jqaQKP6uMfFRBIYS+ZjwKBgQDVlUfDA8CltL6btB7/\\nRpSXBTpPU3PkiveMSXBsTAb25Xy0PoNyYtJ6b5/ExHglM1oixTh0tke7i6n+gyMr\\nNildQx6q9kFkFsEuSf3bgWviZDDD99JqxNvoSe1k8yEGaBAgLGXSVhgTtCdy9N+P\\ni+rfjQKbZn61QI2d6Lfp918WGwKBgQDQtPq/wNWEuh9mcOVVSJSwB3sub1Ca7qQV\\nIHRJ/ym7YDdnU42KbLsNDaiZuhC24KZeNxcvzbHlJBfN1aDnOKiSUuF5aVSsVYRw\\nG9MmBTybYLgGx4a1iZXSq2+JgVBvqsuEEvoZ4084UqOudhPPpkf1kpe9Tb+7aEQR\\nP1/oxuYOGwKBgQDPq93h7cUkmhD3vnShTBRwn0GqHf/CyaiXfFTWyDnBWTQe4eXX\\nk+UJ7X1QyqPzr/HFezRAr3giEFPTR9krS/d+WiP4oYbFdiaSBpnSFA49S6Pq+A1d\\niVo1i6RLEuganZaIYgMDOHkit3ngGd9CaQ6QHUDarxcmz8SjNMOWt9N53wKBgHmW\\nNc6oZLviQpDHjJNWqWizswumRes8w9KbCaRiRsmmkCBeCMNv/LjqECMexsYDsmiI\\n57UtvEml8Ug1AHw0AeDc8AZvWqjbWUCGtUgdHOXYPt2UN+JiuSn2PhB5iYbElbPn\\n6lRSnMBPRJRKrdf3I0zpjgT22pAvehgiN5nA5h2pAoGAPTP1lqg076s3dbyp3YvN\\n750PR8IJxiyWo1nVWNQeoDwlzZFEIdW/eKat4QETdYmhjibQb9CDei296VwXGxl2\\nJX860eMSCq2slqQijUK79J5RYPnkX3TL40b6K1qZOdW2OSKPSE6h2iqCNxYzCTz1\\nAXdyVuug3WxrlL7yZz+tklw=\\n-----END PRIVATE KEY-----\\n\",\n" +
                "  \"client_email\": \"super-admin-service@tabs-super-admin.iam.gserviceaccount.com\",\n" +
                "  \"client_id\": \"115003583686037589203\",\n" +
                "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/super-admin-service%40tabs-super-admin.iam.gserviceaccount.com\",\n" +
                "  \"universe_domain\": \"googleapis.com\"\n" +
                "}";
        InputStream inputStream = new ByteArrayInputStream(credentialsPath.getBytes(StandardCharsets.UTF_8));
        ServiceAccountCredentials credentials = null;
        try {
            credentials = ServiceAccountCredentials.fromStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        bigquery = BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }

    public String getBqDatasetName() {
        logger.info("Running getBqDatasetName()");
        String datasetName = "No Datasets found.";

        for (Dataset dataset : bigquery.listDatasets().iterateAll()) {
            logger.info("Dataset: " + dataset.getDatasetId().getDataset());
            datasetName = dataset.getDatasetId().getDataset();
            break;
        }

        return datasetName;
    }

    public List<Map<String, Object>> getBqData(String bqData) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            String query = String.format(
                    "SELECT invoice.month AS `Invoice Month`, project.name AS Customer, ancestor.display_name AS Dealer, "
                            + "SUM(usage.amount_in_pricing_units) AS `Pages Translated`, SUM(cost) AS `Cost` "
                            + "FROM `%s`, UNNEST(project.ancestors) AS ancestor "
                            + "WHERE sku.id = '44EC-A768-3123' "
                            + "AND ancestor.display_name != project.name "
                            + "AND NOT CONTAINS_SUBSTR(ancestor.resource_name, 'organizations') "
                            + "GROUP BY invoice.month, project.name, ancestor.display_name "
                            + "ORDER BY `Invoice Month`;",
                    bqData
            );

            logger.info("Getting BigQuery Data with Query: " + query);
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            TableResult result = null;
            try {
                result = bigquery.query(queryConfig);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (FieldValueList row : result.iterateAll()) {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("Invoice Month", row.get("Invoice Month").getStringValue());
                rowMap.put("Customer", row.get("Customer").getStringValue());
                rowMap.put("Dealer", row.get("Dealer").getStringValue());
                rowMap.put("Pages Translated", row.get("Pages Translated").getNumericValue());
                rowMap.put("Cost", row.get("Cost").getNumericValue());
                resultList.add(rowMap);
            }

            logger.info("BigQuery Data fetched successfully: " + resultList);
        } catch (BigQueryException e) {
            logger.severe("BigQuery query failed: " + e.getMessage());
        }

        return resultList;
    }

    public String getBillingAccountName(String input) {
        String accountName = input;
        int chIndex = accountName.lastIndexOf("/");
        if (chIndex != -1) {
            return accountName.substring(chIndex + 1).replace('-', '_');
        }
        return "";
    }

}
