package com.es.transalte.skytranslate.service;

import com.google.cloud.bigquery.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BigQueryService {
    private static final Logger logger = Logger.getLogger(BigQueryService.class.getName());
    private BigQuery bigquery;

    public BigQueryService(BigQuery bigquery) {
        this.bigquery = bigquery;
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

}
