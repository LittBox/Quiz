package com.example.quiz.service;

import java.util.List;

import com.example.quiz.model.Question;

public interface ParseService {
    List<Question> parseDocument(String documentContent);
}
