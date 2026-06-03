package com.van.lqsaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class HttpAiInvoke {
    public static String chat() {
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        // API密钥
        String apiKey = ApiKeyConfig.getApiKey();

        // 构建请求体
        JSONObject body = new JSONObject();
        body.set("model", "qwen-plus");

        JSONArray messages = new JSONArray();

        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", "你是一个中短篇小说大师，擅长各种风格");
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", "你是谁？");
        messages.add(userMsg);

        body.set("messages", messages);

        // 发送 POST 请求
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(30000)  // 30秒超时
                .execute();

        // 获取响应
        String result = response.body();
        response.close();  // 记得关闭

        return result;
    }

    // 解析响应示例
    public static String parseContent(String responseBody) {
        JSONObject json = JSONUtil.parseObj(responseBody);
        return json.getByPath("choices[0].message.content", String.class);
    }
}
