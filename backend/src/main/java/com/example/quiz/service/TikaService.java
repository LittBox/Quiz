package com.example.quiz.service;

import java.io.File;
import java.io.InputStream;


public interface TikaService {

     /**
     * 从 PDF 文件中提取文本
     * @param pdfFile PDF 文件对象
     * @return 提取的纯文本内容
     * @throws Exception 解析过程中的异常（文件不存在、格式错误等）
     */
    String extractTextFromPdf(File pdfFile) throws Exception;

    /**
     * 从 PDF 输入流中提取文本（适用于文件上传场景）
     * @param pdfInputStream PDF 文件输入流
     * @return 提取的纯文本内容
     * @throws Exception 解析过程中的异常（流异常、格式错误等）
     */
    String extractTextFromPdf(InputStream pdfInputStream) throws Exception;

    /**
     * 验证文件是否为 PDF 格式（避免非 PDF 文件导致的解析错误）
     * @param file 待验证文件
     * @return true=是 PDF 格式，false=不是
     * @throws Exception 文件读取异常
     */
    boolean isPdfFile(File file) throws Exception;

    /**
     * 验证输入流是否为 PDF 格式（适用于文件上传场景）
     * @param inputStream 待验证输入流
     * @return true=是 PDF 格式，false=不是
     * @throws Exception 流读取异常
     */
    boolean isPdfFile(InputStream inputStream) throws Exception;
    
} 