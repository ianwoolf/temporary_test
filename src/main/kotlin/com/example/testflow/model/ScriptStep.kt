package com.example.testflow.model

/**
 * Script execution step - domain model for runtime use
 */
data class ScriptStep(
    override val id: String,
    override val name: String,
    override val description: String,
    val scriptType: ScriptType,
    val scriptContent: String,
    val environment: Map<String, String> = emptyMap()
) : TestStep {

    companion object {
        fun fromDto(dto: TestStepDto.ScriptStepDto): ScriptStep {
            return ScriptStep(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                scriptType = ScriptType.valueOf(dto.scriptType),
                scriptContent = dto.scriptContent,
                environment = dto.environment
            )
        }
    }

    fun toDto(): TestStepDto.ScriptStepDto {
        return TestStepDto.ScriptStepDto(
            id = id,
            name = name,
            description = description,
            scriptType = scriptType.name,
            scriptContent = scriptContent,
            environment = environment
        )
    }
}

/**
 * Script type enum
 */
enum class ScriptType {
    JAVASCRIPT,
    SHELL,
    PYTHON,
    GROOVY,
    KOTLIN
}
