package com.example.testflow.model

/**
 * Base interface for all test steps
 */
sealed interface TestStep {
    val id: String
    val name: String
    val description: String
}

/**
 * Extension function to convert DTO to domain model
 */
fun TestStepDto.toDomain(): TestStep {
    return when (this) {
        is TestStepDto.HttpStepDto -> HttpStep.fromDto(this)
        is TestStepDto.ScriptStepDto -> ScriptStep.fromDto(this)
    }
}

/**
 * Extension function to convert domain model to DTO
 */
fun TestStep.toDto(): TestStepDto {
    return when (this) {
        is HttpStep -> this.toDto()
        is ScriptStep -> this.toDto()
    }
}
