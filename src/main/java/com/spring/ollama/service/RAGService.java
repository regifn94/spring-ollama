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
        List<User> users = userRepository.findAll();
        System.out.println("Total users found: " + users.size());

        // Konversi data user menjadi dokumen untuk vektor store
        List<Document> documentList = users.stream()
                .map(user -> new Document(
                        "Username: " + user.getUsername() + "\n" +
                                "Name: " + user.getName() + "\n" +
                                "Email: " + user.getEmail() + "\n" +
                                "Role: " + user.getRole() + "\n" +
                                "Address: " + (user.getAddress() != null ? user.getAddress() : "N/A")
                ))
                .collect(Collectors.toList());

        // Simpan ke dalam vector store
        vectorStore.accept(documentList);
        System.out.println("User data loading done.");

        template = """
                Answer the questions only using the information in the provided user database.
                If you do not know the answer, please respond with "I don't know."

                USER DATABASE
                ---
                {documents}
                """;
    }


    public ChatResponse rag(QueryRequest request){
        System.out.println("Received request ..........");
        request.setConversationId(UUID.randomUUID().toString());

        // Retrieval dari vectorStore
        String relevantDocs = vectorStore.similaritySearch(request.getQuery())
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining());

        // Augmented Prompt
        Message systemMessage = new SystemPromptTemplate(template).createMessage(Map.of("documents", relevantDocs));

        // Generasi Respons AI
        Message userMessage = new UserMessage(request.getQuery());
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatClient.CallResponseSpec res = chatClient.prompt(prompt).call();

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(res.content());
        chatResponse.setResponseId(request.getConversationId().toString());

        System.out.println("Response send ..........");
        return chatResponse;
    }

}

