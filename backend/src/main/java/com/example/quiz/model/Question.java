package com.example.quiz.model;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(columnDefinition = "TEXT")
    private String optionA;
    @Column(columnDefinition = "TEXT")
    private String optionB;
    @Column(columnDefinition = "TEXT")
    private String optionC;
    @Column(columnDefinition = "TEXT")
    private String optionD;
    @Column(columnDefinition = "TEXT")
    private String optionE;
    private String answer; // e.g. A
    @Column(length = 255, name = "question_type")
    private String questionType;
     
    @Column(length = 10)
    private String chapter;
   
}
