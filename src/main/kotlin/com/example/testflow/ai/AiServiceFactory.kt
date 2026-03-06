package com.example.testflow.ai

import com.example.testflow.ai.providers.AnthropicProvider
import com.example.testflow.ai.providers.OllamaProvider
import com.example.testflow.ai.providers.OpenAiProvider

/**
 * Factory for creating AI service instances
 */
object AiServiceFactory {

    /**
     * Create an AI service instance based on the configuration
     */
    fun createService(config: AiProviderConfig): AiService {
        return when (config.provider) {
            AiProviderType.OPENAI -> OpenAiProvider(config)
            AiProviderType.ANTHROPIC -> AnthropicProvider(config)
            AiProviderType.OLLAMA -> OllamaProvider(config)
        }
    }

    /**
     * Get a list of all available provider types
     */
    fun getAvailableProviders(): List<AiProviderType> {
        return AiProviderType.entries.toList()
    }

    /**
     * Get the default configuration for a provider type
     */
    fun getDefaultConfig(provider: AiProviderType): AiProviderConfig {
        return when (provider) {
            AiProviderType.OPENAI -> AiProviderConfig(
                provider = provider,
                baseUrl = "https://api.openai.com/v1",
                model = "gpt-4"
            )
            AiProviderType.ANTHROPIC -> AiProviderConfig(
                provider = provider,
                baseUrl = "https://api.anthropic.com/v1",
                model = "claude-3-opus-20240229"
            )
            AiProviderType.OLLAMA -> AiProviderConfig(
                provider = provider,
                baseUrl = "http://localhost:11434",
                model = "llama2"
            )
        }
    }
}
