package com.example.testflow.ui.components

import com.example.testflow.model.TestFlow
import com.example.testflow.model.TestStepDto
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.*

/**
 * Panel for displaying generated test flow
 */
class FlowDisplayPanel : JPanel(BorderLayout()) {

    private val flowInfoLabel = JBLabel("No test flow generated yet").apply {
        horizontalAlignment = SwingConstants.CENTER
        font = font.deriveFont(Font.ITALIC, 12f)
    }

    private val stepsList = JBList<String>()
    private val listModel = DefaultListModel<String>()

    private val detailPanel = JPanel()
    private val detailTextArea = JTextArea().apply {
        isEditable = false
        font = Font("Monospaced", Font.PLAIN, 12)
    }

    private var currentFlow: TestFlow? = null

    init {
        // Top section: Flow info
        val topPanel = JPanel(BorderLayout())
        topPanel.add(flowInfoLabel, BorderLayout.CENTER)

        // Center section: Steps list
        stepsList.model = listModel
        stepsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        stepsList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                showStepDetails()
            }
        }

        val listScrollPane = JBScrollPane(stepsList)

        // Bottom section: Step details
        detailPanel.layout = BorderLayout()
        detailPanel.border = BorderFactory.createTitledBorder("Step Details")
        detailPanel.add(JBScrollPane(detailTextArea), BorderLayout.CENTER)

        // Split pane for list and details
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        splitPane.topComponent = listScrollPane
        splitPane.bottomComponent = detailPanel
        splitPane.resizeWeight = 0.4

        add(topPanel, BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)
    }

    fun displayFlow(flow: TestFlow) {
        currentFlow = flow

        flowInfoLabel.text = "<html><b>${flow.name}</b> - ${flow.description} (${flow.steps.size} steps)</html>"
        flowInfoLabel.horizontalAlignment = SwingConstants.LEFT

        listModel.clear()

        flow.steps.forEachIndexed { index, step ->
            val icon = when (step) {
                is com.example.testflow.model.TestStepDto.HttpStepDto -> "[HTTP]"
                is com.example.testflow.model.TestStepDto.ScriptStepDto -> "[SCRIPT]"
            }
            listModel.addElement("$icon ${index + 1}. ${step.name}")
        }

        if (flow.steps.isNotEmpty()) {
            stepsList.selectedIndex = 0
        }

        revalidate()
        repaint()
    }

    fun clearFlow() {
        currentFlow = null
        flowInfoLabel.text = "No test flow generated yet"
        flowInfoLabel.horizontalAlignment = SwingConstants.CENTER
        listModel.clear()
        detailTextArea.text = ""
        revalidate()
        repaint()
    }

    private fun showStepDetails() {
        val flow = currentFlow ?: return
        val selectedIndex = stepsList.selectedIndex

        if (selectedIndex < 0 || selectedIndex >= flow.steps.size) {
            detailTextArea.text = ""
            return
        }

        val step = flow.steps[selectedIndex]
        detailTextArea.text = formatStepDetails(step, selectedIndex + 1)
    }

    private fun formatStepDetails(step: com.example.testflow.model.TestStepDto, index: Int): String {
        val sb = StringBuilder()
        sb.appendLine("Step $index: ${step.name}")
        sb.appendLine("=".repeat(60))
        sb.appendLine("Description: ${step.description}")
        sb.appendLine()

        when (step) {
            is com.example.testflow.model.TestStepDto.HttpStepDto -> {
                sb.appendLine("Type: HTTP Request")
                sb.appendLine("Method: ${step.method}")
                sb.appendLine("URL: ${step.url}")
                sb.appendLine()

                if (step.headers.isNotEmpty()) {
                    sb.appendLine("Headers:")
                    step.headers.forEach { (k, v) ->
                        sb.appendLine("  $k: $v")
                    }
                    sb.appendLine()
                }

                if (step.body != null) {
                    sb.appendLine("Request Body:")
                    sb.appendLine(step.body)
                    sb.appendLine()
                }

                if (step.authentication != null && step.authentication.type != com.example.testflow.model.AuthType.NONE) {
                    sb.appendLine("Authentication: ${step.authentication.type}")
                    if (step.authentication.parameters.isNotEmpty()) {
                        step.authentication.parameters.forEach { (k, v) ->
                            sb.appendLine("  $k: ***")
                        }
                    }
                    sb.appendLine()
                }

                if (step.assertions.isNotEmpty()) {
                    sb.appendLine("Assertions (${step.assertions.size}):")
                    step.assertions.forEach { assertion ->
                        sb.appendLine("  - ${assertion.type} ${assertion.operator} ${assertion.expectedValue}")
                        sb.appendLine("    Target: ${assertion.target}")
                    }
                }
            }
            is com.example.testflow.model.TestStepDto.ScriptStepDto -> {
                sb.appendLine("Type: Script Execution")
                sb.appendLine("Script Type: ${step.scriptType}")
                sb.appendLine()

                if (step.environment.isNotEmpty()) {
                    sb.appendLine("Environment Variables:")
                    step.environment.forEach { (k, v) ->
                        sb.appendLine("  $k: $v")
                    }
                    sb.appendLine()
                }

                sb.appendLine("Script Content:")
                sb.appendLine("```${step.scriptType.lowercase()}")
                sb.appendLine(step.scriptContent)
                sb.appendLine("```")
            }
        }

        return sb.toString()
    }

    fun getCurrentFlow(): TestFlow? = currentFlow
}
