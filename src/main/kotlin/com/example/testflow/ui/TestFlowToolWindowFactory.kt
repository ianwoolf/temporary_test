package com.example.testflow.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating the TestFlow tool window
 */
class TestFlowToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val testFlowPanel = TestFlowPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(testFlowPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
