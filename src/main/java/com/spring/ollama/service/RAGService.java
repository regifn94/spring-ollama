package com.spring.ollama.service;

import com.spring.ollama.dto.ChatResponse;
import com.spring.ollama.dto.QueryRequest;
import com.spring.ollama.model.User;
import com.spring.ollama.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private final ChatClient chatClient;

    private final EmbeddingModel embeddingModel;

    private SimpleVectorStore vectorStore;

    private String template;

    private final UserRepository userRepository;

    public RAGService(EmbeddingModel embeddingModel, ChatClient.Builder chatClientBuilder, UserRepository userRepository) {
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        System.out.println("Loading user data from database...");

        System.out.println("Fetching users from database...");
        Flux<User> userFlux = Flux.fromIterable(userRepository.findAll());

        userFlux.collectList().subscribe(users -> {
            List<Document> documentList = users.stream()
                    .map(user -> new Document(String.format(
                            """
                            **Username:** %s  
                            **Name:** %s  
                            **Email:** %s  
                            **Role:** %s  
                            **Address:** %s  
                            """,
                            user.getUsername(),
                            cleanData(user.getName()),  // Membersihkan data kosong/null
                            cleanData(user.getEmail()),
                            cleanData(user.getRole()),
                            cleanData(user.getAddress())
                    )))
                    .collect(Collectors.toList());

            vectorStore.accept(documentList);
            System.out.println("User data loading done.");
        });

        System.out.println("User data loading done.");

        template = """
                    You are an AI chatbot named RCC AI Chatbot.
                
                    Answer the questions only using the information in the provided user database.
                    If you do not know the answer, please respond with "I don't know."
                
                    USER DATABASE
                    ---
                    {documents}
                    """;

    }

    private String cleanData(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value.trim();
    }


    public ChatResponse rag(QueryRequest request){
        System.out.println("Received request ..........");
        request.setConversationId(UUID.randomUUID().toString());

        String relevantDocs = vectorStore.similaritySearch(request.getQuery())
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining());

        Message systemMessage = new SystemPromptTemplate(template).createMessage(Map.of("documents", relevantDocs));

        Message userMessage = new UserMessage(request.getQuery());
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatClient.CallResponseSpec res = chatClient.prompt(prompt).call();

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(res.content());
        chatResponse.setResponseId(request.getConversationId().toString());

        System.out.println("Response send ..........");
        return chatResponse;
    }

    public Mono<ChatResponse> ragFlux(QueryRequest request) {
        System.out.println("Received request ..........");
        request.setConversationId(UUID.randomUUID().toString());

        return Mono.fromSupplier(() -> vectorStore.similaritySearch(request.getQuery()))
                .map(docs -> docs.stream().map(Document::getText).collect(Collectors.joining()))
                .flatMap(relevantDocs -> {
                    Message systemMessage = new SystemPromptTemplate(template).createMessage(Map.of("documents", relevantDocs));
                    Message userMessage = new UserMessage(request.getQuery());
                    Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

                    return Mono.fromSupplier(() -> chatClient.prompt(prompt).call())
                            .map(res -> {
                                ChatResponse chatResponse = new ChatResponse();
                                chatResponse.setResponse(res.content());
                                chatResponse.setResponseId(request.getConversationId());
                                System.out.println("Response sent ..........");
                                return chatResponse;
                            });
                });
    }

}