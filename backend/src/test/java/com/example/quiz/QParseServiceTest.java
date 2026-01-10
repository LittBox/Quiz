package com.example.quiz;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.quiz.model.Question;
import com.example.quiz.service.impl.ParseServiceImpl;

@SpringBootTest
public class QParseServiceTest {
    
    @Autowired
    private ParseServiceImpl parseServiceImpl;
    
   

    @Test
    public void testParseService() {
        String testDocument = """
                                    1、马克思主义哲学区别于其他哲学的最显著特征是 ( C )
                                    A、实践性
                                    B、科学性
                                    C、革命性
                                    D、阶级性

                                    2、下列哪种不是Java的基本数据类型 ( D )
                                    A、int
                                    B、boolean
                                    C、char
                                    D、String

                                    3、HTTP协议中，用于请求资源的方法不包括以下哪一个 ( B )
                                    A、GET
                                    B、POST
                                    C、DELETE
                                    D、PUSH

                                    4、数据库中，用于确保数据唯一性的约束是 ( A )
                                    A、PRIMARY KEY
                                    B、FOREIGN KEY
                                    C、CHECK
                                    D、DEFAULT

                                    5、Vue3中，组合式API的核心函数是 ( C )
                                    A、Vue.component()
                                    B、Vue.use()
                                    C、setup()
                                    D、createApp()
                                    """   ;
        List<Question> questions = parseServiceImpl.parseDocument(testDocument);
        for (Question q : questions) {
            System.out.println("Question: " + q.getContent());
            System.out.println("A: " + q.getOptionA());
            System.out.println("B: " + q.getOptionB());
            System.out.println("C: " + q.getOptionC());
            System.out.println("D: " + q.getOptionD());
            System.out.println("Answer: " + q.getAnswer());
            System.out.println("Explanation: " + q.getExplanation());
            System.out.println("SeqOrder: " + q.getSeqOrder());
            System.out.println("---------------------------");
        }
    }
}
