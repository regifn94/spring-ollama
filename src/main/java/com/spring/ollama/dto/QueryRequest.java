package com.spring.ollama.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class QueryRequest {
    private String query;

    private String conversationId;
}
