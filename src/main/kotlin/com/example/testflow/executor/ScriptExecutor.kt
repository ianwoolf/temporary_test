package com.example.testflow.executor

import com.example.testflow.model.ScriptStep
import com.example.testflow.model.ScriptType
import java.io.File

/**
 * Result from script execution
 */
data class ScriptResult(
    val exitCode: Int,
    val output: String,
    val error: String?
)

/**
 * Executor for script steps
 */
class ScriptExecutor {

    /**
     * Execute a script step
     */
    suspend fun execute(step: ScriptStep): ScriptResult {
        return when (step.scriptType) {
            ScriptType.SHELL -> executeShellScript(step)
            ScriptType.JAVASCRIPT -> executeJavaScript(step)
            ScriptType.PYTHON -> executePythonScript(step)
            ScriptType.GROOVY -> executeGroovyScript(step)
            ScriptType.KOTLIN -> executeKotlinScript(step)
        }
    }

    private suspend fun executeShellScript(step: ScriptStep): ScriptResult {
        return try {
            val processBuilder = ProcessBuilder()

            // Use bash for shell scripts
            processBuilder.command("bash", "-c", step.scriptContent)

            // Set environment variables
            val environment = processBuilder.environment()
            step.environment.forEach { (key, value) ->
                environment[key] = value
            }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            ScriptResult(
                exitCode = exitCode,
                output = output.trim(),
                error = if (error.isNotBlank()) error.trim() else null
            )
        } catch (e: Exception) {
            ScriptResult(
                exitCode = -1,
                output = "",
                error = e.message ?: "Failed to execute shell script"
            )
        }
    }

    private suspend fun executeJavaScript(step: ScriptStep): ScriptResult {
        return try {
            // Check if Node.js is available
            val nodeCheck = ProcessBuilder("which", "node").start().waitFor() == 0

            if (!nodeCheck) {
                return ScriptResult(
                    exitCode = -1,
                    output = "",
                    error = "Node.js is not installed or not in PATH"
                )
            }

            // Write script to temporary file
            val tempFile = File.createTempFile("testflow_", ".js")
            tempFile.writeText(step.scriptContent)
            tempFile.deleteOnExit()

            val processBuilder = ProcessBuilder("node", tempFile.absolutePath)

            // Set environment variables
            val environment = processBuilder.environment()
            step.environment.forEach { (key, value) ->
                environment[key] = value
            }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            ScriptResult(
                exitCode = exitCode,
                output = output.trim(),
                error = if (error.isNotBlank()) error.trim() else null
            )
        } catch (e: Exception) {
            ScriptResult(
                exitCode = -1,
                output = "",
                error = e.message ?: "Failed to execute JavaScript"
            )
        }
    }

    private suspend fun executePythonScript(step: ScriptStep): ScriptResult {
        return try {
            // Check if Python is available
            val pythonCheck = ProcessBuilder("which", "python3").start().waitFor() == 0 ||
                    ProcessBuilder("which", "python").start().waitFor() == 0

            if (!pythonCheck) {
                return ScriptResult(
                    exitCode = -1,
                    output = "",
                    error = "Python is not installed or not in PATH"
                )
            }

            val pythonCommand = if (ProcessBuilder("which", "python3").start().waitFor() == 0) {
                "python3"
            } else {
                "python"
            }

            // Write script to temporary file
            val tempFile = File.createTempFile("testflow_", ".py")
            tempFile.writeText(step.scriptContent)
            tempFile.deleteOnExit()

            val processBuilder = ProcessBuilder(pythonCommand, tempFile.absolutePath)

            // Set environment variables
            val environment = processBuilder.environment()
            step.environment.forEach { (key, value) ->
                environment[key] = value
            }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            ScriptResult(
                exitCode = exitCode,
                output = output.trim(),
                error = if (error.isNotBlank()) error.trim() else null
            )
        } catch (e: Exception) {
            ScriptResult(
                exitCode = -1,
                output = "",
                error = e.message ?: "Failed to execute Python script"
            )
        }
    }

    private suspend fun executeGroovyScript(step: ScriptStep): ScriptResult {
        return try {
            // Check if Groovy is available
            val groovyCheck = ProcessBuilder("which", "groovy").start().waitFor() == 0

            if (!groovyCheck) {
                return ScriptResult(
                    exitCode = -1,
                    output = "",
                    error = "Groovy is not installed or not in PATH"
                )
            }

            // Write script to temporary file
            val tempFile = File.createTempFile("testflow_", ".groovy")
            tempFile.writeText(step.scriptContent)
            tempFile.deleteOnExit()

            val processBuilder = ProcessBuilder("groovy", tempFile.absolutePath)

            // Set environment variables
            val environment = processBuilder.environment()
            step.environment.forEach { (key, value) ->
                environment[key] = value
            }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            ScriptResult(
                exitCode = exitCode,
                output = output.trim(),
                error = if (error.isNotBlank()) error.trim() else null
            )
        } catch (e: Exception) {
            ScriptResult(
                exitCode = -1,
                output = "",
                error = e.message ?: "Failed to execute Groovy script"
            )
        }
    }

    private suspend fun executeKotlinScript(step: ScriptStep): ScriptResult {
        // Kotlin script execution requires kotlinc and more complex setup
        // For now, return a not implemented message
        return ScriptResult(
            exitCode = -1,
            output = "",
            error = "Kotlin script execution is not yet supported. Please use JavaScript, Python, Groovy, or Shell scripts."
        )
    }
}
