package com.example.ai.alibaba.hello.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话示例接口。
 *
 * <p>通过构造器注入 {@link ChatClient.Builder}（由 DashScope starter 自动装配），
 * 构建 {@link ChatClient} 后发起同步对话。</p>
 */
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 最简对话：GET /ai/chat?message=你好
     *
     * @param message 用户输入
     * @return 模型返回的文本内容
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "你好，请简单介绍一下你自己") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
