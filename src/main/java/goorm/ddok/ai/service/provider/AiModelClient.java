package goorm.ddok.ai.service.provider;

public interface AiModelClient {
    String generate(String prompt, int maxTokens);
}