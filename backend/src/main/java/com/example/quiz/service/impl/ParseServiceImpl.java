package com.example.quiz.service.impl;

import com.example.quiz.model.Question;
import com.example.quiz.repo.QuestionRepository;
import com.example.quiz.service.ParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParseServiceImpl implements ParseService {

    @Autowired
    private QuestionRepository questionRepository;

    // 题库中常见的题目类型标识（用于区分章节和题目）
    private static final String[] QUESTION_TYPE_MARKERS = {
            "一､单项选择题", "二､多项选择题", "三､材料分析题",
            "第一章", "第二章", "第三章", "第四章", "第五章", "绪论"
    };

    @Override
    public List<Question> parseDocument(String documentContent) {
        List<Question> questionList = new ArrayList<>();
        // 预处理：统一全角符号为半角（如“､”→“、”“（”→“(”），避免格式不统一导致匹配失败
        String processedContent = preprocessDocument(documentContent);

        // 1. 提取材料分析题（先处理材料题，避免材料被单选/多选题正则误匹配）
        List<Question> materialQuestions = parseMaterialAnalysisQuestions(processedContent);
        questionList.addAll(materialQuestions);

        // 2. 提取单选题和多选题（排除已被材料题占用的内容）
        String remainingContent = removeMaterialSections(processedContent);
        List<Question> singleAndMultiQuestions = parseSingleAndMultiQuestions(remainingContent);
        questionList.addAll(singleAndMultiQuestions);

        // 3. 统一设置题目序号（按提取顺序排序）
        resetQuestionSeqOrder(questionList);

        // 批量保存（优化性能，避免循环中频繁调用save）
        questionRepository.saveAll(questionList);
        return questionList;
    }

    /**
     * 文档预处理：统一符号格式、去除多余空行、处理换行符
     */
    private String preprocessDocument(String content) {
        return content
                .replaceAll("､", "、") // 全角顿号→半角
                .replaceAll("（", "(")   // 全角左括号→半角
                .replaceAll("）", ")")   // 全角右括号→半角
                .replaceAll("\\r\\n|\\r|\\n", "\n") // 统一换行符
                .replaceAll("\\n+", "\n") // 合并多余空行
                .trim(); // 去除首尾空白
    }

    /**
     * 提取材料分析题（格式：【材料X】+ 多个问题 + 选项 + 答案）
     */
    private List<Question> parseMaterialAnalysisQuestions(String content) {
        List<Question> materialQuestions = new ArrayList<>();
        // 匹配【材料X】块（包含材料内容 + 关联的问题）
        Pattern materialPattern = Pattern.compile(
                "【材料(\\d+)】([\\s\\S]+?)(?=【材料\\d+】|$)", // 匹配单个材料块（直到下一个材料或文档结束）
                Pattern.CASE_INSENSITIVE
        );
        Matcher materialMatcher = materialPattern.matcher(content);

        while (materialMatcher.find()) {
            String materialNo = materialMatcher.group(1); // 材料编号（如“1”“2”）
            String materialContent = materialMatcher.group(2).trim(); // 材料内容（如“恩格斯说：...”）

            // 匹配材料块内的问题（如“(1)上述材料体现的哲学原理是( )”）
            Pattern materialQuesPattern = Pattern.compile(
                    "([\\s\\S]*?)?" + // 可能的问题前缀（如“1.”“(1)”）
                            "([\\s\\S]+?)" + // 问题题干（如“上述材料体现的哲学原理是”）
                            "\\(([A-D,\\s]+)\\)" + // 答案（如“AB”“C”，支持多选）
                            "\\s*" +
                            "(?:A、([\\s\\S]+?))?" + // 选项A（材料题可能无选项，如主观题，故用非捕获组+可选）
                            "(?:B、([\\s\\S]+?))?" + // 选项B
                            "(?:C、([\\s\\S]+?))?" + // 选项C
                            "(?:D、([\\s\\S]+?))?" + // 选项D
                            "(?=\\(\\d+\\)|【材料\\d+】|$)", // 终止符：下一个问题（如“(2)”）、下一个材料、文档结束
                    Pattern.DOTALL
            );
            Matcher quesMatcher = materialQuesPattern.matcher(materialContent);

            while (quesMatcher.find()) {
                Question question = new Question();
                // 封装材料信息（题干前拼接材料标识，便于区分）
                String questionStem = "【材料" + materialNo + "】" + quesMatcher.group(2).trim();
                // 提取答案（去空格，支持多选如“AB”“A,B”）
                String answer = quesMatcher.group(3).replaceAll("\\s|,", "").trim();
                // 提取选项（无选项时设为空字符串）
                String optionA = quesMatcher.group(4) != null ? quesMatcher.group(4).trim() : "";
                String optionB = quesMatcher.group(5) != null ? quesMatcher.group(5).trim() : "";
                String optionC = quesMatcher.group(6) != null ? quesMatcher.group(6).trim() : "";
                String optionD = quesMatcher.group(7) != null ? quesMatcher.group(7).trim() : "";

                // 设置题目属性
                question.setContent(questionStem);
                question.setOptionA(optionA);
                question.setOptionB(optionB);
                question.setOptionC(optionC);
                question.setOptionD(optionD);
                question.setAnswer(answer);
                question.setQuestionType("材料分析题"); // 标记题目类型
                question.setExplanation(""); // 解析留空（可后续手动补充）

                materialQuestions.add(question);
            }
        }
        return materialQuestions;
    }

    /**
     * 提取单选题和多选题（区分题型，处理跨段落题目）
     */
    private List<Question> parseSingleAndMultiQuestions(String content) {
        List<Question> questions = new ArrayList<>();
        // 先确定当前处理的题型（默认从单选题开始）
        String currentQuestionType = "单选题";

        // 遍历题型标识，分割内容并匹配对应题型的题目
        for (String marker : QUESTION_TYPE_MARKERS) {
            if (content.contains(marker)) {
                // 提取当前题型的内容块（如“一､单项选择题”到“二､多项选择题”之间的内容）
                String[] parts = content.split(Pattern.quote(marker), 2);
                if (parts.length < 2) continue;

                // 更新当前题型
                if (marker.contains("单项选择题")) {
                    currentQuestionType = "单选题";
                } else if (marker.contains("多项选择题")) {
                    currentQuestionType = "多选题";
                }

                // 匹配当前题型下的所有题目（支持题干跨段落）
                Pattern quesPattern = Pattern.compile(
                        "(\\d+)、([^\\n]+?)" + // 题干只匹配“不换行的内容”（若题干换行，可改为 [^A、]+?）
                                "\\((\\s*[A-D]\\s*)\\)\\s*" +
                                "A、([^B、]+?)" + // 选项A只匹配到“B、”前
                                "B、([^C、]+?)" + // 选项B只匹配到“C、”前
                                "C、([^D、]+?)" + // 选项C只匹配到“D、”前
                                "D、([^\\d]+?)(?=\\d+、|$)", // 选项D只匹配到“下一题编号”前
                        Pattern.CASE_INSENSITIVE
                );
                Matcher matcher = quesPattern.matcher(parts[1]);

                while (matcher.find()) {
                    Question question = new Question();
                    // 提取题干（去重首尾空白）
                    String contentStem = matcher.group(2).trim();
                    // 提取答案（去空格和逗号，支持多选如“AB”“A,B”）
                    String answer = matcher.group(7).replaceAll("\\s|,", "").trim();
                    // 提取选项（去重首尾空白，处理选项内换行）
                    String optionA = matcher.group(3).replaceAll("\\n", " ").trim();
                    String optionB = matcher.group(4).replaceAll("\\n", " ").trim();
                    String optionC = matcher.group(5).replaceAll("\\n", " ").trim();
                    String optionD = matcher.group(6).replaceAll("\\n", " ").trim();

                    // 设置题目属性
                    question.setContent(contentStem);
                    question.setOptionA(optionA);
                    question.setOptionB(optionB);
                    question.setOptionC(optionC);
                    question.setOptionD(optionD);
                    question.setAnswer(answer);
                    question.setQuestionType(currentQuestionType); // 标记题型
                    question.setExplanation("");

                    questions.add(question);
                }

                // 更新剩余内容，继续处理下一个题型
                content = parts[1];
            }
        }
        return questions;
    }

    /**
     * 移除文档中的材料分析题部分（避免重复提取）
     */
    private String removeMaterialSections(String content) {
        return content.replaceAll("【材料\\d+】[\\s\\S]+?(?=【材料\\d+】|$)", "");
    }

    /**
     * 重置题目序号（按提取顺序统一编号，避免材料题与单选/多选题序号混乱）
     */
    private void resetQuestionSeqOrder(List<Question> questionList) {
        int seqOrder = 1;
        for (Question question : questionList) {
            question.setSeqOrder(seqOrder++);
        }
    }
}