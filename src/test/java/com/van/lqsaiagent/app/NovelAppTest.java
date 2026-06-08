package com.van.lqsaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NovelAppTest {

    @Resource
    private NovelApp novelApp;

    @Test
    void testDoChat() {

        String userChatId = UUID.randomUUID().toString();

        //第一轮
        String userMessage = "你好，我是作者小白";
        String answer = novelApp.doChat(userMessage, userChatId);
        //第二轮
        userMessage = "你好，请结合金庸、古龙、黄易进行创作一篇武侠小说，先生成第一章的内容";
        answer = novelApp.doChat(userMessage, userChatId);
        Assertions.assertNotNull(answer);
        //第三轮
        userMessage = "你好，请接着写第二章的内容";
        answer = novelApp.doChat(userMessage, userChatId);
        Assertions.assertNotNull(answer);
    }
}