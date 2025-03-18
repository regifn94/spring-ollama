package com.spring.ollama.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ollama.model.User;
import com.spring.ollama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final WebClient.Builder webClient;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public String chatWithAi(String message) {
        // Ambil data user dari database
        List<User> users = userRepository.findAll();
        String userData = users.stream()
                .map(user -> "User: " + user.getName() + ", Email: " + user.getEmail())
                .collect(Collectors.joining(" | ")); // Gunakan pemisah yang aman

        // Gabungkan dengan prompt
        String prompt = "Data user: " + userData + "\n\nUser: " + message + "\nAI:";

        // Kirim ke Ollama
        String url = "http://localhost:11434/api/generate";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama3.2:3b");
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            return restTemplate.postForObject(url, jsonBody, String.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error generating request body";
        }
    }

    public Mono<String> chatWithAiStream(String message) {
        // Ambil data user dari database
        List<User> users = userRepository.findAll();
        String userData = users.stream()
                .map(user -> "User: " + user.getName() + ", Email: " + user.getEmail())
                .collect(Collectors.joining(" | ")); // Pemisah aman

        // Gabungkan dengan prompt
        String prompt = "Data user: " + userData + "\n\nUser: " + message + "\nAI:";
        System.out.println("prompt : " + prompt);
        // Buat request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama3.2:3b");
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            return webClient.baseUrl("http://localhost:11434").build()
                    .post()
                    .uri("/api/generate")
                    .header("Content-Type", "application/json")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Error generating request body", e));
        }
    }
}
