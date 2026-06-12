package com.van.lqsaiagent.app;


import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.van.lqsaiagent.advisor.MyLoggerAdvisor;
import com.van.lqsaiagent.advisor.ReReadingAdvisor;
import com.van.lqsaiagent.chatmemory.FileBasedChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.util.Map;

@Component
@Slf4j
public class NovelApp {

    private final ChatClient chatClient;

    private final Resource systemPromptResource;
    private final Resource writingRulesResource;

    /**
     * 初始化ai客户端
     *
     * @param dashscopeChatModel 阿里云百炼   灵积模型
     */
    public NovelApp(ChatModel dashscopeChatModel,
                    @Value("classpath:prompts/system-prompt.md") Resource systemPromptResource,
                    @Value("classpath:prompts/writing-rules.md") Resource writingRulesResource) {
        this.systemPromptResource = systemPromptResource;
        this.writingRulesResource = writingRulesResource;

        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemoryRepository chatMemoryRepository = new FileBasedChatMemoryRepository(fileDir);


        // 用仓库创建 ChatMemory（滑动窗口，最多保留30次对话）
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(60)
                .build();

        String systemPrompt = loadPrompt(this.systemPromptResource);

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),

                        // 自定义推理增强Advisor，可按需开启。由于我的场景是中文小说创意生成，对“发散性”要求高于“精确推理”，Re2 的收益可能不明显，反而限制了创作的跳跃感。
                        // new ReReadingAdvisor(),

                        // 自定义日志advisor，可按需开启
                        new MyLoggerAdvisor()
                )
                .build();

    }


    /**
     * 从 Resource 文件加载提示词并通过 PromptTemplate 渲染
     *
     * @param resource classpath 下的提示词文件资源
     * @return 渲染后的提示词内容
     */
    private String loadPrompt(Resource resource) {
        try {
            String templateContent = StreamUtils.copyToString(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
            return PromptTemplate.builder()
                    .template(templateContent)
                    .variables(Map.of())
                    .build()
                    .render();
        } catch (Exception e) {
            log.error("加载提示词文件失败: {}", resource.getDescription(), e);
            throw new RuntimeException("加载提示词文件失败", e);
        }
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param userMessage 用户输入的消息
     * @param userChatId  用户对话的id
     * @return ai生成的内容
     */
    public String doChat(String userMessage, String userChatId) {
        // ChatResponse chatResponse = chatClient
        //         .prompt()
        //         .user(userMessage)
        //         .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userChatId))
        //         .call()
        //         .chatResponse();
        //
        // String content = chatResponse.getResult().getOutput().getText();
        //
        // log.info("content: {}", content);
        // return content;

        String content = chatClient
                .prompt()
                .user(userMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userChatId))
                .call()
                .content();

        log.info("content: {}", content);
        return content;
    }





    /**
     * 小说报告记录。
     *
     * @param reportType   报告类型：OUTLINE 或 CHAPTER。
     * @param content      大纲或正文的完整内容（Markdown）。

     */
    public record NovelReport(
            @JsonPropertyDescription("报告类型：OUTLINE 或 CHAPTER")
            String reportType,

            @JsonPropertyDescription("大纲或正文的完整内容（Markdown）")
            String content

    ) {

    }



    /**
     * AI 小说报告功能（结构化输出）
     *
     * @param userMessage 用户输入的消息
     * @param userChatId  用户对话的id
     * @return ai生成的内容
     */
    public NovelReport doChatWithReport(String userMessage, String userChatId) {
        String systempPrompt = loadPrompt(systemPromptResource) + loadPrompt(writingRulesResource);
        NovelReport novelReport = chatClient
                .prompt()
                .system(systempPrompt)
                .user(userMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userChatId))
                .call()
                .entity(NovelReport.class);

        log.info("novelReport: {}", novelReport);

        return novelReport;
    }


}
