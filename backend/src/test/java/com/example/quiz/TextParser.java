package com.example.quiz;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.quiz.model.Question;
import com.example.quiz.repo.QuestionRepository;
import com.example.quiz.service.impl.TikaServiceImpl;
import com.example.utils.CommonUtil;

@SpringBootTest
public class TextParser {

    @Autowired
    private TikaServiceImpl tikaServiceImpl;


    @Test
    public  void testRegex() {
        String text = "核心主题: 不选可调天幕是否会热\n" +
                "主要观点: 不选可调天幕在高温天气下可能会很热\n" +
                "论据: 可调天幕可以在高温天气下调节车内温度，降低车内温度，而不选可调天幕在高温天气下可能会因为无法调节车内温度而使车内变得更加炎热\n" +
                "情绪分数: 1\n" +
                 "核心主题: 不选可调天幕是否会热\n" +
                "主要观点: 不选可调天幕在高温天气下可能会很热\n" +
                "论据: 可调天幕可以在高温天气下调节车内温度，降低车内温度，而不选可调天幕在高温天气下可能会因为无法调节车内温度而使车内变得更加炎热\n" +
                "情绪分数: 1";

        // 将全角字符转换为半角字符
        text = CommonUtil.fullWidthToHalfWidth(text);

        // 定义正则表达式
        String regex = "核心主题:(.*?)\\n主要观点:(.*?)\\n论据:(.*?)\\n情绪分数:(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        List<ContentBean> contentBeans = new ArrayList<>();
        while (matcher.find()) {
            String coreTheme = matcher.group(1);
            String mainPoints = matcher.group(2);
            String evidence = matcher.group(3);
            String emotionScore = matcher.group(4);

            System.out.println("核心主题: " + coreTheme);
            System.out.println("主要观点: " + mainPoints);
            System.out.println("论据: " + evidence);
            System.out.println("情绪分数: " + emotionScore);

            ContentBean bean = new ContentBean();
            bean.setCoreTheme(coreTheme.trim());
            bean.setMainPoints(mainPoints.trim());
            bean.setEvidence(evidence.trim());
            bean.setEmotionScore(emotionScore.trim());
            contentBeans.add(bean);
        } 
        System.out.println("总共解析出 " + contentBeans.size() + " 个内容块。");
    }



    @Autowired
    private QuestionRepository questionRepository;

    @Test
    public void testImport() throws Exception {
        // 这一个正则就够了！一次性捕获 题号、题干、答案、A/B/C/D四个选项 全部内容
        String regex =   "(\\d+)[、\\.]\\s*([^(]+?)\\s*\\(\\s*([A-Z]+)\\s*\\)\\s*A[、\\.]\\s*(.+?)\\s*B[、\\.]\\s*(.+?)\\s*C[、\\.]\\s*(.+?)\\s*D[、\\.]\\s*(.+?)(?:\\s*E[、\\.]\\s*(.+?))?(?=\\s*\\d+[、\\.]|$)";
        Pattern pattern = Pattern.compile(regex);
        File file = new File("D:\\BaiduNetdiskDownload\\test.pdf");
        String result = tikaServiceImpl.extractTextFromPdf(file);


        // 文本预处理：统一格式，消除PDF提取的常见干扰
            String processedResult = result
            // 1. 统一题号格式（将"1．"、"1."等统一为"1、"）
            .replaceAll("(\\d+)[．\\.](?!\\s*[A-D])", "$1、")
            // 2. 统一选项分隔符
            .replace("A.", "A、")
            .replace("B.", "B、")
            .replace("C.", "C、")
            .replace("D.", "D、")
            .replace("E.", "E、")
            // 3. 处理全角括号和空格
            .replace("（", "(")
            .replace("）", ")")
            .replace("　", " ")
            .replace(") ", ")")
            .replace(" )", ")")
            .replace("( ", "(")
            .replace(" (", "(")
            // 4. 处理多选题答案空格
            .replace("(A B C D)", "(ABCD)")
            .replace("(A B C)", "(ABC)")
            .replace("(A B D)", "(ABD)")
            .replace("(A C D)", "(ACD)")
            .replace("(B C D)", "(BCD)")
            .replace("(A B)", "(AB)")
            .replace("(A C)", "(AC)")
            .replace("(A D)", "(AD)")
            .replace("(B C)", "(BC)")
            .replace("(B D)", "(BD)")
            .replace("(C D)", "(CD)")
            // 5. 去除章节标题
            .replace("第一章", "")
            .replace("第二章", "")
            .replace("第三章", "")
            .replace("第四章", "")
            .replace("第五章", "")
            .replace("第六章", "")
            .replace("绪论", "")
            .replace("一、单项选择题", "")
            .replace("二、多项选择题", "")
            .replace("三、材料分析题", "")
            .replace("《马克思主义基本原理概论》题库", "")
            // 6. 处理连续空白和换行
            .replaceAll("\\s+", " ")
            // 7. 特别处理：将换行后的题号连接到上一行
            .replaceAll("\\s*(\\d+)[、\\.]\\s*", " $1、")
            // 8. 去除无意义字符
            .replaceAll("[｡\\u0000-\\u001F]", "")
            .trim();
                

        // 🔴 关键：打印预处理后的文本片段，确认格式是否统一（重点看题号、答案、选项的格式）
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
            question.setSeqOrder(Integer.parseInt(matcher.group(1).trim()));
            question.setContent(matcher.group(2).trim());
            question.setAnswer(matcher.group(3).trim());
            question.setOptionA(matcher.group(4).trim());
            question.setOptionB(matcher.group(5).trim());
            question.setOptionC(matcher.group(6).trim());
            question.setOptionD(matcher.group(7).trim());

            // 处理E选项（如果有）
            if (matcher.group(8) != null && !matcher.group(8).trim().isEmpty()) {
                question.setOptionE(matcher.group(8).trim());
                System.out.println("题目 #" + question.getSeqOrder() + " 包含E选项");
            }

            questions.add(question);
            
            questionRepository.save(question);

        }
        System.out.println("总共解析出 " + questions.size() + " 道题目。");
    }


}
