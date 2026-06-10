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
    void doChat() {

        String userChatId = UUID.randomUUID().toString();

        //第一轮
        String userMessage = "你好，我是作者小白";
        String answer = novelApp.doChat(userMessage, userChatId);
        //第二轮
        userMessage = "你好，请结合金庸、古龙、黄易进行创作一篇武侠小说，先写大纲";
        answer = novelApp.doChat(userMessage, userChatId);
        Assertions.assertNotNull(answer);
        //第三轮
        userMessage = "生成第一章的内容";
        answer = novelApp.doChat(userMessage, userChatId);
        Assertions.assertNotNull(answer);
    }


    @Test
    void doChatWithReport() {
        String userChatId = UUID.randomUUID().toString();
        String userMessage = "结合金庸、古龙、黄易风格，写一个少年从江湖底层崛起的故事的大纲，投向起点";

        // 生成小说的大纲
        NovelApp.NovelReport outline = novelApp.doChatWithReport(userMessage, userChatId);

        System.out.println("大纲：");
        System.out.println(outline.reportType().toString());
        System.out.println(outline.content());

        System.out.println("=====================================");
        // 生成正文 第一章
        userMessage = "根据生成的大纲实现第一章的内容";
        NovelApp.NovelReport chapter1 = novelApp.doChatWithReport(userMessage, userChatId);

        System.out.println("==========第一章正文：===============");
        System.out.println(chapter1.reportType().toString());
        System.out.println("======");
        System.out.println(chapter1.content());


        // 第2章（AI 会自动回顾第1章和设定）
        userMessage = "根据生成的大纲实现第二章的内容";
        NovelApp.NovelReport chapter2 = novelApp.doChatWithReport(userMessage, userChatId);

        System.out.println("=== 第2章 ===");
        System.out.println(chapter2.reportType().toString());
        System.out.println("======");
        System.out.println(chapter2.content());

    }
}