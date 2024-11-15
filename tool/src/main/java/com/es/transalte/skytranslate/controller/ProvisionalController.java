package com.es.transalte.skytranslate.controller;

import com.es.transalte.skytranslate.model.FormData;
import com.es.transalte.skytranslate.service.BillingService;
import com.es.transalte.skytranslate.service.GoogleResourceManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.resourcemanager.v3.Folder;
import com.google.cloud.resourcemanager.v3.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Controller
public class ProvisionalController {

    @Autowired private GoogleResourceManagerService googleResourceManagerService;
    @Autowired private BillingService billingService;

    @GetMapping("/SkyTranslate")
    public String showForm(Model model) {
        googleResourceManagerService.processFirstPage();
        model.addAttribute("formData", new FormData());
        return "home";
    }

    @PostMapping("/submitForm")
    public ResponseEntity<InputStreamResource> submitForm(@ModelAttribute FormData formData) {
try {
            // Convert FormData to JSON
            ObjectMapper mapper = new ObjectMapper();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mapper.writeValue(outputStream, formData);
            byte[] jsonData = outputStream.toByteArray();

            // Set headers for downloading the file
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=formData.json");

            // Return the file as a downloadable resource
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(jsonData.length)
                    .body(new InputStreamResource(new ByteArrayInputStream(jsonData)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}

//def link_billing_account(project_id: str, billing_account: str) -> None:
//def enable_service(project_id: str, service: str) -> None:
//def create_budget(billing_account_id: str, project_id: str, budget_amount: int):
//def create_service_account(project_id: str, displayName: str) -> types.ServiceAccount:
//def create_key(project_id: str, service_account: str) -> str:
//def add_user_role(project_id: str, service_account_email:str) -> None:
