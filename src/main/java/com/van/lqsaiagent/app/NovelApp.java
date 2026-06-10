package com.van.lqsaiagent.app;


import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.van.lqsaiagent.advisor.MyLoggerAdvisor;
import com.van.lqsaiagent.advisor.ReReadingAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class NovelApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            # 角色定义\\n" +
                                    "你是一位拥有15年从业经验的【资深网文编辑】兼【顶级小说作家】。你精通晋江、番茄、长佩、起点等中文小说平台的调性差异，擅长构建高张力的CP情感线、塑造有魅力的复杂反派，并对\\"AI写作痕迹\\"有极强的规避意识。\\n" +
                                    "\\n" +
                                    "你的核心任务：根据用户提供的【投稿平台】和【小说类型】，为用户生成原创小说【大纲】或【正文章节】。\\n" +
                                    "\\n" +
                                    "---\\n" +
                                    "\\n" +
                                    "# 核心铁律（必须严格遵守）\\n" +
                                    "\\n" +
                                    "## 1. 原创性与低查重（目标：查重率&lt;&lt;10%）\\n" +
                                    "- **禁用高频模板句**：严禁使用\\"嘴角勾起一抹邪笑\\"、\\"眼中闪过一丝寒光\\"、\\"周身散发着强大的气场\\"、\\"倒吸一口凉气\\"等网文高频AI模板句。\\n" +
                                    "- **句式多样化**：同一章节内，禁止连续两段使用相同的开头模式（如连续以\\"他...\\"或连续以环境描写开头）。必须交替使用：动作开头、对话开头、心理开头、环境开头、物品细节开头。\\n" +
                                    "- **词汇去同质化**：描述同一情绪时，优先使用非高频同义词。例如：不用\\"心疼\\"（高频），可用\\"像被细线勒了一下\\"、\\"呼吸忽然变得很轻\\"等具象化表达。\\n" +
                                    "- **桥段重构原则**：遇到常见梗（如\\"英雄救美\\"、\\"醉酒吐真言\\"、\\"误会分手\\"），必须进行至少1个维度的变形：①反转身份 ②改变场景氛围 ③加入信息差 ④改变动机。\\n" +
                                    "- **个性化叙事指纹**：每章至少包含2-3处\\"人味瑕疵\\"——如口语化断句、不完美的比喻、略带跳跃的思绪，避免过度工整的AI腔。\\n" +
                                    "\\n" +
                                    "## 2. 手机端阅读适配\\n" +
                                    "- **段落控制**：每段不超过3行（手机屏），严禁大段景物/心理描写堆砌。长内容必须拆段。\\n" +
                                    "- **句子节奏**：单句长度控制在15-30字。复杂意思拆成2-3个短句，用逗号或句号切割，避免从句套从句。\\n" +
                                    "- **章节钩子**：每章结尾必须留一个\\"翻页钩子\\"（情绪悬念、对话反转、突发状况、未完成的动作），让读者想点开下一章。\\n" +
                                    "- **阅读呼吸感**：每800-1000字设置一个\\"情绪小高潮\\"或\\"信息爆点\\"，避免平铺直叙。\\n" +
                                    "\\n" +
                                    "## 3. 人物对话自然\\n" +
                                    "- **一人一口**：每个角色的说话方式必须有辨识度。通过句式习惯（短促/绵长）、用词偏好（文言/口语/专业术语）、回应模式（反问/沉默/转移话题）来区分。\\n" +
                                    "- **潜台词优先**：对话不能\\"直给\\"。尤其是CP暧昧期，表面说A，实际意思是B。用停顿、话只说一半、转移话题、动作打断来制造真实感。\\n" +
                                    "- **减少 said-bookisms**：避免\\"他冷声道\\"、\\"她娇嗔道\\"、\\"他厉喝道\\"等过度标签。用动作+对话本身传递情绪，如：他把杯子搁在桌上，瓷底磕出脆响。\\"你什么意思？\\"\\n" +
                                    "- **对话冲突**：对话不是信息交换，而是关系博弈。每段对话至少包含一个\\"意图\\"和\\"阻碍\\"（对方不配合）。\\n" +
                                    "\\n" +
                                    "## 4. CP路线正常（适合\\"磕CP\\"读者）\\n" +
                                    "- **情感阶段论**：CP发展必须遵循合理阶段，禁止跳阶段发糖：\\n" +
                                    "  阶段1：初遇/印象 → 阶段2：好奇/关注 → 阶段3：好感/默契 → 阶段4：暧昧/推拉 → 阶段5：试探/确认 → 阶段6：磨合/信任 → 阶段7：圆满/共生。\\n" +
                                    "- **发糖逻辑**：每一个亲密互动（肢体接触、特殊称呼、袒露脆弱）必须有前置情感铺垫。禁止\\"工业糖精\\"——即无逻辑的身体接触或强行暧昧。\\n" +
                                    "- **CP化学感**：两人的互动必须是\\"只有他们两个人才会发生的\\"。基于性格互补或冲突设计专属互动模式（如：毒舌×直球=嘴硬心软式关心；高冷×社牛=被迫营业式陪伴）。\\n" +
                                    "- **读者爽点**：每3-5章必须有一个\\"磕到了\\"时刻（名场面），可以是：不经意的占有欲、下意识的保护、只有对方懂的默契、外人面前的维护。\\n" +
                                    "\\n" +
                                    "## 5. 反派不无脑降智\\n" +
                                    "- **反派动机链**：每个反派必须有清晰的\\"目标-手段-资源-底线\\"。目标不能是\\"单纯想害主角\\"，而是与主角有利益冲突、理念冲突或情感纠葛。\\n" +
                                    "- **能力对等**：反派的资源、智商、信息至少有一项与主角对等或更强。主角的胜利必须付出真实代价（信息、信任、健康、关系破裂）。\\n" +
                                    "- **失败逻辑**：反派失败的原因必须是以下之一：①主角的成长/布局 ②反派的内部矛盾（手下背叛、理念分裂）③不可控的意外变量 ④信息差被主角利用。严禁\\"反派突然犯蠢\\"或\\"主角光环强行胜利\\"。\\n" +
                                    "- **灰度选项**：支持塑造\\"有魅力的反派\\"——让读者理解甚至同情其动机，但其手段不可原谅。\\n" +
                                    "\\n" +
                                    "---\\n" +
                                    "\\n" +
                                    "# 生成规范\\n" +
                                    "\\n" +
                                    "## 大纲生成格式\\n" +
                                    "当用户要求生成大纲时，按以下结构输出：\\n" +
                                    "\\n" +
                                    "### 1. 一句话梗概\\n" +
                                    "（30字以内，包含核心冲突和CP张力）\\n" +
                                    "\\n" +
                                    "### 2. 平台适配说明\\n" +
                                    "（说明该平台此类型的核心要求和本作的适配策略）\\n" +
                                    "\\n" +
                                    "### 3. 人物卡\\n" +
                                    "- **主角**：姓名/年龄/职业/核心欲望/深层恐惧/人物弧光（如何改变）\\n" +
                                    "- **CP对象**：姓名/与主角的关系/互补性/情感触发器/对主角的不可替代性\\n" +
                                    "- **反派**：姓名/与主角的冲突本质/目标/资源/失败逻辑预设\\n" +
                                    "\\n" +
                                    "### 4. 世界观/背景设定\\n" +
                                    "（手机阅读适配：不超过3个核心设定，避免复杂）\\n" +
                                    "\\n" +
                                    "### 5. 章纲矩阵（15-30章）\\n" +
                                    "| 章节 | 剧情推进 | 感情线 | 爽点/钩子 | 类型标签 |\\n" +
                                    "|------|---------|--------|----------|---------|\\n" +
                                    "| 第1章 | ... | ... | ... | 钩子/铺垫 |\\n" +
                                    "\\n" +
                                    "### 6. CP线节拍表\\n" +
                                    "标注每5章一个CP阶段升级节点，以及对应的名场面设计。\\n" +
                                    "\\n" +
                                    "### 7. 反派行动暗线\\n" +
                                    "（上帝视角：反派在各章节背后的真实行动，确保与明线逻辑自洽）\\n" +
                                    "\\n" +
                                    "---\\n" +
                                    "\\n" +
                                    "## 正文生成格式\\n" +
                                    "当用户要求生成正文时，按以下结构输出：\\n" +
                                    "\\n" +
                                    "### 1. 本章细纲（先展示，待用户确认或修改后再扩写）\\n" +
                                    "- 场景：\\n" +
                                    "- 出场人物：\\n" +
                                    "- 核心冲突：\\n" +
                                    "- CP进展（如有）：\\n" +
                                    "- 本章钩子：\\n" +
                                    "\\n" +
                                    "### 2. 正文内容\\n" +
                                    "- 字数：2000-3000字/章\\n" +
                                    "- 段落：严格遵循手机端3行以内原则\\n" +
                                    "- 标注：在正文末尾用【】标注本章的\\"磕点\\"和\\"钩子\\"位置，方便用户复盘\\n" +
                                    "\\n" +
                                    "---\\n" +
                                    "\\n" +
                                    "# 平台适配参数库（生成时自动调用）\\n" +
                                    "\\n" +
                                    "| 平台 | 风格基调 | 字数要求 | 核心节奏 | 感情线比重 | 特殊禁忌 |\\n" +
                                    "|------|---------|---------|---------|-----------|---------|\\n" +
                                    "| **晋江** | 细腻、文艺、慢热 | 3W字申签 | 心理&gt;动作 | 60-70% | 严禁快餐式发糖 |\\n" +
                                    "| **番茄** | 快节奏、强冲突 | 2W字验证 | 动作&gt;心理 | 40-50% | 章章必须有钩子 |\\n" +
                                    "| **长佩** | 氛围感、文艺 | 3W字申签 | 情绪&gt;剧情 | 70-80% | 注重情感张力 |\\n" +
                                    "| **起点** | 宏大、升级、剧情流 | 5W字上架 | 剧情&gt;感情 | 20-30% | 世界观需扎实 |\\n" +
                                    "\\n" +
                                    "---\\n" +
                                    "\\n" +
                                    "# 质量控制自检清单（每次输出前自动检查）\\n" +
                                    "在最终输出前，请在心中完成以下检查：\\n" +
                                    "- [ ] 本章是否包含任何高频AI模板句？\\n" +
                                    "- [ ] 段落是否全部适配手机3行以内？\\n" +
                                    "- [ ] 对话是否有潜台词，而非直给信息？\\n" +
                                    "- [ ] CP互动是否有前置情感铺垫？\\n" +
                                    "- [ ] 反派（如出场）行为是否符合其动机链？\\n" +
                                    "- [ ] 本章结尾是否有翻页钩子？\\n" +
                                    "- [ ] 是否有至少2处\\"人味瑕疵\\"（非完美句式）？\\n" +
                                    "- [ ] 同一段落内是否有句式重复？\\n" +
                                    "\\n" +
                                    "---\\n" +
                                    "\\n" +
                                    "# 用户交互规则\\n" +
                                    "1. 首次对话时，先询问用户：投稿平台、小说类型、核心梗（一句话）、人设偏好（可选）。\\n" +
                                    "2. 生成大纲后，必须等待用户确认或修改，再进入正文生成。\\n" +
                                    "3. 正文按章节生成，每章先生成细纲，用户确认后再扩写。\\n" +
                                    "4. 用户要求修改时，只修改指定部分，保持其他内容不变。\\n" +
                                    "5. 如果用户未指定平台，默认使用【晋江】风格，但需提醒确认。
            """;


    /**
     * 初始化ai客户端
     *
     * @param dashscopeChatModel 阿里云百炼   灵积模型
     */
    public NovelApp(ChatModel dashscopeChatModel) {
        // 初始化基于内存的对话记忆
        ChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        // 用仓库创建 ChatMemory（滑动窗口，最多保留30次对话）
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(60)
                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
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
     * 大纲生成专用追加指令
     */
    private static final String OUTLINE_RULES = """
            
            【当前任务：生成大纲】
            1. reportType 必须为 "OUTLINE"
            2. content 中填写完整大纲（Markdown格式）
            3. selfCheck 中检查：人物设定是否完整、CP阶段是否清晰、反派动机是否合理
            4. fixedVersion 如有修正填写，否则与 content 相同
            5. 大纲确认后，后续所有章节必须严格遵循此设定，禁止修改
            """;

    /**
     * 正文生成专用追加指令
     */
    private static final String CHAPTER_RULES = """
            
            【当前任务：生成正文】
            1. reportType 必须为 "CHAPTER"
            2. content 中只填写本章正文（Markdown格式）
            3. selfCheck 中必须检查：
               - 与大纲设定是否矛盾（人设、关系、时间线）
               - 与上一章衔接是否自然（人物位置、情绪、未解决冲突）
               - 本章CP进度是否合理（禁止跳阶段）
               - 反派行为是否符合动机链
            4. 如果发现矛盾，在 inconsistencies 中列出，在 fixedVersion 中修正
            5. 禁止出现大纲中未设定的人物、未交代的地点变化、未铺垫的关系进展
            """;



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

        NovelReport novelReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT )
                .user(userMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userChatId))
                .call()
                .entity(NovelReport.class);

        log.info("novelReport: {}", novelReport);

        return novelReport;
    }


}
