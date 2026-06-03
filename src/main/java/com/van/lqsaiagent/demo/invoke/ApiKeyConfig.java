package com.van.lqsaiagent.demo.invoke;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.ai.dashscope")
@Data
public class ApiKeyConfig {

    private String apiKey;

    // 静态持有
    private static ApiKeyConfig instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static String getApiKey() {
        return instance == null ? null : instance.apiKey;
    }

}
