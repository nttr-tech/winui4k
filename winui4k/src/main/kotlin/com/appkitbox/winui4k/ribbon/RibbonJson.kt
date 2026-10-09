package com.appkitbox.winui4k.ribbon

/**
 * A small JSON reader and writer for [RibbonStateSerializer] (implemented here because the core does not depend on external libraries).
 *
 * Values are represented by Kotlin types: objects are Map<String, Any?> (preserving key order), arrays are List<Any?>,
 * strings are String, numbers are Long if integral and Double otherwise, booleans are Boolean, and null is null.
 * Reading strictly follows RFC 8259 (no comments or trailing commas), and invalid input throws
 * [IllegalArgumentException].
 */
internal object RibbonJson {
    /** Parses [text]. Throws [IllegalArgumentException] if it is invalid JSON. */
    fun parse(text: String): Any? {
        val reader = Reader(text)
        val value = reader.readValue()
        reader.skipWhitespace()
        require(reader.atEnd()) { "Extra characters after the JSON (position ${reader.position})" }
        return value
    }

    /** Converts [value] to JSON. If [indented], indents with 2 spaces. */
    fun write(value: Any?, indented: Boolean): String = StringBuilder().also { Writer(it, indented).write(value, 0) }.toString()

    private class Reader(private val text: String) {
        var position = 0
            private set

        fun atEnd(): Boolean = position >= text.length

        fun skipWhitespace() {
            while (!atEnd() && text[position] in " \t\r\n") position++
        }

        fun readValue(): Any? {
            skipWhitespace()
            require(!atEnd()) { "The JSON ends unexpectedly" }
            return when (val c = text[position]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> {
                    require(c == '-' || c.isDigit()) { "Invalid character '$c' (position $position)" }
                    readNumber()
                }
            }
        }

        private fun readObject(): Map<String, Any?> {
            val result = LinkedHashMap<String, Any?>()
            position++ // {
            skipWhitespace()
            if (consume('}')) return result
            while (true) {
                skipWhitespace()
                require(!atEnd() && text[position] == '"') { "Missing object key (position $position)" }
                val key = readString()
                skipWhitespace()
                require(consume(':')) { "Missing ':' (position $position)" }
                result[key] = readValue()
                skipWhitespace()
                if (consume('}')) return result
                require(consume(',')) { "Missing ',' or '}' (position $position)" }
            }
        }

        private fun readArray(): List<Any?> {
            val result = ArrayList<Any?>()
            position++ // [
            skipWhitespace()
            if (consume(']')) return result
            while (true) {
                result += readValue()
                skipWhitespace()
                if (consume(']')) return result
                require(consume(',')) { "Missing ',' or ']' (position $position)" }
            }
        }

        private fun readString(): String {
            position++ // "
            val builder = StringBuilder()
            while (true) {
                require(!atEnd()) { "Unterminated string" }
                val c = text[position++]
                when {
                    c == '"' -> return builder.toString()
                    c == '\\' -> builder.append(readEscape())
                    c < ' ' -> throw IllegalArgumentException("Control character in string (position ${position - 1})")
                    else -> builder.append(c)
                }
            }
        }

        private fun readEscape(): Char {
            require(!atEnd()) { "The escape sequence ends unexpectedly" }
            return when (val c = text[position++]) {
                '"' -> '"'
                '\\' -> '\\'
                '/' -> '/'
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    require(position + 4 <= text.length) { "\\u escape is too short" }
                    val hex = text.substring(position, position + 4)
                    require(hex.all { Character.digit(it, 16) >= 0 }) { "Invalid \\u escape: $hex" }
                    position += 4
                    hex.toInt(16).toChar()
                }
                else -> throw IllegalArgumentException("Invalid escape '\\$c'")
            }
        }

        private fun readNumber(): Any {
            val start = position
            if (text[position] == '-') position++
            while (!atEnd() && (text[position].isDigit() || text[position] in ".eE+-")) position++
            val token = text.substring(start, position)
            require(NUMBER.matches(token)) { "Invalid number: $token" }
            val isInteger = token.none { it in ".eE" }
            return if (isInteger) token.toLongOrNull() ?: token.toDouble() else token.toDouble()
        }

        private fun readLiteral(literal: String, value: Any?): Any? {
            require(text.startsWith(literal, position)) { "Invalid literal (position $position)" }
            position += literal.length
            return value
        }

        private fun consume(c: Char): Boolean {
            if (atEnd() || text[position] != c) return false
            position++
            return true
        }
    }

    private class Writer(private val out: StringBuilder, private val indented: Boolean) {
        fun write(value: Any?, depth: Int) {
            when (value) {
                null -> out.append("null")
                is String -> writeString(value)
                is Boolean -> out.append(value)
                is Double -> out.append(formatDouble(value))
                is Number -> out.append(value.toLong())
                is Map<*, *> -> writeContainer('{', '}', value.entries.toList(), depth) { entry ->
                    writeString(entry.key.toString())
                    out.append(if (indented) ": " else ":")
                    write(entry.value, depth + 1)
                }
                is Iterable<*> -> writeContainer('[', ']', value.toList(), depth) { write(it, depth + 1) }
                else -> writeString(value.toString())
            }
        }

        private fun <T> writeContainer(open: Char, close: Char, elements: List<T>, depth: Int, writeElement: (T) -> Unit) {
            out.append(open)
            if (elements.isEmpty()) {
                out.append(close)
                return
            }
            for ((i, element) in elements.withIndex()) {
                if (i > 0) out.append(',')
                newLine(depth + 1)
                writeElement(element)
            }
            newLine(depth)
            out.append(close)
        }

        private fun newLine(depth: Int) {
            if (!indented) return
            out.append('\n')
            repeat(depth) { out.append("  ") }
        }

        private fun writeString(value: String) {
            out.append('"')
            for (c in value) {
                when {
                    c == '"' -> out.append("\\\"")
                    c == '\\' -> out.append("\\\\")
                    c == '\n' -> out.append("\\n")
                    c == '\r' -> out.append("\\r")
                    c == '\t' -> out.append("\\t")
                    c < ' ' -> out.append(String.format(java.util.Locale.ROOT, "\\u%04X", c.code))
                    else -> out.append(c)
                }
            }
            out.append('"')
        }

        /** Integral values without a decimal point (300.0 → 300), others in the shortest form (looks the same as .NET). */
        private fun formatDouble(value: Double): String {
            require(!value.isNaN() && !value.isInfinite()) { "NaN / infinity cannot be written in JSON" }
            return if (value == Math.floor(value) && Math.abs(value) < 1e15) value.toLong().toString() else value.toString()
        }
    }

    private val NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
}
