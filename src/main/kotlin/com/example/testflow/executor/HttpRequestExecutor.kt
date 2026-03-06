package com.example.testflow.executor

import com.example.testflow.model.Assertion
import com.example.testflow.model.AssertionOperator
import com.example.testflow.model.AssertionType
import com.example.testflow.model.AuthType
import com.example.testflow.model.HttpStep
import com.google.gson.JsonParser
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Response from HTTP execution
 */
data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String?,
    val responseTime: Long,
    val assertions: List<AssertionResult>
)

/**
 * Executor for HTTP requests
 */
class HttpRequestExecutor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Execute an HTTP step
     */
    suspend fun execute(step: HttpStep): HttpResponse {
        val startTime = System.currentTimeMillis()

        val requestBuilder = Request.Builder()
            .url(step.url)

        // Add headers
        step.headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        // Add authentication
        addAuthentication(requestBuilder, step)

        // Add body for POST/PUT/PATCH
        if (step.body != null && step.method in listOf(
                com.example.testflow.model.HttpMethod.POST,
                com.example.testflow.model.HttpMethod.PUT,
                com.example.testflow.model.HttpMethod.PATCH
            )
        ) {
            val mediaType = step.headers["Content-Type"]?.toMediaType()
                ?: "application/json".toMediaType()
            requestBuilder.post(step.body.toRequestBody(mediaType))
        }

        val request = when (step.method) {
            com.example.testflow.model.HttpMethod.GET -> requestBuilder.get().build()
            com.example.testflow.model.HttpMethod.POST -> requestBuilder.post(
                (step.body ?: "").toRequestBody(
                    step.headers["Content-Type"]?.toMediaType() ?: "application/json".toMediaType()
                )
            ).build()
            com.example.testflow.model.HttpMethod.PUT -> requestBuilder.put(
                (step.body ?: "").toRequestBody(
                    step.headers["Content-Type"]?.toMediaType() ?: "application/json".toMediaType()
                )
            ).build()
            com.example.testflow.model.HttpMethod.DELETE -> requestBuilder.delete(
                step.body?.toRequestBody(
                    step.headers["Content-Type"]?.toMediaType() ?: "application/json".toMediaType()
                )
            ).build()
            com.example.testflow.model.HttpMethod.PATCH -> requestBuilder.patch(
                (step.body ?: "").toRequestBody(
                    step.headers["Content-Type"]?.toMediaType() ?: "application/json".toMediaType()
                )
            ).build()
            com.example.testflow.model.HttpMethod.HEAD -> requestBuilder.head().build()
            com.example.testflow.model.HttpMethod.OPTIONS -> requestBuilder.method("OPTIONS", null).build()
        }

        val response = client.newCall(request).execute()

        val responseTime = System.currentTimeMillis() - startTime
        val responseBody = response.body?.string()
        val headers = response.headers.toMap()

        // Run assertions
        val assertionResults = runAssertions(step.assertions, response, responseBody, responseTime)

        return HttpResponse(
            statusCode = response.code,
            headers = headers,
            body = responseBody,
            responseTime = responseTime,
            assertions = assertionResults
        )
    }

    private fun addAuthentication(builder: Request.Builder, step: HttpStep) {
        when (step.authentication?.type) {
            AuthType.BASIC -> {
                val username = step.authentication.parameters["username"] ?: ""
                val password = step.authentication.parameters["password"] ?: ""
                val credential = Credentials.basic(username, password)
                builder.addHeader("Authorization", credential)
            }
            AuthType.BEARER -> {
                val token = step.authentication.parameters["token"] ?: ""
                builder.addHeader("Authorization", "Bearer $token")
            }
            AuthType.API_KEY -> {
                val headerName = step.authentication.parameters["header"] ?: "X-API-Key"
                val keyValue = step.authentication.parameters["value"] ?: ""
                builder.addHeader(headerName, keyValue)
            }
            AuthType.OAUTH2 -> {
                val token = step.authentication.parameters["access_token"] ?: ""
                builder.addHeader("Authorization", "Bearer $token")
            }
            AuthType.NONE, null -> {
                // No authentication
            }
        }
    }

    private fun runAssertions(
        assertions: List<Assertion>,
        response: okhttp3.Response,
        body: String?,
        responseTime: Long
    ): List<AssertionResult> {
        return assertions.map { assertion ->
            try {
                val passed = evaluateAssertion(assertion, response, body, responseTime)
                val actual = getActualValue(assertion, response, body, responseTime)

                AssertionResult(
                    description = buildAssertionDescription(assertion),
                    passed = passed,
                    expected = assertion.expectedValue,
                    actual = actual?.toString()
                )
            } catch (e: Exception) {
                AssertionResult(
                    description = buildAssertionDescription(assertion),
                    passed = false,
                    expected = assertion.expectedValue,
                    actual = null,
                    error = e.message
                )
            }
        }
    }

    private fun evaluateAssertion(
        assertion: Assertion,
        response: okhttp3.Response,
        body: String?,
        responseTime: Long
    ): Boolean {
        val actual = getActualValue(assertion, response, body, responseTime)
        val expected = assertion.expectedValue

        return when (assertion.operator) {
            AssertionOperator.EQUALS -> actual.toString() == expected
            AssertionOperator.NOT_EQUALS -> actual.toString() != expected
            AssertionOperator.CONTAINS -> actual.toString().contains(expected)
            AssertionOperator.NOT_CONTAINS -> !actual.toString().contains(expected)
            AssertionOperator.GREATER_THAN -> {
                val actualNum = actual.toString().toDoubleOrNull()
                val expectedNum = expected.toDoubleOrNull()
                actualNum != null && expectedNum != null && actualNum > expectedNum
            }
            AssertionOperator.LESS_THAN -> {
                val actualNum = actual.toString().toDoubleOrNull()
                val expectedNum = expected.toDoubleOrNull()
                actualNum != null && expectedNum != null && actualNum < expectedNum
            }
            AssertionOperator.MATCHES_REGEX -> {
                val regex = Regex(expected)
                regex.containsMatchIn(actual.toString())
            }
        }
    }

    private fun getActualValue(
        assertion: Assertion,
        response: okhttp3.Response,
        body: String?,
        responseTime: Long
    ): Any? {
        return when (assertion.type) {
            AssertionType.STATUS_CODE -> response.code
            AssertionType.HEADER -> {
                val headerName = assertion.target
                response.headers[headerName]
            }
            AssertionType.BODY_FIELD -> {
                try {
                    val json = JsonParser.parseString(body ?: "{}").asJsonObject
                    getNestedValue(json, assertion.target)
                } catch (e: Exception) {
                    null
                }
            }
            AssertionType.BODY_CONTAINS -> {
                if (body != null && body.contains(assertion.target)) {
                    assertion.target
                } else {
                    null
                }
            }
            AssertionType.RESPONSE_TIME -> responseTime
        }
    }

    private fun getNestedValue(json: com.google.gson.JsonObject, path: String): Any? {
        val parts = path.split(".")
        var current: Any? = json

        for (part in parts) {
            when (current) {
                is com.google.gson.JsonObject -> current = current.get(part)
                is Map<*, *> -> current = (current as Map<*, *>)[part]
            }
        }

        return current
    }

    private fun buildAssertionDescription(assertion: Assertion): String {
        return "Assert ${assertion.target} ${assertion.operator} ${assertion.expectedValue}"
    }
}
