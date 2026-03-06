package com.example.testflow.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Represents a complete test flow with multiple steps
 */
@Serializable
data class TestFlow(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<TestStepDto>,
    @Contextual
    val createdAt: Instant = Instant.now(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * DTO for serialization/deserialization of test steps
 */
@Serializable
sealed class TestStepDto {
    abstract val id: String
    abstract val name: String
    abstract val description: String
    abstract val type: StepType

    /**
     * HTTP request step DTO
     */
    @Serializable
    data class HttpStepDto(
        override val id: String,
        override val name: String,
        override val description: String,
        val method: String,
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val authentication: AuthConfigDto? = null,
        val assertions: List<AssertionDto> = emptyList(),
        override val type: StepType = StepType.HTTP
    ) : TestStepDto()

    /**
     * Script execution step DTO
     */
    @Serializable
    data class ScriptStepDto(
        override val id: String,
        override val name: String,
        override val description: String,
        val scriptType: String,
        val scriptContent: String,
        val environment: Map<String, String> = emptyMap(),
        override val type: StepType = StepType.SCRIPT
    ) : TestStepDto()
}

/**
 * Step type enum
 */
@Serializable
enum class StepType {
    HTTP,
    SCRIPT
}

/**
 * Authentication configuration DTO
 */
@Serializable
data class AuthConfigDto(
    val type: AuthType,
    val parameters: Map<String, String> = emptyMap()
)

/**
 * Authentication type enum
 */
@Serializable
enum class AuthType {
    NONE,
    BASIC,
    BEARER,
    API_KEY,
    OAUTH2
}

/**
 * Assertion DTO for validating responses
 */
@Serializable
data class AssertionDto(
    val type: AssertionType,
    val target: String, // e.g., "status_code", "body.field"
    val operator: AssertionOperator,
    val expectedValue: String
)

/**
 * Assertion type enum
 */
@Serializable
enum class AssertionType {
    STATUS_CODE,
    HEADER,
    BODY_FIELD,
    RESPONSE_TIME,
    BODY_CONTAINS
}

/**
 * Assertion operator enum
 */
@Serializable
enum class AssertionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    GREATER_THAN,
    LESS_THAN,
    MATCHES_REGEX
}
