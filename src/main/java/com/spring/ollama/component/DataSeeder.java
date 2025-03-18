package com.spring.ollama.component;

import com.spring.ollama.model.User;
import com.spring.ollama.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            List<User> users = List.of(
                    new User(null,"joen","john_doe", "john@example.com", "USER", "Jakarta"),
                    new User(null,"admusr","admin_user", "admin@example.com", "ADMIN", "Manado"),
                    new User(null,"jne","jane_doe", "jane@example.com", "USER", "Surabaya")
            );
            userRepository.saveAll(users);
        }
    }
}

