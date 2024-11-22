package com.es.transalte.skytranslate.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class JsonLoader {

    @Value("classpath:key.json")
    private org.springframework.core.io.Resource keyFile;

    public String loadJson() throws IOException {
        return new String(Files.readAllBytes(Paths.get(keyFile.getURI())));
    }
}
