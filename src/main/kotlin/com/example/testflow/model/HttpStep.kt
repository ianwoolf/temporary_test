package com.example.testflow.model

/**
 * HTTP request step - domain model for runtime use
 */
data class HttpStep(
    override val id: String,
    override val name: String,
    override val description: String,
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val authentication: AuthConfig? = null,
    val assertions: List<Assertion> = emptyList()
) : TestStep {

    companion object {
        fun fromDto(dto: TestStepDto.HttpStepDto): HttpStep {
            return HttpStep(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                method = HttpMethod.valueOf(dto.method),
                url = dto.url,
                headers = dto.headers,
                body = dto.body,
                authentication = dto.authentication?.let { AuthConfig.fromDto(it) },
                assertions = dto.assertions.map { Assertion.fromDto(it) }
            )
        }
    }

    fun toDto(): TestStepDto.HttpStepDto {
        return TestStepDto.HttpStepDto(
            id = id,
            name = name,
            description = description,
            method = method.name,
            url = url,
            headers = headers,
            body = body,
            authentication = authentication?.toDto(),
            assertions = assertions.map { it.toDto() }
        )
    }
}

/**
 * HTTP method enum
 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS
}

/**
 * Authentication configuration - domain model
 */
data class AuthConfig(
    val type: AuthType,
    val parameters: Map<String, String> = emptyMap()
) {
    companion object {
        fun fromDto(dto: AuthConfigDto): AuthConfig {
            return AuthConfig(
                type = dto.type,
                parameters = dto.parameters
            )
        }
    }

    fun toDto(): AuthConfigDto {
        return AuthConfigDto(
            type = type,
            parameters = parameters
        )
    }
}

/**
 * Assertion for validating responses - domain model
 */
data class Assertion(
    val type: AssertionType,
    val target: String,
    val operator: AssertionOperator,
    val expectedValue: String
) {
    companion object {
        fun fromDto(dto: AssertionDto): Assertion {
            return Assertion(
                type = dto.type,
                target = dto.target,
                operator = dto.operator,
                expectedValue = dto.expectedValue
            )
        }
    }

    fun toDto(): AssertionDto {
        return AssertionDto(
            type = type,
            target = target,
            operator = operator,
            expectedValue = expectedValue
        )
    }
}
