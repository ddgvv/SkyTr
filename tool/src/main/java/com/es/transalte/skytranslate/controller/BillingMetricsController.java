package com.es.transalte.skytranslate.controller;

import com.es.transalte.skytranslate.service.BigQueryDatasetFetcher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.Map;

@Controller
public class BillingMetricsController {

    @GetMapping("/billing-metrics")
    @ResponseBody
    public List<Map<String, Object>> getBillingMetrics() {
        BigQueryDatasetFetcher bigQueryDatasetFetcher = new BigQueryDatasetFetcher();
        return bigQueryDatasetFetcher.getBqData("tabs-super-admin." + bigQueryDatasetFetcher.getBqDatasetName() + ".gcp_billing_export_v1_"
                    + bigQueryDatasetFetcher.getBillingAccountName("billingAccounts/019127-211CFD-310FB5") );
    }

    @GetMapping("/billing-metrics-page")
    public String getBillingMetricsPage(Model model) {
        return "billing";
    }
}