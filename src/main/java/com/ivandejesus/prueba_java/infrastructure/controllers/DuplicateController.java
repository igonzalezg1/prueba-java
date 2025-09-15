package com.ivandejesus.prueba_java.infrastructure.controllers;

import com.ivandejesus.prueba_java.domain.ports.in.DuplicateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/excel")
public class DuplicateController {

    @Autowired
    private DuplicateService duplicateService;

    @PostMapping(path = "/find-matchs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InputStreamResource> findMatchs(@RequestPart("file") MultipartFile file, @RequestParam(value = "sheet", required = false) String sheetName) throws IOException {
//        Validar que exista el archivo
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
//      Validar que sea tipo excel
        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity
                    .badRequest()
                    .body(null);
        }

        byte[] xlsx = duplicateService.findMatchs(file.getInputStream(), sheetName);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"matches.xlsx\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(xlsx.length)
                .body(new InputStreamResource(new ByteArrayInputStream(xlsx)));
    }
}
