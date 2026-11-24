package com.example.agentscope.travel.controller;

import com.example.agentscope.travel.api.TravelChatRequest;
import com.example.agentscope.travel.api.TravelChatResponse;
import com.example.agentscope.travel.service.TravelGuideService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travel")
public class TravelChatController {

    private final TravelGuideService travelGuideService;

    public TravelChatController(TravelGuideService travelGuideService) {
        this.travelGuideService = travelGuideService;
    }

    @PostMapping("/chat")
    public TravelChatResponse chat(@Valid @RequestBody TravelChatRequest request) {
        return travelGuideService.chat(request);
    }
}
