package com.example.quiz.repo;

import com.example.quiz.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    

    
    // 查找下一题
    @Query("SELECT q FROM Question q WHERE q.id > :currentId ORDER BY q.id ASC")
    Question findNextQuestion(@Param("currentId") Integer currentId);
    
    // 查找上一题
    @Query("SELECT q FROM Question q WHERE q.id < :currentId ORDER BY q.id DESC")
    Question findPreviousQuestion(@Param("currentId") Integer currentId);
    
    // 按类型查询
    List<Question> findByQuestionType(String questionType);
    
    // 统计题目总数
    @Query("SELECT COUNT(q) FROM Question q")
    Long countAllQuestions();
    
    // 获取最小ID
    @Query("SELECT MIN(q.id) FROM Question q")
    Integer findMinId();
    
    // 获取最大ID
    @Query("SELECT MAX(q.id) FROM Question q")
    Integer findMaxId();
    
    // 分页查询（用于管理界面）
    @Query("SELECT q FROM Question q ORDER BY q.id ASC")
    List<Question> findAllWithPagination();
}