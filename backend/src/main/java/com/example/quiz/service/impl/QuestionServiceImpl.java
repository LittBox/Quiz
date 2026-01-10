package com.example.quiz.service.impl;

import com.example.quiz.model.Question;
import com.example.quiz.repo.QuestionRepository;
import com.example.quiz.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionServiceImpl implements QuestionService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Override
    public Long getTotalCount() {
        return questionRepository.count();
    }
    
    @Override
    public Question getQuestionById(Integer id) {
        if (id == null || id <= 0) {
            return getFirstQuestion();
        }
        
        Optional<Question> question = questionRepository.findById(id);
        return question.orElse(getFirstQuestion());
    }
    
    @Override
    public Question getNextQuestion(Integer currentId) {
        if (currentId == null) {
            return getFirstQuestion();
        }
        
        // 获取所有题目ID并按顺序排序
        List<Question> allQuestions = questionRepository.findAll();
        allQuestions.sort(Comparator.comparing(Question::getId));
        
        for (int i = 0; i < allQuestions.size(); i++) {
            if (allQuestions.get(i).getId().equals(currentId) && i + 1 < allQuestions.size()) {
                return allQuestions.get(i + 1);
            }
        }
        
        // 如果是最后一题，返回第一题
        return getFirstQuestion();
    }
    
    @Override
    public Question getPreviousQuestion(Integer currentId) {
        if (currentId == null) {
            return getFirstQuestion();
        }
        
        // 获取所有题目ID并按顺序排序
        List<Question> allQuestions = questionRepository.findAll();
        allQuestions.sort(Comparator.comparing(Question::getId));
        
        for (int i = 0; i < allQuestions.size(); i++) {
            if (allQuestions.get(i).getId().equals(currentId) && i > 0) {
                return allQuestions.get(i - 1);
            }
        }
        
        // 如果是第一题，返回最后一题
        return getLastQuestion();
    }
    
    @Override
    public Question getFirstQuestion() {
        List<Question> allQuestions = questionRepository.findAll();
        if (allQuestions.isEmpty()) {
            return null;
        }
        
        allQuestions.sort(Comparator.comparing(Question::getId));
        return allQuestions.get(0);
    }
    
    @Override
    public Question getLastQuestion() {
        List<Question> allQuestions = questionRepository.findAll();
        if (allQuestions.isEmpty()) {
            return null;
        }
        
        allQuestions.sort(Comparator.comparing(Question::getId));
        return allQuestions.get(allQuestions.size() - 1);
    }
    
    @Override
    @Transactional
    public Question addQuestion(Question question) {
        if (question == null) {
            throw new IllegalArgumentException("题目不能为空");
        }
        
        return questionRepository.save(question);
    }
    
    @Override
    @Transactional
    public List<Question> addQuestions(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }
    
    @Override
    @Transactional
    public Question updateQuestion(Integer id, Question question) {
        Optional<Question> existingQuestionOpt = questionRepository.findById(id);
        if (existingQuestionOpt.isEmpty()) {
            throw new RuntimeException("题目不存在，ID: " + id);
        }
        
        Question existingQuestion = existingQuestionOpt.get();
        
        // 更新字段
        if (question.getContent() != null) {
            existingQuestion.setContent(question.getContent());
        }
        if (question.getAnswer() != null) {
            existingQuestion.setAnswer(question.getAnswer());
        }
        if (question.getQuestionType() != null) {
            existingQuestion.setQuestionType(question.getQuestionType());
        }
        if (question.getOptionA() != null) {
            existingQuestion.setOptionA(question.getOptionA());
        }
        if (question.getOptionB() != null) {
            existingQuestion.setOptionB(question.getOptionB());
        }
        if (question.getOptionC() != null) {
            existingQuestion.setOptionC(question.getOptionC());
        }
        if (question.getOptionD() != null) {
            existingQuestion.setOptionD(question.getOptionD());
        }
        if (question.getOptionE() != null) {
            existingQuestion.setOptionE(question.getOptionE());
        }
        if (question.getExplanation() != null) {
            existingQuestion.setExplanation(question.getExplanation());
        }
        if (question.getSeqOrder() != null) {
            existingQuestion.setSeqOrder(question.getSeqOrder());
        }
        
        return questionRepository.save(existingQuestion);
    }
    
    @Override
    @Transactional
    public void deleteQuestion(Integer id) {
        questionRepository.deleteById(id);
    }
    
    @Override
    public List<Question> getQuestionsByType(String type) {
        return questionRepository.findByQuestionType(type);
    }
    
    @Override
    public Question getRandomQuestion() {
        List<Question> allQuestions = questionRepository.findAll();
        if (allQuestions.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        return allQuestions.get(random.nextInt(allQuestions.size()));
    }
    
    @Override
    public List<Question> getRandomQuestions(Integer count) {
        List<Question> allQuestions = questionRepository.findAll();
        if (allQuestions.isEmpty()) {
            return Collections.emptyList();
        }
        
        if (count == null || count <= 0 || count >= allQuestions.size()) {
            return allQuestions;
        }
        
        Collections.shuffle(allQuestions);
        return allQuestions.subList(0, count);
    }
    
    @Override
    public List<Question> searchQuestions(String keyword) {
        List<Question> allQuestions = questionRepository.findAll();
        if (keyword == null || keyword.trim().isEmpty()) {
            return allQuestions;
        }
        
        List<Question> result = new ArrayList<>();
        String searchTerm = keyword.trim().toLowerCase();
        
        for (Question question : allQuestions) {
            if ((question.getContent() != null && question.getContent().toLowerCase().contains(searchTerm)) ||
                (question.getOptionA() != null && question.getOptionA().toLowerCase().contains(searchTerm)) ||
                (question.getOptionB() != null && question.getOptionB().toLowerCase().contains(searchTerm)) ||
                (question.getOptionC() != null && question.getOptionC().toLowerCase().contains(searchTerm)) ||
                (question.getOptionD() != null && question.getOptionD().toLowerCase().contains(searchTerm)) ||
                (question.getExplanation() != null && question.getExplanation().toLowerCase().contains(searchTerm))) {
                result.add(question);
            }
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public Integer importQuestions(String textContent) {
        if (textContent == null || textContent.trim().isEmpty()) {
            return 0;
        }
        
        List<Question> questions = parseQuestionsFromText(textContent);
        if (questions.isEmpty()) {
            return 0;
        }
        
        // 保存到数据库
        List<Question> savedQuestions = addQuestions(questions);
        
        return savedQuestions.size();
    }
    
    /**
     * 从文本解析题目
     */
    private List<Question> parseQuestionsFromText(String text) {
        List<Question> questions = new ArrayList<>();
        
        // 预处理文本
        String processedText = text
                .replaceAll("\\s+", " ")
                .replace("A.", "A、")
                .replace("B.", "B、")
                .replace("C.", "C、")
                .replace("D.", "D、")
                .replace("E.", "E、")
                .replace("（", "(")
                .replace("）", ")")
                .trim();
        
        // 正则表达式匹配题目
        String regex = "(\\d+)[、\\.]\\s*([^(]+?)\\s*\\(\\s*([A-Z]+)\\s*\\)\\s*A[、\\.]\\s*(.+?)\\s*B[、\\.]\\s*(.+?)\\s*C[、\\.]\\s*(.+?)\\s*D[、\\.]\\s*(.+?)(?=\\s*(?:E[、\\.]|\\d+[、\\.]|$))";
        
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(processedText);
        
        while (matcher.find()) {
            try {
                Question question = new Question();
                question.setSeqOrder(Integer.parseInt(matcher.group(1).trim()));
                question.setContent(matcher.group(2).trim());
                
                String answer = matcher.group(3).trim();
                question.setAnswer(answer);
                question.setQuestionType(answer.length() == 1 ? "single_choice" : "multi_choice");
                
                question.setOptionA(matcher.group(4).trim());
                question.setOptionB(matcher.group(5).trim());
                question.setOptionC(matcher.group(6).trim());
                question.setOptionD(matcher.group(7).trim());
                
                questions.add(question);
            } catch (Exception e) {
                System.err.println("解析题目失败: " + e.getMessage());
            }
        }
        
        return questions;
    }

    @Override
    public List<Question> getAllQuestions(Integer page, Integer size) {
        if (page == null || size == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        throw new UnsupportedOperationException("Unimplemented method 'getAllQuestions'");
    }
}