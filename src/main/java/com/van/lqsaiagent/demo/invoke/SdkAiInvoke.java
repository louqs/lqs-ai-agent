// package com.van.lqsaiagent.demo.invoke;
//
// import java.util.Arrays;
// import java.lang.System;
// import com.alibaba.dashscope.aigc.generation.Generation;
// import com.alibaba.dashscope.aigc.generation.GenerationParam;
// import com.alibaba.dashscope.aigc.generation.GenerationResult;
// import com.alibaba.dashscope.common.Message;
// import com.alibaba.dashscope.common.Role;
// import com.alibaba.dashscope.exception.ApiException;
// import com.alibaba.dashscope.exception.InputRequiredException;
// import com.alibaba.dashscope.exception.NoApiKeyException;
// import com.alibaba.dashscope.protocol.Protocol;
//
// /**
//  * 阿里云灵积AI SDK调用
//  */
// public class SdkAiInvoke {
//
//     public static GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
//         // 以下为华北2（北京）地域的URL，各地域的URL不同。
//         Generation gen = new Generation(Protocol.HTTP.getValue(), "https://dashscope.aliyuncs.com/api/v1");
//         Message systemMsg = Message.builder()
//                 .role(Role.SYSTEM.getValue())
//                 .content("你是一个中短篇小说大师，擅长各种风格")
//                 .build();
//         Message userMsg = Message.builder()
//                 .role(Role.USER.getValue())
//                 .content("你是谁？")
//                 .build();
//         GenerationParam param = GenerationParam.builder()
//                 // 若没有配置环境变量，请用阿里云百炼API Key将下行替换为：.apiKey("sk-xxx")
//                 .apiKey(ApiKeyConfig.getApiKey())
//                 // 模型列表：https://help.aliyun.com/model-studio/getting-started/models
//                 .model("qwen-plus")
//                 .messages(Arrays.asList(systemMsg, userMsg))
//                 .resultFormat(GenerationParam.ResultFormat.MESSAGE)
//                 .build();
//         return gen.call(param);
//     }
// //    public static void main(String[] args) {
// //        try {
// //            GenerationResult result = callWithMessage();
// //            System.out.println(result.getOutput().getChoices().get(0).getMessage().getContent());
// //        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
// //            System.err.println("错误信息："+e.getMessage());
// //            System.out.println("请参考文档：https://help.aliyun.com/model-studio/developer-reference/error-code");
// //        }
// //        System.exit(0);
// //    }
// }