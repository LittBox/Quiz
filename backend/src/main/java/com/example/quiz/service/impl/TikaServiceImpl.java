package com.example.quiz.service.impl;

import com.example.quiz.service.TikaService;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Tika 服务实现类，基于 Apache Tika 解析 PDF 文档
 */
@Service
public class TikaServiceImpl implements TikaService {

    // Tika 核心对象（线程安全，可全局复用）
    private final Tika tika;

    public TikaServiceImpl() {
        // 初始化 Tika，启用自动格式检测
        this.tika = new Tika();
    }

    @Override
    public String extractTextFromPdf(File pdfFile) throws Exception {
        // 先验证是否为 PDF 文件
        if (!isPdfFile(pdfFile)) {
            throw new IllegalArgumentException("传入的文件不是 PDF 格式！");
        }

        // 使用 FileInputStream 读取文件
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(pdfFile))) {
            return extractTextFromPdf(inputStream);
        }
    }

    @Override
    public String extractTextFromPdf(InputStream pdfInputStream) throws Exception {
        // BodyContentHandler：捕获解析后的文本内容（默认无长度限制）
        BodyContentHandler contentHandler = new BodyContentHandler(-1); // -1 表示不限制文本长度
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();

        AutoDetectParser parser = new AutoDetectParser();
        parseContext.set(PDFParser.class, new PDFParser());


        try {
            // 执行解析
            parser.parse(pdfInputStream, contentHandler, metadata, parseContext);
            // 返回提取的纯文本
            return contentHandler.toString().trim();
        } finally {
            // 关闭输入流（避免资源泄漏）
            pdfInputStream.close();
        }
    }

    @Override
    public boolean isPdfFile(File file) throws Exception {
        // 通过 Tika 检测文件 MIME 类型
        String mimeType = tika.detect(file);
        return "application/pdf".equals(mimeType);
    }

    @Override
    public boolean isPdfFile(InputStream inputStream) throws Exception {
        // 通过 Tika 检测输入流 MIME 类型（注意：流会被读取一部分，需确保后续可重复读取）
        String mimeType = tika.detect(inputStream);
        // 重置流指针到开头（如果流支持重置）
        if (inputStream.markSupported()) {
            inputStream.reset();
        }
        return "application/pdf".equals(mimeType);
    }
}