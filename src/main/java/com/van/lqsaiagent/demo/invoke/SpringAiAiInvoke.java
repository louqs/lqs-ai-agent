package com.van.lqsaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * spring ai框架调用ai大模型
 */
//@Component
//public class SpringAiAiInvoke implements CommandLineRunner {
//
//    @Resource
//    private ChatModel dashscopeChatModel;
//
//    @Override
//    public void run(String... args) throws Exception {
//        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你是谁？"))
//                .getResult()
//                .getOutput();
//        System.out.printf("ai回答:%s%n", assistantMessage.getText());
//    }
//}
