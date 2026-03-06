package com.example.testflow.ai.providers

import com.example.testflow.ai.*
import com.example.testflow.model.TestFlow
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

/**
 * Ollama (local LLM) provider
 */
class OllamaProvider(private val config: AiProviderConfig) : AiService {

    companion object {
        private const val DEFAULT_BASE_URL = "http://localhost:11434"
        private const val DEFAULT_MODEL = "llama2"
        private const val CHAT_ENDPOINT = "/api/chat"
    }

    private val client = OkHttpClient.Builder()
        .build()

    private val gson = Gson()

    override val providerName: String = "Ollama"

    override suspend fun generateTestFlow(documentation: String): Result<TestFlow> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = config.baseUrl ?: DEFAULT_BASE_URL
            val model = config.model ?: DEFAULT_MODEL

            val systemPrompt = PromptBuilder.buildSystemPrompt()
            val userPrompt = PromptBuilder.buildUserPrompt(documentation)

            val requestBody = mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to userPrompt)
                ),
                "stream" to false,
                "options" to mapOf(
                    "temperature" to 0.7,
                    "num_predict" to config.maxTokens
                )
            )

            val mediaType = "application/json".toMediaType()
            val body = gson.toJson(requestBody).toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$baseUrl$CHAT_ENDPOINT")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(
                    AiServiceException("Ollama API error: ${response.code}, $errorBody")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(AiServiceException("Empty response from Ollama"))

            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val content = jsonResponse
                .getAsJsonObject("message")
                .get("content").asString

            // Ollama might include markdown code blocks, extract JSON
            val jsonContent = extractJson(content)

            val flowJson = JsonParser.parseString(jsonContent).asJsonObject
            val flow = parseTestFlow(flowJson)

            Result.success(flow)
        } catch (e: IOException) {
            Result.failure(AiServiceException("Network error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(AiServiceException("Failed to generate test flow: ${e.message}", e))
        }
    }

    private fun extractJson(content: String): String {
        // Try to extract JSON from markdown code blocks
        val jsonBlock = Regex("```json\\s*([\\s\\S]*?)\\s*```").find(content)
        if (jsonBlock != null) {
            return jsonBlock.groupValues[1].trim()
        }

        // Try without json specifier
        val codeBlock = Regex("```\\s*([\\s\\S]*?)\\s*```").find(content)
        if (codeBlock != null) {
            return codeBlock.groupValues[1].trim()
        }

        return content.trim()
    }

    private fun parseTestFlow(json: com.google.gson.JsonObject): TestFlow {
        return TestFlow(
            id = UUID.randomUUID().toString(),
            name = json.get("name")?.asString ?: "Untitled Test Flow",
            description = json.get("description")?.asString ?: "",
            steps = json.getAsJsonArray("steps").map { stepJson ->
                parseStep(stepJson.asJsonObject)
            }
        )
    }

    private fun parseStep(json: com.google.gson.JsonObject): com.example.testflow.model.TestStepDto {
        val typeStr = json.get("type")?.asString ?: "HTTP"
        val type = when (typeStr.uppercase()) {
            "SCRIPT" -> com.example.testflow.model.StepType.SCRIPT
            else -> com.example.testflow.model.StepType.HTTP
        }

        val id = json.get("id")?.asString ?: UUID.randomUUID().toString()
        val name = json.get("name")?.asString ?: "Unnamed Step"
        val description = json.get("description")?.asString ?: ""

        return when (type) {
            com.example.testflow.model.StepType.HTTP -> {
                com.example.testflow.model.TestStepDto.HttpStepDto(
                    id = id,
                    name = name,
                    description = description,
                    method = json.get("method")?.asString ?: "GET",
                    url = json.get("url")?.asString ?: "",
                    headers = parseHeaders(json.get("headers")),
                    body = json.get("body")?.asString,
                    authentication = json.get("authentication")?.let { parseAuth(it.asJsonObject) },
                    assertions = json.getAsJsonArray("assertions")?.map { assertJson ->
                        parseAssertion(assertJson.asJsonObject)
                    } ?: emptyList()
                )
            }
            com.example.testflow.model.StepType.SCRIPT -> {
                com.example.testflow.model.TestStepDto.ScriptStepDto(
                    id = id,
                    name = name,
                    description = description,
                    scriptType = json.get("scriptType")?.asString ?: "JAVASCRIPT",
                    scriptContent = json.get("scriptContent")?.asString ?: "",
                    environment = parseMap(json.get("environment"))
                )
            }
        }
    }

    private fun parseHeaders(element: com.google.gson.JsonElement?): Map<String, String> {
        if (element == null || !element.isJsonObject) return emptyMap()
        return element.asJsonObject.entrySet().associate { it.key to it.value.asString }
    }

    private fun parseMap(element: com.google.gson.JsonElement?): Map<String, String> {
        if (element == null || !element.isJsonObject) return emptyMap()
        return element.asJsonObject.entrySet().associate { it.key to it.value.asString }
    }

    private fun parseAuth(json: com.google.gson.JsonObject): com.example.testflow.model.AuthConfigDto {
        return com.example.testflow.model.AuthConfigDto(
            type = com.example.testflow.model.AuthType.valueOf(
                json.get("type")?.asString?.uppercase() ?: "NONE"
            ),
            parameters = parseMap(json.get("parameters"))
        )
    }

    private fun parseAssertion(json: com.google.gson.JsonObject): com.example.testflow.model.AssertionDto {
        return com.example.testflow.model.AssertionDto(
            type = com.example.testflow.model.AssertionType.valueOf(
                json.get("type")?.asString?.uppercase()?.replace(" ", "_") ?: "STATUS_CODE"
            ),
            target = json.get("target")?.asString ?: "",
            operator = com.example.testflow.model.AssertionOperator.valueOf(
                json.get("operator")?.asString?.uppercase() ?: "EQUALS"
            ),
            expectedValue = json.get("expectedValue")?.asString ?: ""
        )
    }

    override fun isConfigured(): Boolean {
        // Ollama doesn't require API key, just needs to be accessible
        return true
    }

    override fun getAvailableModels(): List<String> {
        // Common Ollama models - users can install different ones
        return listOf(
            "llama2",
            "llama2:13b",
            "mistral",
            "neural-chat",
            "starling-lm"
        )
    }
}
