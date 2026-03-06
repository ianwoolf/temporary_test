package com.example.testflow.ui.components

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Panel for inputting documentation text
 */
class DocumentInputPanel : JPanel(BorderLayout()) {

    private val textArea = JBTextArea().apply {
        rows = 15
        wrapStyleWord = true
        lineWrap = true
        // Note: emptyText is set via the UI component property

        // Example placeholder
        text = """
            Example: User Authentication API Testing

            This test suite validates the user authentication endpoints of our API.

            Test Cases:
            1. Register a new user
               - POST /api/users/register
               - Body: {username, email, password}
               - Assert: 201 Created, user returned

            2. Login with valid credentials
               - POST /api/auth/login
               - Body: {email, password}
               - Assert: 200 OK, JWT token returned

            3. Access protected endpoint with token
               - GET /api/users/profile
               - Header: Authorization: Bearer {token}
               - Assert: 200 OK, user profile returned
        """.trimIndent()
    }

    init {
        val scrollPane = JBScrollPane(textArea)
        add(scrollPane, BorderLayout.CENTER)
    }

    fun getDocumentText(): String {
        return textArea.text.trim()
    }

    fun setDocumentText(text: String) {
        textArea.text = text
    }

    fun clearDocument() {
        textArea.text = ""
    }
}
