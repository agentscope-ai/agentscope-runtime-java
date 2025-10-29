package io.agentscope.chatbot;

import io.agentscope.chatbot.model.User;
import io.agentscope.chatbot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Web Server Application - Provides REST API for frontend
 * Equivalent to web_server.py
 */
@SpringBootApplication
public class WebServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(WebServerApplication.class);

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Create sample users if none exist
            if (userRepository.count() == 0) {
                logger.info("Initializing database with sample users...");

                User user1 = new User();
                user1.setUsername("user1");
                user1.setName("Bruce");
                user1.setPassword("password123");
                userRepository.save(user1);

                User user2 = new User();
                user2.setUsername("user2");
                user2.setName("John");
                user2.setPassword("password456");
                userRepository.save(user2);

                logger.info("✅ Sample users created: user1, user2");
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(WebServerApplication.class, args);
    }
}

