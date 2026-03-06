package com.example.testflow.exporter

import com.example.testflow.model.FlowExportFormat
import com.example.testflow.model.FlowExportResult
import com.example.testflow.model.TestFlow
import com.google.gson.GsonBuilder

/**
 * Exporter for JSON format
 */
class JsonExporter {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    /**
     * Export test flow to JSON format
     */
    fun export(flow: TestFlow): FlowExportResult {
        val json = gson.toJson(flow)

        return FlowExportResult(
            format = FlowExportFormat.JSON,
            content = json,
            fileName = "${sanitizeFileName(flow.name)}.json"
        )
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}
