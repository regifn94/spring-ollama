package com.spring.ollama.controller;

import com.spring.ollama.dto.ChatResponse;
import com.spring.ollama.dto.QueryRequest;
import com.spring.ollama.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("/rag")
public class RAGController {
    private final RAGService ragService;

    @PostMapping("/ask")
    public Object askRag(@RequestBody QueryRequest request){
        return ragService.rag(request);
    }

    @PostMapping("/askflux")
    public Mono<ChatResponse> getRAGResponse(@RequestBody QueryRequest request) {
        return ragService.ragFlux(request);
    }
}
