package app.verdant.mcp

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

/** JSON Schema fragments for MCP tool inputs and the validation the server applies to them. */
object McpSchema {
    fun id(description: String? = null) = prop(description, "type" to "integer", "minimum" to 1)
    fun count(description: String? = null, minimum: Int = 1) = prop(description, "type" to "integer", "minimum" to minimum, "maximum" to 1_000_000)
    fun number(description: String? = null) = prop(description, "type" to "number", "minimum" to 0)
    fun text(maxLength: Int, description: String? = null) = prop(description, "type" to "string", "minLength" to 1, "maxLength" to maxLength)
    fun date(description: String? = null) = prop(description, "type" to "string", "format" to "date", "maxLength" to 10)
    fun bool(description: String? = null) = prop(description, "type" to "boolean")
    fun oneOf(values: List<String>, description: String? = null) = prop(description, "type" to "string", "enum" to values, "maxLength" to 40)
    inline fun <reified E : Enum<E>> oneOf(description: String? = null, exclude: Set<E> = emptySet()) =
        oneOf(enumValues<E>().filterNot { it in exclude }.map { it.name }, description)

    fun prop(description: String?, vararg entries: Pair<String, Any>): Map<String, Any> =
        mapOf(*entries) + (description?.let { mapOf("description" to it) } ?: emptyMap())

    fun validate(args: JsonNode, schema: Map<*, *>) {
        require(args.isObject) { "Arguments must be an object" }
        val properties = schema["properties"] as Map<*, *>
        val required = schema["required"] as List<*>
        required.firstOrNull { !args.has(it.toString()) || args.get(it.toString()).isNull }?.let { throw IllegalArgumentException("Missing required argument: $it") }
        args.fields().forEach { (key, value) ->
            val rule = properties[key] as? Map<*, *> ?: throw IllegalArgumentException("Unknown argument: $key")
            if (value.isNull) return@forEach
            when (rule["type"]) {
                "integer" -> require(value.isIntegralNumber && value.canConvertToLong() && value.longValue() >= (rule["minimum"] as Number).toLong() && value.longValue() <= ((rule["maximum"] as? Number)?.toLong() ?: Long.MAX_VALUE)) { "Invalid integer: $key" }
                "number" -> require(value.isNumber && value.doubleValue().isFinite() && value.doubleValue() >= (rule["minimum"] as Number).toDouble()) { "Invalid number: $key" }
                "boolean" -> require(value.isBoolean) { "Invalid boolean: $key" }
                "string" -> {
                    require(value.isTextual && value.textValue().isNotBlank() && value.textValue().length <= (rule["maxLength"] as Number).toInt()) { "Invalid string: $key" }
                    (rule["enum"] as? List<*>)?.let { require(value.textValue() in it) { "$key must be one of ${it.joinToString()}" } }
                    if (rule["format"] == "date") require(runCatching { LocalDate.parse(value.textValue()) }.isSuccess) { "$key must be YYYY-MM-DD" }
                }
            }
        }
    }
}
