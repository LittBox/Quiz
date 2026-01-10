package com.example.quiz.service;

import com.example.quiz.model.Question;
import java.util.List;

public interface QuestionService {
    
    // 获取题目总数
    Long getTotalCount();
    
    // 根据ID获取题目
    Question getQuestionById(Integer id);
    
    // 获取下一题
    Question getNextQuestion(Integer currentId);
    
    // 获取上一题
    Question getPreviousQuestion(Integer currentId);
    
    // 获取第一题
    Question getFirstQuestion();
    
    // 获取最后一题
    Question getLastQuestion();
    
    // 添加题目
    Question addQuestion(Question question);
    
    // 批量添加题目
    List<Question> addQuestions(List<Question> questions);
    
    // 更新题目
    Question updateQuestion(Integer id, Question question);
    
    // 删除题目
    void deleteQuestion(Integer id);
    
    // 根据类型获取题目
    List<Question> getQuestionsByType(String type);
    
    // 随机获取题目
    Question getRandomQuestion();
    
    // 获取指定数量的随机题目
    List<Question> getRandomQuestions(Integer count);
    
    // 搜索题目
    List<Question> searchQuestions(String keyword);
    
    // 获取所有题目（分页）
    List<Question> getAllQuestions(Integer page, Integer size);
    
    // 批量导入题目
    Integer importQuestions(String textContent);
}