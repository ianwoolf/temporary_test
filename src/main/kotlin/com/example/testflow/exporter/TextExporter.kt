package com.example.testflow.exporter

import com.example.testflow.model.FlowExportFormat
import com.example.testflow.model.FlowExportResult
import com.example.testflow.model.TestFlow

/**
 * Exporter for plain text format
 */
class TextExporter {

    /**
     * Export test flow to plain text format
     */
    fun export(flow: TestFlow): FlowExportResult {
        val sb = StringBuilder()
        sb.append("=".repeat(80)).append("\n")
        sb.append("TEST FLOW: ${flow.name.uppercase()}").append("\n")
        sb.append("=".repeat(80)).append("\n")
        sb.append("\n")
        sb.append("Description: ${flow.description}").append("\n")
        sb.append("Total Steps: ${flow.steps.size}").append("\n")
        sb.append("Created: ${flow.createdAt}").append("\n")
        sb.append("\n")
        sb.append("-".repeat(80)).append("\n")
        sb.append("\n")

        flow.steps.forEachIndexed { index, step ->
            appendStep(sb, step, index + 1)
            sb.append("\n")
        }

        sb.append("=".repeat(80)).append("\n")
        sb.append("END OF TEST FLOW").append("\n")
        sb.append("=".repeat(80)).append("\n")

        return FlowExportResult(
            format = FlowExportFormat.TEXT,
            content = sb.toString(),
            fileName = "${sanitizeFileName(flow.name)}.txt"
        )
    }

    /**
     * Export test flow to Markdown format
     */
    fun exportMarkdown(flow: TestFlow): FlowExportResult {
        val sb = StringBuilder()
        sb.append("# ${flow.name}").append("\n")
        sb.append("\n")
        sb.append("**Description:** ${flow.description}").append("\n")
        sb.append("\n")
        sb.append("**Total Steps:** ${flow.steps.size}").append("\n")
        sb.append("\n")
        sb.append("---").append("\n")
        sb.append("\n")

        flow.steps.forEachIndexed { index, step ->
            appendStepMarkdown(sb, step, index + 1)
            sb.append("\n")
        }

        return FlowExportResult(
            format = FlowExportFormat.MARKDOWN,
            content = sb.toString(),
            fileName = "${sanitizeFileName(flow.name)}.md"
        )
    }

    private fun appendStep(
        sb: StringBuilder,
        step: com.example.testflow.model.TestStepDto,
        index: Int
    ) {
        sb.append("STEP $index: ${step.name}").append("\n")
        sb.append("-".repeat(80)).append("\n")
        sb.append("Description: ${step.description}").append("\n")
        sb.append("\n")

        when (step) {
            is com.example.testflow.model.TestStepDto.HttpStepDto -> appendHttpStep(sb, step)
            is com.example.testflow.model.TestStepDto.ScriptStepDto -> appendScriptStep(sb, step)
        }
    }

    private fun appendHttpStep(sb: StringBuilder, step: com.example.testflow.model.TestStepDto.HttpStepDto) {
        sb.append("Type: HTTP Request").append("\n")
        sb.append("Method: ${step.method}").append("\n")
        sb.append("URL: ${step.url}").append("\n")
        sb.append("\n")

        if (step.headers.isNotEmpty()) {
            sb.append("Headers:").append("\n")
            step.headers.forEach { (key, value) ->
                sb.append("  $key: $value").append("\n")
            }
            sb.append("\n")
        }

        if (step.body != null) {
            sb.append("Request Body:").append("\n")
            sb.append(step.body.prependIndent("  ")).append("\n")
            sb.append("\n")
        }

        if (step.authentication != null && step.authentication.type != com.example.testflow.model.AuthType.NONE) {
            sb.append("Authentication: ${step.authentication.type}").append("\n")
            if (step.authentication.parameters.isNotEmpty()) {
                sb.append("Parameters:").append("\n")
                step.authentication.parameters.forEach { (key, _) ->
                    sb.append("  $key: ***").append("\n")
                }
            }
            sb.append("\n")
        }

        if (step.assertions.isNotEmpty()) {
            sb.append("Assertions (${step.assertions.size}):").append("\n")
            step.assertions.forEach { assertion ->
                sb.append("  - ${assertion.type} ${assertion.operator} '${assertion.expectedValue}'").append("\n")
                sb.append("    Target: ${assertion.target}").append("\n")
            }
        }
    }

    private fun appendScriptStep(sb: StringBuilder, step: com.example.testflow.model.TestStepDto.ScriptStepDto) {
        sb.append("Type: Script Execution").append("\n")
        sb.append("Script Type: ${step.scriptType}").append("\n")
        sb.append("\n")

        if (step.environment.isNotEmpty()) {
            sb.append("Environment Variables:").append("\n")
            step.environment.forEach { (key, value) ->
                sb.append("  $key=$value").append("\n")
            }
            sb.append("\n")
        }

        sb.append("Script:").append("\n")
        sb.append("```${step.scriptType.lowercase()}").append("\n")
        sb.append(step.scriptContent).append("\n")
        sb.append("```").append("\n")
    }

    private fun appendStepMarkdown(
        sb: StringBuilder,
        step: com.example.testflow.model.TestStepDto,
        index: Int
    ) {
        sb.append("## Step $index: ${step.name}").append("\n")
        sb.append("\n")
        sb.append("**Description:** ${step.description}").append("\n")
        sb.append("\n")

        when (step) {
            is com.example.testflow.model.TestStepDto.HttpStepDto -> appendHttpStepMarkdown(sb, step)
            is com.example.testflow.model.TestStepDto.ScriptStepDto -> appendScriptStepMarkdown(sb, step)
        }
    }

    private fun appendHttpStepMarkdown(sb: StringBuilder, step: com.example.testflow.model.TestStepDto.HttpStepDto) {
        sb.append("**Type:** HTTP Request  ").append("\n")
        sb.append("**Method:** `${step.method}`  ").append("\n")
        sb.append("**URL:** `${step.url}`  ").append("\n")
        sb.append("\n")

        if (step.headers.isNotEmpty()) {
            sb.append("**Headers:**").append("\n")
            sb.append("| Header | Value |").append("\n")
            sb.append("|--------|-------|").append("\n")
            step.headers.forEach { (key, value) ->
                sb.append("| `$key` | `$value` |").append("\n")
            }
            sb.append("\n")
        }

        if (step.body != null) {
            sb.append("**Request Body:**").append("\n")
            sb.append("```json").append("\n")
            sb.append(step.body).append("\n")
            sb.append("```").append("\n")
            sb.append("\n")
        }

        if (step.authentication != null && step.authentication.type != com.example.testflow.model.AuthType.NONE) {
            sb.append("**Authentication:** ${step.authentication.type}  ").append("\n")
            sb.append("\n")
        }

        if (step.assertions.isNotEmpty()) {
            sb.append("**Assertions:**").append("\n")
            step.assertions.forEach { assertion ->
                sb.append("- ${assertion.type} ${assertion.operator} `${assertion.expectedValue}` (target: `${assertion.target}`)").append("\n")
            }
        }
    }

    private fun appendScriptStepMarkdown(sb: StringBuilder, step: com.example.testflow.model.TestStepDto.ScriptStepDto) {
        sb.append("**Type:** Script Execution  ").append("\n")
        sb.append("**Script Type:** ${step.scriptType}  ").append("\n")
        sb.append("\n")

        if (step.environment.isNotEmpty()) {
            sb.append("**Environment Variables:**").append("\n")
            step.environment.forEach { (key, value) ->
                sb.append("- `$key` = `$value`").append("\n")
            }
            sb.append("\n")
        }

        sb.append("**Script:**").append("\n")
        sb.append("```${step.scriptType.lowercase()}").append("\n")
        sb.append(step.scriptContent).append("\n")
        sb.append("```").append("\n")
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}
