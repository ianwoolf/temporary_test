package com.example.testflow.executor

import com.example.testflow.model.TestFlow
import com.example.testflow.model.TestStep
import com.example.testflow.model.TestStepDto
import com.example.testflow.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Result of a test flow execution
 */
data class FlowExecutionResult(
    val flowId: String,
    val flowName: String,
    val stepResults: List<StepExecutionResult>,
    val overallStatus: ExecutionStatus,
    val startTime: Long,
    val endTime: Long
) {
    val duration: Long get() = endTime - startTime
    val passedCount: Int get() = stepResults.count { it.status == ExecutionStatus.PASSED }
    val failedCount: Int get() = stepResults.count { it.status == ExecutionStatus.FAILED }
    val skippedCount: Int get() = stepResults.count { it.status == ExecutionStatus.SKIPPED }
}

/**
 * Result of a single step execution
 */
data class StepExecutionResult(
    val stepId: String,
    val stepName: String,
    val status: ExecutionStatus,
    val output: String?,
    val error: String?,
    val assertions: List<AssertionResult>,
    val startTime: Long,
    val endTime: Long
) {
    val duration: Long get() = endTime - startTime
}

/**
 * Result of an assertion
 */
data class AssertionResult(
    val description: String,
    val passed: Boolean,
    val expected: String?,
    val actual: String?,
    val error: String? = null
)

/**
 * Execution status
 */
enum class ExecutionStatus {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
    SKIPPED,
    ERROR
}

/**
 * Executor for running test flows
 */
class FlowExecutor(
    private val httpExecutor: HttpRequestExecutor = HttpRequestExecutor(),
    private val scriptExecutor: ScriptExecutor = ScriptExecutor()
) {

    private val listeners = mutableListOf<ExecutionListener>()

    /**
     * Add a listener for execution events
     */
    fun addListener(listener: ExecutionListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: ExecutionListener) {
        listeners.remove(listener)
    }

    /**
     * Execute a test flow
     */
    suspend fun execute(flow: TestFlow): FlowExecutionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        listeners.forEach { it.onFlowStarted(flow) }

        val stepResults = coroutineScope {
            flow.steps.mapIndexed { index, step ->
                async {
                    executeStep(step.toDomain(), index + 1)
                }
            }.map { it.await() }
        }

        val endTime = System.currentTimeMillis()
        val overallStatus = determineOverallStatus(stepResults)

        val result = FlowExecutionResult(
            flowId = flow.id,
            flowName = flow.name,
            stepResults = stepResults,
            overallStatus = overallStatus,
            startTime = startTime,
            endTime = endTime
        )

        listeners.forEach { it.onFlowCompleted(result) }

        result
    }

    /**
     * Execute a single step
     */
    private suspend fun executeStep(step: TestStep, stepNumber: Int): StepExecutionResult {
        val startTime = System.currentTimeMillis()

        listeners.forEach { it.onStepStarted(step, stepNumber) }

        val result = when (step) {
            is com.example.testflow.model.HttpStep -> executeHttpStep(step, stepNumber, startTime)
            is com.example.testflow.model.ScriptStep -> executeScriptStep(step, stepNumber, startTime)
        }

        listeners.forEach { it.onStepCompleted(result) }

        return result
    }

    private suspend fun executeHttpStep(
        step: com.example.testflow.model.HttpStep,
        stepNumber: Int,
        startTime: Long
    ): StepExecutionResult {
        return try {
            val response = httpExecutor.execute(step)

            val assertionResults = response.assertions
            val allPassed = assertionResults.all { it.passed }

            val status = if (allPassed) ExecutionStatus.PASSED else ExecutionStatus.FAILED

            StepExecutionResult(
                stepId = step.id,
                stepName = step.name,
                status = status,
                output = response.body,
                error = if (!allPassed) "Some assertions failed" else null,
                assertions = assertionResults,
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            StepExecutionResult(
                stepId = step.id,
                stepName = step.name,
                status = ExecutionStatus.ERROR,
                output = null,
                error = e.message ?: "Unknown error",
                assertions = emptyList(),
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        }
    }

    private suspend fun executeScriptStep(
        step: com.example.testflow.model.ScriptStep,
        stepNumber: Int,
        startTime: Long
    ): StepExecutionResult {
        return try {
            val result = scriptExecutor.execute(step)

            val status = if (result.exitCode == 0) ExecutionStatus.PASSED else ExecutionStatus.FAILED

            StepExecutionResult(
                stepId = step.id,
                stepName = step.name,
                status = status,
                output = result.output,
                error = if (result.exitCode != 0) result.error else null,
                assertions = emptyList(),
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            StepExecutionResult(
                stepId = step.id,
                stepName = step.name,
                status = ExecutionStatus.ERROR,
                output = null,
                error = e.message ?: "Unknown error",
                assertions = emptyList(),
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        }
    }

    private fun determineOverallStatus(stepResults: List<StepExecutionResult>): ExecutionStatus {
        return when {
            stepResults.any { it.status == ExecutionStatus.ERROR } -> ExecutionStatus.ERROR
            stepResults.any { it.status == ExecutionStatus.FAILED } -> ExecutionStatus.FAILED
            stepResults.all { it.status == ExecutionStatus.PASSED } -> ExecutionStatus.PASSED
            else -> ExecutionStatus.SKIPPED
        }
    }
}

/**
 * Listener for execution events
 */
interface ExecutionListener {
    fun onFlowStarted(flow: TestFlow) {}
    fun onFlowCompleted(result: FlowExecutionResult) {}
    fun onStepStarted(step: TestStep, stepNumber: Int) {}
    fun onStepCompleted(result: StepExecutionResult) {}
}
