package com.example.quiz;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.quiz.service.impl.TikaServiceImpl;

@SpringBootTest
public class testTika {
    @Autowired
    private TikaServiceImpl tikaServiceImpl;

    @Test
    public void testExtractText() throws Exception {
        long pdfStart = System.currentTimeMillis();
        File file = new File("D:\\BaiduNetdiskDownload\\test.pdf");
        String result = tikaServiceImpl.extractTextFromPdf(file);
        long pdfEnd = System.currentTimeMillis();
        System.out.println(result);
        System.out.println("PDF文本提取耗时：" + (pdfEnd - pdfStart) + "ms");
        Long Len = Long.valueOf(result.length());
        System.out.println(Len);



    }
}
