package com.example.testflow.ai

import com.example.testflow.model.TestFlow

/**
 * Interface for AI service providers
 */
interface AiService {
    /**
     * Provider name
     */
    val providerName: String

    /**
     * Generate test flow from documentation
     * @param documentation The input documentation text
     * @return Generated test flow
     */
    suspend fun generateTestFlow(documentation: String): Result<TestFlow>

    /**
     * Check if the service is properly configured
     */
    fun isConfigured(): Boolean

    /**
     * Get available models
     */
    fun getAvailableModels(): List<String>
}

/**
 * AI provider configuration
 */
data class AiProviderConfig(
    val provider: AiProviderType,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val model: String? = null,
    val timeout: Int = 60000, // milliseconds
    val maxTokens: Int = 4000
)

/**
 * Supported AI provider types
 */
enum class AiProviderType(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    OLLAMA("Ollama (Local)")
}

/**
 * Response from AI API
 */
data class AiResponse(
    val content: String,
    val model: String,
    val tokensUsed: Int? = null
)

/**
 * Exception for AI service errors
 */
class AiServiceException(message: String, cause: Throwable? = null) : Exception(message, cause)
