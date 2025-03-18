package com.spring.ollama.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ChatResponse {
    private String responseId;
    private String response;
}
