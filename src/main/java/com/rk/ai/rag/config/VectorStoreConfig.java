package com.rk.ai.rag.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Vector Store and Embedding Model beans.
 * Note: Both VectorStore and EmbeddingModel are auto-configured by Spring AI 
 * based on application.properties settings:
 * - spring.ai.openai.* for OpenAI-compatible embedding model
 * - spring.ai.vectorstore.pgvector.* for PgVector store configuration
 * 
 * This configuration class is kept for future custom bean definitions if needed.
 */
@Configuration
public class VectorStoreConfig {
    // Spring AI auto-configures EmbeddingModel and VectorStore from application.properties
}
