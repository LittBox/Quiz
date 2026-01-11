package com.example.quiz.controller;

import com.example.quiz.model.Question;
import com.example.quiz.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;

    @Autowired
    private com.example.quiz.repo.QuestionRepository questionRepository;
    
    // 获取题目总数
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getQuestionCount() {
        Long count = questionService.getTotalCount();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    
    // 根据ID获取题目
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Integer id) {
        Question question = questionService.getQuestionById(id);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }
    
    // 获取下一题
    @GetMapping("/next")
    public ResponseEntity<Question> getNextQuestion(@RequestParam(required = false) Integer currentId) {
        Question question = questionService.getNextQuestion(currentId);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }
    
    // 获取上一题
    @GetMapping("/prev")
    public ResponseEntity<Question> getPrevQuestion(@RequestParam(required = false) Integer currentId) {
        Question question = questionService.getPreviousQuestion(currentId);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }
    
    // 获取第一题
    @GetMapping("/first")
    public ResponseEntity<Question> getFirstQuestion() {
        Question question = questionService.getFirstQuestion();
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }
    
    // 随机获取题目
    @GetMapping("/random")
    public ResponseEntity<Question> getRandomQuestion() {
        Question question = questionService.getRandomQuestion();
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }
    
    // 添加题目
    @PostMapping
    public ResponseEntity<Question> addQuestion(@RequestBody Question question) {
        Question savedQuestion = questionService.addQuestion(question);
        return ResponseEntity.ok(savedQuestion);
    }
    
    // 批量添加题目
    @PostMapping("/batch")
    public ResponseEntity<List<Question>> addQuestions(@RequestBody List<Question> questions) {
        List<Question> savedQuestions = questionService.addQuestions(questions);
        return ResponseEntity.ok(savedQuestions);
    }
    
    // 更新题目
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Integer id, @RequestBody Question question) {
        Question updatedQuestion = questionService.updateQuestion(id, question);
        if (updatedQuestion == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedQuestion);
    }
    
    // 删除题目
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Integer id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok().build();
    }
    
    // 根据类型查询
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Question>> getQuestionsByType(@PathVariable String type) {
        List<Question> questions = questionService.getQuestionsByType(type);
        return ResponseEntity.ok(questions);
    }
    
    // 搜索题目
    @GetMapping("/search")
    public ResponseEntity<List<Question>> searchQuestions(@RequestParam String keyword) {
        List<Question> questions = questionService.searchQuestions(keyword);
        return ResponseEntity.ok(questions);
    }
    
    // 获取所有题目（分页）
    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<Question> questions = questionService.getAllQuestions(page, size);
        return ResponseEntity.ok(questions);
    }
    
    // 批量导入题目
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importQuestions(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "导入内容不能为空"));
        }
        
        Integer count = questionService.importQuestions(content);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("importedCount", count);
        response.put("message", "成功导入 " + count + " 道题目");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getAllQuestionIds() {
        List<Question> questions = questionRepository.findAll();
        List<Long> ids = questions.stream() //开启流
                .map(Question::getId)  //等价.map( question -> question.getId() )
                .collect(Collectors.toList()); //收集结果，终止流（收尾操作） Collectors.toList() ：JDK 提供的收集器，作用是「把流中的元素收集到一个 ArrayList 集合中」
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Question>> getAllQuestions() {
        List<Question> questions = questionRepository.findAll();
        return ResponseEntity.ok(questions);
    }

    
}