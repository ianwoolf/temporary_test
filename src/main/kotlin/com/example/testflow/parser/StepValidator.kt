package com.example.testflow.parser

import com.example.testflow.model.*
import java.net.URL
import java.util.*

/**
 * Validator for test steps
 */
object StepValidator {

    /**
     * Validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    /**
     * Validate a test flow
     */
    fun validateFlow(flow: TestFlow): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (flow.name.isBlank()) {
            errors.add("Flow name cannot be blank")
        }

        if (flow.steps.isEmpty()) {
            errors.add("Flow must have at least one step")
        }

        flow.steps.forEachIndexed { index, step ->
            val stepResult = validateStep(step)
            errors.addAll(stepResult.errors.map { "Step ${index + 1}: $it" })
            warnings.addAll(stepResult.warnings.map { "Step ${index + 1}: $it" })
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validate a single step
     */
    fun validateStep(step: TestStepDto): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        when (step) {
            is TestStepDto.HttpStepDto -> validateHttpStep(step, errors, warnings)
            is TestStepDto.ScriptStepDto -> validateScriptStep(step, errors, warnings)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateHttpStep(
        step: TestStepDto.HttpStepDto,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (step.name.isBlank()) {
            errors.add("Step name cannot be blank")
        }

        if (step.url.isBlank()) {
            errors.add("URL cannot be blank")
        } else {
            try {
                URL(step.url)
            } catch (e: Exception) {
                errors.add("Invalid URL format: ${step.url}")
            }
        }

        try {
            HttpMethod.valueOf(step.method.uppercase())
        } catch (e: IllegalArgumentException) {
            errors.add("Invalid HTTP method: ${step.method}")
        }

        if (step.method in listOf("POST", "PUT", "PATCH") && step.body.isNullOrBlank()) {
            warnings.add("HTTP ${step.method} request usually has a body")
        }

        if (step.assertions.isEmpty()) {
            warnings.add("No assertions defined for this HTTP step")
        }
    }

    private fun validateScriptStep(
        step: TestStepDto.ScriptStepDto,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (step.name.isBlank()) {
            errors.add("Step name cannot be blank")
        }

        if (step.scriptContent.isBlank()) {
            errors.add("Script content cannot be blank")
        }

        try {
            ScriptType.valueOf(step.scriptType.uppercase())
        } catch (e: IllegalArgumentException) {
            errors.add("Invalid script type: ${step.scriptType}")
        }

        if (step.scriptContent.lines().size < 2) {
            warnings.add("Script appears to be very short")
        }
    }

    /**
     * Validate HTTP method
     */
    fun isValidHttpMethod(method: String): Boolean {
        return try {
            HttpMethod.valueOf(method.uppercase())
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Validate URL format
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}
