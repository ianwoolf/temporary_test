package com.example.testflow.settings

import com.example.testflow.ai.AiProviderType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level settings state for TestFlow plugin
 */
@Service(Service.Level.APP)
@State(
    name = "TestFlowSettings",
    storages = [Storage("TestFlowSettings.xml")]
)
class SettingsState : PersistentStateComponent<SettingsState> {

    // AI Provider Settings
    var providerType: AiProviderType = AiProviderType.OPENAI
    var apiKey: String = ""
    var baseUrl: String = ""
    var model: String = ""

    // Timeout Settings
    var timeout: Int = 60000 // milliseconds

    // Advanced Settings
    var maxTokens: Int = 4000
    var temperature: Double = 0.7

    // UI Settings
    var showDetailedErrors: Boolean = true
    var autoSaveFlows: Boolean = true

    override fun getState(): SettingsState {
        return this
    }

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): SettingsState {
            return ApplicationManager.getApplication().getService(SettingsState::class.java)
        }
    }
}
