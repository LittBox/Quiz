package com.example.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.quiz.model.Question;
import com.example.quiz.repo.QuestionRepository;
import com.example.quiz.service.impl.TikaServiceImpl;

import jakarta.transaction.Transactional;


@Transactional
public class PDFFileImport {

        
        @Autowired
        private QuestionRepository questionRepository;
        @Autowired
        private TikaServiceImpl tikaServiceImpl;

        public void importPDF(File file) throws Exception {
        // This class is intended for importing PDF files.
        // 这一个正则就够了！一次性捕获 题号、题干、答案、A/B/C/D四个选项 全部内容
            String regex =  "(\\d+)"                         // 题号
                            + "([^A]*?)"                                // 题干（非贪婪）
                            + "\\(([A-Z]+)\\)"                          // 答案
                            + "A([^B]*?)"                               // 选项A（注意这里加了A）
                            + "B([^C]*?)"                               // 选项B
                            + "C([^D]*?)"                               // 选项C
                            + "D([^E\\d]*)"                             // 选项D（贪婪，直到E或数字）
                            + "(?:E([^\\d]*))?"                         // 可选选项E
                            + "(?=\\d|$)";
            Pattern pattern = Pattern.compile(regex);
            
            String result = tikaServiceImpl.extractTextFromPdf(file);


            // 文本预处理：统一格式，消除PDF提取的常见干扰
                String processedResult = result
                                            // 第一阶段：规范化格式
                                            // 1. 统一全角/半角字符
                                            .replace("　", " ")          // 全角空格转半角空格
                                            .replace("（", "(")          // 全角括号转半角
                                            .replace("）", ")")
                                            .replace("．", "")          // 全角点转半角点
                                            .replace("，", "")
                                            .replace("；", ";")
                                            .replace("：", ":")
                                            .replace("？", "?")
                                            .replace("！", "!")
                                            .replace("、", "")          // 中文顿号转逗号
                                            
                                            // 2. 移除所有换行和制表符
                                            .replaceAll("\\s", "")
                                            
                                            // 第二阶段：统一题号和选项格式
                                            // 4. 统一题号格式（所有题号都变成"数字、"形式）
                                            .replaceAll("(\\d+)\\s*[\\.、,]\\s*", "$1")
                                            
                                            // 5. 统一选项格式
                                            .replaceAll("\\s*([A-E])\\s*[\\.、，,]\\s*", "$1")  // 选项前无空格，后无标点
                                            
                                        
                                        
                                            
                                            // 第三阶段：移除不需要的字符和标题
                                            // 8. 移除章节标题和特殊标记
                                            .replaceAll("第[一二三四五六七八九十]+章\\s*", "")
                                            .replaceAll("第[一二三四五六七八九十]+节\\s*", "")
                                            .replaceAll("[一二三四五六七八九十]、[^\\s]*题型", "")
                                            .replaceAll("一单项选择题", "")
                                            .replaceAll("二多项选择题", "")
                                            .replaceAll("三材料分析题", "")
                                            .replaceFirst("《马克思主义基本原理概论》", "")
                                            .replace("绪论", "")
                                            .replace("题库", "")
                                            .replace("试卷", "")
                                            .replace("测试题", "")
                                            
                                            // 9. 移除不可见字符和特殊符号
                                            .replaceAll("[｡●◆■▲►▼◄►◄※★☆◎○●◇◆□℃§¶•→←↑↓↔↕↨↻⇄⇅⇆⇇⇈⇉]", "")
                                            .replaceAll("[\\u0000-\\u001F\\u007F-\\u009F]", "")  // 控制字符
                                            .replaceAll("[\\u2000-\\u206F\\u2E00-\\u2E7F]", "")  // 标点符号区域
                                            .replaceAll("[\\u3000]", "")  // 表意文字空格
                                            
                                            
                                            // 13. 清理开头和结尾的空格
                                            .trim();
                    

            // 关键：打印预处理后的文本片段，确认格式是否统一（重点看题号、答案、选项的格式）
            System.out.println("预处理后的文本片段（前1000字符）：\n" + processedResult.substring(0, Math.min(processedResult.length(), 1000)));

            Matcher matcher = pattern.matcher(processedResult);

            List<Question> questions = new ArrayList<>();
            while (matcher.find()) {
                Question question = new Question();
                // 判断题目类型：答案长度大于1为多选题，否则为单选题
                if (matcher.group(3).trim().length() > 1) {
                    question.setQuestionType("multi_choice");
                } else {
                    question.setQuestionType("single_choice");
                }
           
                question.setContent(matcher.group(2).trim());
                question.setAnswer(matcher.group(3).trim());
                question.setOptionA(matcher.group(4).trim());
                question.setOptionB(matcher.group(5).trim());
                question.setOptionC(matcher.group(6).trim());
                question.setOptionD(matcher.group(7).trim());

                // 处理E选项（如果有）
                if (matcher.group(8) != null && !matcher.group(8).trim().isEmpty()) {
                    question.setOptionE(matcher.group(8).trim());
                }

                questions.add(question);
                
                questionRepository.save(question);

            }
            System.out.println("总共解析出 " + questions.size() + " 道题目。");
        }
}
