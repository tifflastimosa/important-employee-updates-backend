package com.example.important_messages_prototype;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables auditing so fields annotated with @CreatedDate are filled in automatically on save.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
