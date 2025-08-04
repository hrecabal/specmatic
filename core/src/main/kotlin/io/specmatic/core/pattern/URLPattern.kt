package io.specmatic.core.pattern

import io.specmatic.core.Resolver
import io.specmatic.core.Result
import io.specmatic.core.pattern.config.NegativePatternConfiguration
import io.specmatic.core.value.JSONArrayValue
import io.specmatic.core.value.StringValue
import io.specmatic.core.value.Value
import java.net.URI

data class URLPattern(val scheme: URLScheme = URLScheme.HTTPS, override val typeAlias: String? = null): Pattern, ScalarType {
    override val pattern: String = "(url)"

    override fun matches(sampleData: Value?, resolver: Resolver): Result {
        return when (sampleData) {
            is StringValue -> {
                if(scheme.matches(parse(sampleData.string, resolver))) {
                    Result.Success()
                } else Result.Failure("Expected ${sampleData.string} to be ${scheme.type}")
            }
            else -> Result.Failure("URLs can only be held in strings.")
        }
    }

    override fun generate(resolver: Resolver): StringValue {
        val providedString = resolver.provideString(this)
        return providedString ?: StringValue("${scheme.prefix}${randomString().lowercase()}.com/${randomString().lowercase()}")
    }

    override fun newBasedOn(row: Row, resolver: Resolver): Sequence<ReturnValue<Pattern>> = sequenceOf(HasValue(this))

    override fun newBasedOn(resolver: Resolver): Sequence<Pattern> = sequenceOf(this)
    override fun negativeBasedOn(row: Row, resolver: Resolver, config: NegativePatternConfiguration): Sequence<ReturnValue<Pattern>> {
        return newBasedOn(row, resolver)
    }

    override fun parse(value: String, resolver: Resolver): StringValue = StringValue(URI.create(value).toString())

    override fun encompasses(otherPattern: Pattern, thisResolver: Resolver, otherResolver: Resolver, typeStack: TypeStack): Result {
        return when(otherPattern) {
            this -> Result.Success()
            is URLPattern -> Result.Failure("Expected ${scheme.type}, got ${otherPattern.scheme.type}")
            else -> Result.Failure("Expected $typeName, got ${otherPattern.typeName}")
        }
    }

    override fun listOf(valueList: List<Value>, resolver: Resolver): Value {
        return JSONArrayValue(valueList)
    }

    override val typeName: String = "url"
}
