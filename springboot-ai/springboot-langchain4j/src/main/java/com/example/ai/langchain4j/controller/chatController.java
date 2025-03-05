package com.example.ai.langchain4j.controller;

import com.example.ai.langchain4j.service.IOllamaChatModelService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 20:21
 * @Description:
 */

@RestController
public class chatController {



    @Resource
    private IOllamaChatModelService ollamaChatModelService;
    /**
     * 交流
     * @param message
     * @return
     */


        @GetMapping("/chat")
        public String chat(@RequestParam("message") String message){

            return ollamaChatModelService.chat(message);

        }
}
