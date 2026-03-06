package com.example.testflow.ai

/**
 * Builds prompts for AI to generate structured test flows
 */
object PromptBuilder {

    /**
     * Build the system prompt for test flow generation
     */
    fun buildSystemPrompt(): String {
        return """
            You are a test automation expert. Your task is to analyze documentation and generate structured test flows.

            Rules:
            1. Each test flow should have a clear name and description
            2. Break down the test into logical steps
            3. Each step should be either an HTTP request or a script execution
            4. For HTTP requests, include method, URL, headers, body, and assertions
            5. For scripts, specify the script type and provide the script content
            6. Use proper assertions to validate responses

            Output Format:
            You MUST respond with a valid JSON object in the following format:

            {
              "name": "Test Flow Name",
              "description": "Detailed description of what this test flow validates",
              "steps": [
                {
                  "type": "HTTP" or "SCRIPT",
                  "name": "Step name",
                  "description": "What this step does",
                  ...step-specific fields
                }
              ]
            }

            HTTP Step Format:
            {
              "type": "HTTP",
              "id": "unique-id",
              "name": "Step Name",
              "description": "Description",
              "method": "GET|POST|PUT|DELETE|PATCH",
              "url": "https://api.example.com/endpoint",
              "headers": {"Content-Type": "application/json"},
              "body": "{\"key\": \"value\"}",
              "authentication": {
                "type": "NONE|BASIC|BEARER|API_KEY",
                "parameters": {"username": "user", "password": "pass"}
              },
              "assertions": [
                {
                  "type": "STATUS_CODE|HEADER|BODY_FIELD|BODY_CONTAINS",
                  "target": "status_code or field path",
                  "operator": "EQUALS|CONTAINS|MATCHES_REGEX",
                  "expectedValue": "200"
                }
              ]
            }

            Script Step Format:
            {
              "type": "SCRIPT",
              "id": "unique-id",
              "name": "Step Name",
              "description": "Description",
              "scriptType": "JAVASCRIPT|SHELL|PYTHON|GROOVY|KOTLIN",
              "scriptContent": "# Script code here",
              "environment": {"VAR1": "value1"}
            }

            IMPORTANT:
            - Respond ONLY with the JSON object, no additional text
            - Use realistic values based on the documentation
            - Make reasonable assumptions when documentation is incomplete
            - Include proper error handling assertions
            - Use UUID format for step IDs
        """.trimIndent()
    }

    /**
     * Build the user prompt from documentation
     */
    fun buildUserPrompt(documentation: String): String {
        return """
            Analyze the following documentation and generate a comprehensive test flow:

            ${documentation.trim()}

            Generate a JSON test flow following the specified format.
        """.trimIndent()
    }

    /**
     * Build a prompt for improving an existing test flow
     */
    fun buildImprovementPrompt(currentFlow: String, feedback: String): String {
        return """
            Here is the current test flow:

            $currentFlow

            User feedback: $feedback

            Please improve the test flow based on the feedback and return the updated JSON.
            Remember to respond ONLY with the JSON object.
        """.trimIndent()
    }
}
