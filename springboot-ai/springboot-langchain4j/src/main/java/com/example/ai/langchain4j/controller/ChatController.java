package com.example.ai.langchain4j.controller;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping(value = "/langchain4j")
public class ChatController {

    @Resource
    private ChatModel chatModel;

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        String chatted = chatModel.chat(prompt);
        return chatted;
    }
}