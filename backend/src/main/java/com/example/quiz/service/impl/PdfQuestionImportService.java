package com.example.quiz.service.impl;

import com.example.quiz.model.Question;
import com.example.quiz.repo.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

/**
 * PDF 题目导入服务：读取本地 PDF → 解析题目 → 存入数据库
 */
@Service
@RequiredArgsConstructor // Lombok 注入依赖（无需手动写 @Autowired）
public class PdfQuestionImportService {

    private final TikaServiceImpl tikaServiceImpl; // PDF 文本提取服务
    private final ParseServiceImpl parseServiceImpl; // 题目解析服务
    private final QuestionRepository questionRepository; // JPA 数据库操作接口

    /**
     * 从本地 PDF 文件导入题目并存入数据库
     * @param pdfFilePath 本地 PDF 文件绝对路径（如 "D:/Quiz/题目文档.pdf"）
     * @param clearBeforeImport 是否在导入前清空现有题目（true=清空，false=追加）
     * @return 导入的题目数量
     * @throws Exception 读取/解析/数据库异常
     */
    @Transactional // 事务管理：要么全部导入成功，要么全部回滚
    public int importQuestionsFromLocalPdf(String pdfFilePath, boolean clearBeforeImport) throws Exception {
        // 1. 验证 PDF 文件存在
        File pdfFile = new File(pdfFilePath);
        if (!pdfFile.exists()) {
            throw new IllegalArgumentException("PDF 文件不存在！路径：" + pdfFilePath);
        }

        // 2. 验证是否为 PDF 格式
        if (!tikaServiceImpl.isPdfFile(pdfFile)) {
            throw new IllegalArgumentException("文件不是 PDF 格式！路径：" + pdfFilePath);
        }

        // 3. 提取 PDF 中的纯文本
        String pdfText = tikaServiceImpl.extractTextFromPdf(pdfFile);
        if (pdfText.isEmpty()) {
            throw new RuntimeException("PDF 文件中未提取到文本内容！");
        }

        // 4. 解析文本为 Question 列表
        List<Question> questionList = parseServiceImpl.parseDocument(pdfText);
        if (questionList.isEmpty()) {
            throw new RuntimeException("未从 PDF 中解析到题目！请检查题目格式是否正确（第X题、选项A-D、(答案)）");
        }

        // 5. 可选：导入前清空现有题目（根据需求选择）
        if (clearBeforeImport) {
            questionRepository.deleteAll();
            System.out.println("已清空数据库中所有现有题目");
        }

        // 6. 批量存入数据库（效率高于单条插入）
        List<Question> savedQuestions = questionRepository.saveAll(questionList);
        System.out.println("PDF 题目导入成功！共导入 " + savedQuestions.size() + " 道题目");

        return savedQuestions.size();
    }
}