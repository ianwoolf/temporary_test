package com.example.testflow.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Action to open the TestFlow tool window
 */
class GenerateFlowAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow = toolWindowManager.getToolWindow("TestFlow") ?: return

        toolWindow.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
