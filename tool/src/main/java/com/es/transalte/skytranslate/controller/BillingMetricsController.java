package com.es.transalte.skytranslate.controller;

import com.es.transalte.skytranslate.handler.UserReportHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BillingMetricsController {

    private UserReportHandler userReportHandler;

    BillingMetricsController(UserReportHandler userReportHandler) {
        this.userReportHandler = userReportHandler;
    }

    @GetMapping("/billing-metrics")
    @ResponseBody
    public List<Map<String, Object>> getBillingMetrics() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("Customer", "tbs-austin");
        map.put("InvoiceMonth", "202408");
        map.put("Pages Translated", 2.0);
        map.put("Cost", 0.16);
        map.put("Dealer", "tbs-texas");
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("Customer", "austin");
        objectMap.put("InvoiceMonth", "202408");
        objectMap.put("Pages Translated", 2.7);
        objectMap.put("Cost", 0.1);
        objectMap.put("Dealer", "texas");
        List<Map<String, Object>> getBillingMetrics = new ArrayList<>();
        getBillingMetrics.add(map);
        getBillingMetrics.add(objectMap);
        return getBillingMetrics;
        // return userReportHandler.getUserReports();
    }

    @GetMapping("/billing-metrics-page")
    public String getBillingMetricsPage(Model model) {
        return "billing";
    }
}

