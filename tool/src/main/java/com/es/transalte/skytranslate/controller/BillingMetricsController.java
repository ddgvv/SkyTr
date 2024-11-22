package com.es.transalte.skytranslate.controller;

import com.es.transalte.skytranslate.handler.UserReportHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
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
        return userReportHandler.getUserReports();
    }

    @GetMapping("/billing-metrics-page")
    public String getBillingMetricsPage(Model model) {
        return "billing";
    }
}

