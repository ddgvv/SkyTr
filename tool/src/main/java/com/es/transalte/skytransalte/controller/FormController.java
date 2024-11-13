package com.es.transalte.skytransalte.controller;

import com.es.transalte.skytransalte.model.FormData;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class FormController {

    @GetMapping("/skytranslate")
    public String showForm(Model model) {
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
