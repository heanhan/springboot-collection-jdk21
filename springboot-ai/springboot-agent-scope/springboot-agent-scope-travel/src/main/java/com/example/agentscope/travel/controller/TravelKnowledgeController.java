package com.example.agentscope.travel.controller;

import com.example.agentscope.travel.api.KnowledgeSearchRequest;
import com.example.agentscope.travel.api.KnowledgeSearchResponse;
import com.example.agentscope.travel.rag.TravelKnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旅游知识检索调试接口。
 */
@RestController
@RequestMapping("/api/travel/knowledge")
public class TravelKnowledgeController {

    private final TravelKnowledgeService travelKnowledgeService;

    public TravelKnowledgeController(TravelKnowledgeService travelKnowledgeService) {
        this.travelKnowledgeService = travelKnowledgeService;
    }

    /**
     * 检索与问题相关的旅游知识片段。
     */
    @PostMapping("/search")
    public KnowledgeSearchResponse search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return travelKnowledgeService.search(request);
    }
}
