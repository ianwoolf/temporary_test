package com.example.testflow.ui

import com.example.testflow.ai.AiProviderType
import com.example.testflow.ai.AiServiceFactory
import com.example.testflow.ai.AiServiceFactory.getDefaultConfig
import com.example.testflow.model.TestFlow
import com.example.testflow.settings.SettingsState
import com.example.testflow.ui.components.DocumentInputPanel
import com.example.testflow.ui.components.FlowDisplayPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.event.ActionEvent
import javax.swing.*

/**
 * Main panel for the TestFlow tool window
 */
class TestFlowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val documentInputPanel = DocumentInputPanel()
    private val flowDisplayPanel = FlowDisplayPanel()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var currentFlow: TestFlow? = null

    init {
        // Create main content with split pane
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)

        // Top panel: Document input
        val topPanel = JPanel(BorderLayout())
        topPanel.border = BorderFactory.createTitledBorder("1. Input Documentation")
        topPanel.add(documentInputPanel, BorderLayout.CENTER)
        topPanel.add(createButtonPanel(), BorderLayout.SOUTH)

        // Bottom panel: Flow display
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = BorderFactory.createTitledBorder("2. Generated Test Flow")
        bottomPanel.add(JBScrollPane(flowDisplayPanel), BorderLayout.CENTER)
        bottomPanel.add(createExportPanel(), BorderLayout.SOUTH)

        splitPane.topComponent = topPanel
        splitPane.bottomComponent = bottomPanel
        splitPane.resizeWeight = 0.5

        add(splitPane, BorderLayout.CENTER)
    }

    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))

        val generateButton = JButton("Generate Test Flow")
        generateButton.addActionListener { e -> generateTestFlow(e) }

        val clearButton = JButton("Clear")
        clearButton.addActionListener {
            documentInputPanel.clearDocument()
            flowDisplayPanel.clearFlow()
            currentFlow = null
        }

        panel.add(generateButton)
        panel.add(clearButton)

        return panel
    }

    private fun createExportPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))

        val exportJsonButton = JButton("Export JSON")
        exportJsonButton.addActionListener { exportJson() }

        val exportTextButton = JButton("Export Text")
        exportTextButton.addActionListener { exportText() }

        val saveButton = JButton("Save to Project")
        saveButton.addActionListener { saveToProject() }

        val executeButton = JButton("Execute Test")
        executeButton.addActionListener { executeTest() }

        panel.add(exportJsonButton)
        panel.add(exportTextButton)
        panel.add(saveButton)
        panel.add(executeButton)

        return panel
    }

    private fun generateTestFlow(event: ActionEvent) {
        val document = documentInputPanel.getDocumentText()

        if (document.isBlank()) {
            Messages.showWarningDialog(
                project,
                "Please enter some documentation text first.",
                "No Documentation"
            )
            return
        }

        // Disable button during generation
        val source = event.source as? JButton ?: return
        source.isEnabled = false
        source.text = "Generating..."

        scope.launch(Dispatchers.Main) {
            try {
                val settings = ApplicationManager.getApplication().getService(SettingsState::class.java)
                val providerType = settings?.providerType ?: AiProviderType.OPENAI

                val config = getDefaultConfig(providerType).copy(
                    apiKey = settings?.apiKey,
                    baseUrl = settings?.baseUrl?.takeIf { it.isNotBlank() },
                    model = settings?.model?.takeIf { it.isNotBlank() }
                )

                val service = AiServiceFactory.createService(config)

                if (!service.isConfigured()) {
                    withContext(Dispatchers.Main) {
                        Messages.showErrorDialog(
                            project,
                            "AI service is not configured. Please go to Settings > Tools > TestFlow Generator to configure your API keys.",
                            "Not Configured"
                        )
                    }
                    return@launch
                }

                val result = service.generateTestFlow(document)

                result.fold(
                    onSuccess = { flow ->
                        currentFlow = flow
                        withContext(Dispatchers.Main) {
                            flowDisplayPanel.displayFlow(flow)
                            Messages.showInfoMessage(
                                project,
                                "Test flow generated successfully with ${flow.steps.size} steps!",
                                "Success"
                            )
                        }
                    },
                    onFailure = { error ->
                        withContext(Dispatchers.Main) {
                            Messages.showErrorDialog(
                                project,
                                "Failed to generate test flow: ${error.message}",
                                "Generation Failed"
                            )
                        }
                    }
                )
            } finally {
                withContext(Dispatchers.Main) {
                    source.isEnabled = true
                    source.text = "Generate Test Flow"
                }
            }
        }
    }

    private fun exportJson() {
        val flow = currentFlow
        if (flow == null) {
            Messages.showWarningDialog(project, "No test flow to export. Generate one first.", "No Flow")
            return
        }

        val exporter = com.example.testflow.exporter.JsonExporter()
        val result = exporter.export(flow)

        showExportDialog(result)
    }

    private fun exportText() {
        val flow = currentFlow
        if (flow == null) {
            Messages.showWarningDialog(project, "No test flow to export. Generate one first.", "No Flow")
            return
        }

        val exporter = com.example.testflow.exporter.TextExporter()
        val result = exporter.export(flow)

        showExportDialog(result)
    }

    private fun showExportDialog(result: com.example.testflow.model.FlowExportResult) {
        val dialog = JDialog()
        dialog.title = "Exported Test Flow"
        dialog.layout = BorderLayout()

        val textArea = JTextArea(result.content)
        textArea.isEditable = false
        val scrollPane = JBScrollPane(textArea)

        val buttonPanel = JPanel(FlowLayout())

        val copyButton = JButton("Copy to Clipboard")
        copyButton.addActionListener {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val stringSelection = java.awt.datatransfer.StringSelection(result.content)
            clipboard.setContents(stringSelection, null)
            Messages.showInfoMessage(dialog, "Copied to clipboard!", "Success")
        }

        val saveButton = JButton("Save to File")
        saveButton.addActionListener {
            val chooser = JFileChooser()
            chooser.selectedFile = java.io.File(result.fileName)
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile.writeText(result.content)
                Messages.showInfoMessage(dialog, "Saved successfully!", "Success")
            }
        }

        val closeButton = JButton("Close")
        closeButton.addActionListener { dialog.isVisible = false }

        buttonPanel.add(copyButton)
        buttonPanel.add(saveButton)
        buttonPanel.add(closeButton)

        dialog.add(scrollPane, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)

        dialog.setSize(800, 600)
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }

    private fun saveToProject() {
        Messages.showInfoMessage(project, "Save to project feature coming soon!", "Coming Soon")
    }

    private fun executeTest() {
        Messages.showInfoMessage(project, "Test execution feature coming soon!", "Coming Soon")
    }

    fun cleanup() {
        scope.cancel()
    }
}
