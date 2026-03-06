package com.example.testflow.ui.components

import com.example.testflow.ai.AiProviderType
import com.example.testflow.ai.AiServiceFactory
import com.example.testflow.settings.SettingsState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * Configurable panel for TestFlow settings
 */
class SettingsConfigurable : Configurable {

    private lateinit var providerType: AiProviderType
    private val apiKeyField = JBPasswordField()
    private val baseUrlField = JBTextField()
    private val modelField = JBTextField()
    private val timeoutField = JBTextField()
    private val maxTokensField = JBTextField()
    private val temperatureField = JBTextField()
    private var showDetailedErrorsValue = true
    private var autoSaveFlowsValue = true

    private val settings: SettingsState
        get() = SettingsState.getInstance()

    override fun getDisplayName(): String = "TestFlow Generator"

    override fun createComponent(): JComponent {
        // Load current settings
        providerType = settings.providerType
        apiKeyField.text = settings.apiKey
        baseUrlField.text = settings.baseUrl
        modelField.text = settings.model
        timeoutField.text = settings.timeout.toString()
        maxTokensField.text = settings.maxTokens.toString()
        temperatureField.text = settings.temperature.toString()
        showDetailedErrorsValue = settings.showDetailedErrors
        autoSaveFlowsValue = settings.autoSaveFlows

        return panel {
            group("AI Provider Configuration") {
                row("Provider:") {
                    comboBox(AiProviderType.entries.toList())
                        .bindItem(
                            getter = { providerType },
                            setter = { providerType = it ?: AiProviderType.OPENAI }
                        )
                        .onChanged { updateDefaults() }
                }.bottomGap(BottomGap.SMALL)

                row("API Key:") {
                    cell(apiKeyField)
                        .align(AlignX.FILL)
                        .comment("Required for OpenAI and Anthropic. Leave empty for Ollama.")
                }.bottomGap(BottomGap.SMALL)

                row("Base URL:") {
                    cell(baseUrlField)
                        .align(AlignX.FILL)
                        .comment("Optional. Override the default API endpoint.")
                }.bottomGap(BottomGap.SMALL)

                row("Model:") {
                    cell(modelField)
                        .align(AlignX.FILL)
                        .comment("e.g., gpt-4, claude-3-opus-20240229")
                }.bottomGap(BottomGap.SMALL)

                row {
                    button("Test Connection") {
                        testConnection()
                    }
                }
            }.resizableRow()

            separator()

            group("Advanced Settings") {
                row("Timeout (ms):") {
                    cell(timeoutField)
                        .comment("Request timeout in milliseconds (1000-300000)")
                }.bottomGap(BottomGap.SMALL)

                row("Max Tokens:") {
                    cell(maxTokensField)
                        .comment("Maximum tokens in AI response (100-32000)")
                }.bottomGap(BottomGap.SMALL)

                row("Temperature:") {
                    cell(temperatureField)
                        .comment("Controls randomness in AI responses (0.0-2.0)")
                }
            }.resizableRow()

            separator()

            group("UI Options") {
                row {
                    checkBox("Show detailed errors")
                        .bindSelected(
                            getter = { showDetailedErrorsValue },
                            setter = { showDetailedErrorsValue = it }
                        )
                }.bottomGap(BottomGap.SMALL)

                row {
                    checkBox("Auto-save generated flows")
                        .bindSelected(
                            getter = { autoSaveFlowsValue },
                            setter = { autoSaveFlowsValue = it }
                        )
                }
            }

            // Add model suggestions based on provider
            row("") {
                label("Available models for ${providerType.displayName}:")
                    .bold()
            }.bottomGap(BottomGap.SMALL)

            row("") {
                val models = AiServiceFactory.createService(
                    com.example.testflow.ai.AiProviderConfig(
                        provider = providerType
                    )
                ).getAvailableModels()

                comment(models.joinToString(", "))
            }
        }
    }

    private fun updateDefaults() {
        val defaults = com.example.testflow.ai.AiServiceFactory.getDefaultConfig(providerType)
        baseUrlField.text = defaults.baseUrl ?: ""
        modelField.text = defaults.model ?: ""
    }

    private fun testConnection() {
        val apiKey = String(apiKeyField.password)
        val baseUrl = baseUrlField.text.takeIf { it.isNotBlank() }
        val model = modelField.text.takeIf { it.isNotBlank() }

        val config = com.example.testflow.ai.AiProviderConfig(
            provider = providerType,
            apiKey = apiKey.takeIf { it.isNotBlank() },
            baseUrl = baseUrl,
            model = model
        )

        val service = com.example.testflow.ai.AiServiceFactory.createService(config)

        if (!service.isConfigured()) {
            Messages.showWarningDialog(
                "Please configure an API key for ${providerType.displayName}",
                "Configuration Required"
            )
            return
        }

        Messages.showInfoMessage(
            "Configuration looks good! Note: Full connection test requires making an API call, which will consume credits.",
            "Configuration Check"
        )
    }

    override fun isModified(): Boolean {
        return providerType != settings.providerType ||
                String(apiKeyField.password) != settings.apiKey ||
                baseUrlField.text != settings.baseUrl ||
                modelField.text != settings.model ||
                timeoutField.text != settings.timeout.toString() ||
                maxTokensField.text != settings.maxTokens.toString() ||
                temperatureField.text != settings.temperature.toString() ||
                showDetailedErrorsValue != settings.showDetailedErrors ||
                autoSaveFlowsValue != settings.autoSaveFlows
    }

    override fun apply() {
        settings.providerType = providerType
        settings.apiKey = String(apiKeyField.password)
        settings.baseUrl = baseUrlField.text
        settings.model = modelField.text
        settings.timeout = timeoutField.text.toIntOrNull() ?: 60000
        settings.maxTokens = maxTokensField.text.toIntOrNull() ?: 4000
        settings.temperature = temperatureField.text.toDoubleOrNull() ?: 0.7
        settings.showDetailedErrors = showDetailedErrorsValue
        settings.autoSaveFlows = autoSaveFlowsValue
    }

    override fun reset() {
        providerType = settings.providerType
        apiKeyField.text = settings.apiKey
        baseUrlField.text = settings.baseUrl
        modelField.text = settings.model
        timeoutField.text = settings.timeout.toString()
        maxTokensField.text = settings.maxTokens.toString()
        temperatureField.text = settings.temperature.toString()
        showDetailedErrorsValue = settings.showDetailedErrors
        autoSaveFlowsValue = settings.autoSaveFlows
    }
}
