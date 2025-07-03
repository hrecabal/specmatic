package io.specmatic.conversions

import io.specmatic.core.*
import io.specmatic.core.pattern.Row
import io.specmatic.core.pattern.StringPattern
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import org.apache.http.HttpHeaders.AUTHORIZATION

data class BearerSecurityScheme(private val configuredToken: String? = null) : OpenAPISecurityScheme {
    override fun matches(httpRequest: HttpRequest, resolver: Resolver): Result {
        val authHeaderValue = httpRequest.headers.entries.find {
            it.key.equals(AUTHORIZATION, ignoreCase = true)
        } ?: return when(resolver.mockMode) {
            true -> Result.Success()
            else -> Result.Failure(
                breadCrumb = BreadCrumb.HEADER.with(AUTHORIZATION),
                message = resolver.mismatchMessages.expectedKeyWasMissing("Header", AUTHORIZATION)
            )
        }

        if (!authHeaderValue.value.lowercase().startsWith("bearer")) {
            return Result.Failure(
                breadCrumb = BreadCrumb.HEADER.with(AUTHORIZATION),
                message = "$AUTHORIZATION header must be prefixed with \"Bearer\""
            )
        }

        return Result.Success()
    }

    override fun removeParam(httpRequest: HttpRequest): HttpRequest {
        val headersWithoutAuthorization = httpRequest.headers.filterKeys { !it.equals(AUTHORIZATION, ignoreCase = true) }
        return httpRequest.copy(headers = headersWithoutAuthorization)
    }

    override fun addTo(httpRequest: HttpRequest, resolver: Resolver): HttpRequest {
        return httpRequest.addSecurityHeader(AUTHORIZATION, getAuthorizationHeaderValue(resolver))
    }

    override fun addTo(requestPattern: HttpRequestPattern, row: Row): HttpRequestPattern {
        return addToHeaderType(AUTHORIZATION, row, requestPattern)
    }

    override fun isInRow(row: Row): Boolean {
        return row.columnNames.any { it.equals(AUTHORIZATION, ignoreCase = true) }
    }

    override fun isInRequest(request: HttpRequest, complete: Boolean): Boolean {
        return request.hasHeader(AUTHORIZATION)
    }

    private fun getAuthorizationHeaderValue(resolver: Resolver): String {
        val updatedResolver = resolver.updateLookupForParam(BreadCrumb.HEADER.value)
        return "Bearer " + (configuredToken ?: updatedResolver.generate(null, AUTHORIZATION, StringPattern()).toStringLiteral())
    }

    override fun copyFromTo(originalRequest: HttpRequest, newHttpRequest: HttpRequest): HttpRequest {
        if (!originalRequest.headers.containsKey(AUTHORIZATION)) return newHttpRequest
        return newHttpRequest.addSecurityHeader(AUTHORIZATION, originalRequest.headers.getValue(AUTHORIZATION))
    }

    override fun getHeaderKey(): String? {
        return AUTHORIZATION
    }

    override fun warnIfExistsInParameters(parameters: List<Parameter>, method: String, path: String) {
        val matchingHeaders = parameters.filterIsInstance<HeaderParameter>().filter {
            it.name.equals(AUTHORIZATION, ignoreCase = true)
        }

        if(matchingHeaders.isNotEmpty()) {
            printWarningsForOverriddenSecurityParameters(
                matchingParameters = matchingHeaders,
                securitySchemeDescription = "Bearer Authorization",
                httpParameterType = "header",
                method = method,
                path = path
            )

        }
    }
}
