package com.example.testflow.parser

import com.example.testflow.ai.AiServiceException
import com.example.testflow.model.TestFlow
import com.example.testflow.model.TestStepDto
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.util.*

/**
 * Parser for converting AI response into TestFlow objects
 */
object FlowParser {

    private val gson = Gson()

    /**
     * Parse JSON string into TestFlow
     */
    fun parseFlow(jsonContent: String): Result<TestFlow> {
        return try {
            // Try to extract JSON from markdown code blocks
            val cleanJson = extractJson(jsonContent)
            val jsonObject = JsonParser.parseString(cleanJson).asJsonObject

            val flow = TestFlow(
                id = UUID.randomUUID().toString(),
                name = jsonObject.get("name")?.asString ?: "Untitled Test Flow",
                description = jsonObject.get("description")?.asString ?: "",
                steps = parseSteps(jsonObject.getAsJsonArray("steps"))
            )

            Result.success(flow)
        } catch (e: JsonSyntaxException) {
            Result.failure(AiServiceException("Invalid JSON format: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(AiServiceException("Failed to parse test flow: ${e.message}", e))
        }
    }

    /**
     * Extract JSON from markdown code blocks
     */
    private fun extractJson(content: String): String {
        val trimmed = content.trim()

        // Try ```json block
        val jsonBlock = Regex("```json\\s*([\\s\\S]*?)\\s*```").find(trimmed)
        if (jsonBlock != null) {
            return jsonBlock.groupValues[1].trim()
        }

        // Try regular ``` block
        val codeBlock = Regex("```\\s*([\\s\\S]*?)\\s*```").find(trimmed)
        if (codeBlock != null) {
            return codeBlock.groupValues[1].trim()
        }

        return trimmed
    }

    /**
     * Parse steps array
     */
    private fun parseSteps(stepsArray: com.google.gson.JsonArray?): List<TestStepDto> {
        if (stepsArray == null) return emptyList()

        return stepsArray.mapIndexed { index, element ->
            parseStep(element.asJsonObject, index)
        }
    }

    /**
     * Parse a single step
     */
    private fun parseStep(jsonObject: com.google.gson.JsonObject, index: Int): TestStepDto {
        val typeStr = jsonObject.get("type")?.asString ?: "HTTP"
        val type = when (typeStr.uppercase()) {
            "SCRIPT" -> com.example.testflow.model.StepType.SCRIPT
            else -> com.example.testflow.model.StepType.HTTP
        }

        val id = jsonObject.get("id")?.asString ?: UUID.randomUUID().toString()
        val name = jsonObject.get("name")?.asString ?: "Step ${index + 1}"
        val description = jsonObject.get("description")?.asString ?: ""

        return when (type) {
            com.example.testflow.model.StepType.HTTP -> {
                com.example.testflow.model.TestStepDto.HttpStepDto(
                    id = id,
                    name = name,
                    description = description,
                    method = jsonObject.get("method")?.asString ?: "GET",
                    url = jsonObject.get("url")?.asString ?: "",
                    headers = parseStringMap(jsonObject.get("headers")),
                    body = jsonObject.get("body")?.asString,
                    authentication = jsonObject.get("authentication")?.let { parseAuth(it.asJsonObject) },
                    assertions = parseAssertions(jsonObject.get("assertions"))
                )
            }
            com.example.testflow.model.StepType.SCRIPT -> {
                com.example.testflow.model.TestStepDto.ScriptStepDto(
                    id = id,
                    name = name,
                    description = description,
                    scriptType = jsonObject.get("scriptType")?.asString ?: "JAVASCRIPT",
                    scriptContent = jsonObject.get("scriptContent")?.asString ?: "",
                    environment = parseStringMap(jsonObject.get("environment"))
                )
            }
        }
    }

    private fun parseStringMap(element: com.google.gson.JsonElement?): Map<String, String> {
        if (element == null || !element.isJsonObject) return emptyMap()
        return element.asJsonObject.entrySet().associate { it.key to it.value.asString }
    }

    private fun parseAuth(jsonObject: com.google.gson.JsonObject): com.example.testflow.model.AuthConfigDto {
        return com.example.testflow.model.AuthConfigDto(
            type = com.example.testflow.model.AuthType.valueOf(
                jsonObject.get("type")?.asString?.uppercase() ?: "NONE"
            ),
            parameters = parseStringMap(jsonObject.get("parameters"))
        )
    }

    private fun parseAssertions(element: com.google.gson.JsonElement?): List<com.example.testflow.model.AssertionDto> {
        if (element == null || !element.isJsonArray) return emptyList()

        return element.asJsonArray.map { assertJson ->
            parseAssertion(assertJson.asJsonObject)
        }
    }

    private fun parseAssertion(jsonObject: com.google.gson.JsonObject): com.example.testflow.model.AssertionDto {
        return com.example.testflow.model.AssertionDto(
            type = com.example.testflow.model.AssertionType.valueOf(
                jsonObject.get("type")?.asString?.uppercase()?.replace(" ", "_") ?: "STATUS_CODE"
            ),
            target = jsonObject.get("target")?.asString ?: "",
            operator = com.example.testflow.model.AssertionOperator.valueOf(
                jsonObject.get("operator")?.asString?.uppercase() ?: "EQUALS"
            ),
            expectedValue = jsonObject.get("expectedValue")?.asString ?: ""
        )
    }
}
