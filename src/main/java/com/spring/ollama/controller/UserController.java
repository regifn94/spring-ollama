package com.spring.ollama.controller;

import com.spring.ollama.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @GetMapping("/generate")
    public String chatWithUser(@RequestParam String message){
        System.out.println("Request Body: " + message); // Cek request body
        return userService.chatWithAi(message);
    }

    @GetMapping("/generateStream")
    public Mono<String> chatWithAi(@RequestParam String request){
        return userService.chatWithAiStream(request);
    }
}
