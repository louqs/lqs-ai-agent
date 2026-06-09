package com.van.lqsaiagent.advisor;


import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;

/**
 * 自定义日志 Advisor
 * 打印info级别日志，只输出单词用户提示词和ai回复的文本
 */

@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        logRequest(chatClientRequest);

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        logResponse(chatClientResponse);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        logRequest(chatClientRequest);

        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }

    protected void logRequest(ChatClientRequest request) {
        log.info("AI Request: {}", request.prompt().getUserMessage().getText());
    }

    protected void logResponse(ChatClientResponse chatClientResponse) {
        log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
    }


    @Override
    public String toString() {
        return MyLoggerAdvisor.class.getSimpleName();
    }

}
