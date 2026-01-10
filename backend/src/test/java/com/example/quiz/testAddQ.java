package com.example.quiz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.quiz.service.impl.PdfQuestionImportService;

@SpringBootTest
public class testAddQ {
    
    @Autowired
    private PdfQuestionImportService pdfQuestionImportService;
    
    @Test
    public void testAddQuestion() {
        String pdfFilePath = "D:\\BaiduNetdiskDownload\\test.pdf";
        try {
            System.out.println("pdfQuestionImportService: " + pdfQuestionImportService); 
            pdfQuestionImportService.importQuestionsFromLocalPdf(pdfFilePath, false);
            System.out.println("Questions imported successfully from PDF.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to import questions from PDF.");
        }
    }
}
