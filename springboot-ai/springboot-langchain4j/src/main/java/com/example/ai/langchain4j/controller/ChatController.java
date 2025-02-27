package com.example.ai.langchain4j.controller;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class ChatController {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        return chatLanguageModel.chat(prompt);
    }
}