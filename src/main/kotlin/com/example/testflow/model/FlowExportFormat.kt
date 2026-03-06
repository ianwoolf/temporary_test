package com.example.testflow.model

/**
 * Export format enum for test flows
 */
enum class FlowExportFormat {
    JSON,
    TEXT,
    MARKDOWN
}

/**
 * Result of a test flow export operation
 */
data class FlowExportResult(
    val format: FlowExportFormat,
    val content: String,
    val fileName: String
)
