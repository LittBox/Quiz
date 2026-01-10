package com.example.quiz.controller;

import com.example.quiz.model.Question;
import com.example.quiz.service.impl.ParseServiceImpl;

import org.apache.tika.exception.TikaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UploadController {
    private final ParseServiceImpl parseService;

    public UploadController(ParseServiceImpl parseService) {
        this.parseService = parseService;
    }

    // @PostMapping("/upload")
    // public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException, TikaException {
    //     List<Question> saved = parseService.parseAndSave(file);
    //     return ResponseEntity.ok().body("Imported " + saved.size() + " questions");
    // }
}
