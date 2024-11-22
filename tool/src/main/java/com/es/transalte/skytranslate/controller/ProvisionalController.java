package com.es.transalte.skytranslate.controller;

import com.es.transalte.skytranslate.handler.ProvisionalHandler;
import com.es.transalte.skytranslate.model.FormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.nio.charset.StandardCharsets;

@Controller
public class ProvisionalController {
    final Logger logger = LoggerFactory.getLogger(ProvisionalController.class);
    private final ProvisionalHandler provisionalHandler;

    @Autowired public ProvisionalController(ProvisionalHandler provisionalHandler) {
        this.provisionalHandler = provisionalHandler;
    }

    @GetMapping("/SkyTranslate")
    public String showForm(Model model) {
        model.addAttribute("formData", new FormData());
        return "home";
    }

    @PostMapping("/submitForm")
    public ResponseEntity<ByteArrayResource> submitForm(@RequestBody FormData formData) {
        try {
            String keyData = provisionalHandler.processFirstPage(formData.getDealerName().toLowerCase(),formData.getCustomerName().toLowerCase(), formData.getAccountType());
            ByteArrayResource resource = new ByteArrayResource(keyData.getBytes(StandardCharsets.UTF_8));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=response.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}