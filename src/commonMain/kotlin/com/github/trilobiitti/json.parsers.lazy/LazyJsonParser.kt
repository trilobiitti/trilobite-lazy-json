package com.github.trilobiitti.json.parsers.lazy

abstract class LazyJsonParser<
        TObject : Any,
        TArray : Any,
        TObjectBuilder,
        TArrayBuilder
        > {
    abstract fun startObject(): TObjectBuilder
    abstract fun addField(builder: TObjectBuilder, fieldName: String, value: Any?): TObjectBuilder
    abstract fun endObject(builder: TObjectBuilder): TObject
    abstract fun lazyObject(string: String, spanStart: Int, spanEnd: Int): TObject

    abstract fun startArray(): TArrayBuilder
    abstract fun addElement(builder: TArrayBuilder, value: Any?): TArrayBuilder
    abstract fun endArray(builder: TArrayBuilder): TArray
    abstract fun lazyArray(string: String, spanStart: Int, spanEnd: Int): TArray

    protected open fun parseFloat(string: String, spanStart: Int, spanEnd: Int): Number =
        string
            .substring(spanStart, spanEnd)
            .toDouble()

    protected open fun parseInteger(string: String, spanStart: Int, spanEnd: Int): Number =
        parseFloat(string, spanStart, spanEnd)

    protected fun parseObject(string: String, spanStart: Int, spanEnd: Int): TObject {
        var index = skipWhitespace(string, spanStart + 1)
        var builder = startObject()

        if (string[index] == '}') {
            return endObject(builder)
        }

        while (true) {
            val (i0, key) = parseString(string, index)

            index = skipWhitespace(string, i0)

            when (val c = string[index]) {
                ':' -> {
                }
                else -> throw IllegalArgumentException(
                    "Unexpected character '$c' at position $index in JSON string. Expected is: ':'."
                )
            }

            index = skipWhitespace(string, index + 1)

            val (i1, value) = parseValueLazy(string, index)

            index = skipWhitespace(string, i1)
            builder = addField(builder, key, value)

            when (val c = string[index]) {
                ',' -> {
                    ++index
                }
                '}' -> return endObject(builder)
                else -> throw IllegalArgumentException(
                    "Unexpected character '$c' at position $index in JSON string. Expected are: ',', '}'."
                )
            }

            index = skipWhitespace(string, index)
        }
    }

    protected fun parseArray(string: String, spanStart: Int, spanEnd: Int): TArray {
        var index = skipWhitespace(string, spanStart + 1)
        var builder = startArray()

        if (string[index] == ']') {
            return endArray(builder)
        }

        while (true) {
            val (i, v) = parseValueLazy(string, index)

            builder = addElement(builder, v)
            index = skipWhitespace(string, i)

            when (val c = string[index]) {
                ',' -> {
                    ++index
                }
                ']' -> return endArray(builder)
                else -> throw IllegalArgumentException(
                    "Unexpected character '$c' at position $index in JSON string. Expected are: ',', ']'."
                )
            }

            index = skipWhitespace(string, index)
        }
    }

    /**
     * ```
     * {"answer": 42.0}
     *            ^------ start
     *                ^-- returned
     * ```
     */
    private fun parseNumber(string: String, start: Int): Pair<Int, Number> {
        var index = start + 1 // `parseNumber` is expected to be called with `string[start]` of '-' or '0'..'9'
        val len = string.length

        parseInt@ while (true) {
            if (index == len) {
                return index to parseInteger(string, start, index)
            }

            when (string[index]) {
                '.' -> break@parseInt
                'e' -> break@parseInt
                'E' -> break@parseInt
                in '0'..'9' -> {
                    ++index
                }
                else -> return index to parseInteger(string, start, index)
            }
        }
        // `string[index]` is ether '.' or 'e'/'E'
        if (string[index] == '.') {
            ++index

            parseFraction@ while (true) {
                if (index == len) {
                    return index to parseFloat(string, start, index)
                }

                when (string[index]) {
                    in '0'..'9' -> {
                        ++index
                    }
                    'e' -> break@parseFraction
                    'E' -> break@parseFraction
                    else -> return index to parseFloat(string, start, index)
                }
            }
        }

        // `string[index]` is 'e' or 'E'
        ++index

        when (val c = string[index]) {
            '+' -> {
                ++index
            }
            '-' -> {
                ++index
            }
            in '0'..'9' -> {
            }
            else -> throw IllegalArgumentException(
                "Illegal character '$c' at position $index in JSON string"
            )
        }

        if (string[index] !in '0'..'9') {
            throw IllegalArgumentException(
                "Invalid numeric literal at position $index. At least one exponent digit is expected after '${string[index - 1]}'"
            )
        }

        ++index

        while (true) {
            when {
                len == index -> break
                string[index] !in '0'..'9' -> break
            }

            ++index
        }

        return index to parseFloat(string, start, index)
    }

    /**
     * ```
     * ["Hello world!"]
     *  ^---------------- start
     *                ^-- end
     * ```
     */
    private fun parseString(string: String, start: Int): Pair<Int, String> {
        var index = start

        noEscapeString@ while (true) {
            ++index

            when (string[index]) {
                '"' -> {
                    return index + 1 to string.substring(start + 1, index)
                }
                '\\' -> {
                    break@noEscapeString
                }
            }
        }

        val stringBuilder = StringBuilder(string.subSequence(start + 1, index))

        while (true) {
            when (val c = string[index]) {
                '"' -> return index + 1 to stringBuilder.toString()
                '\\' -> {
                    ++index
                    stringBuilder.append(
                        when (val esc = string[index]) {
                            '"' -> '"'
                            '\\' -> '\\'
                            '/' -> '/'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'u' -> {
                                index += 4
                                string.substring(index - 3, index + 1).toInt(16).toChar()
                            }
                            else -> throw IllegalArgumentException(
                                "Illegal string escape character '$esc' at position $index in JSON string"
                            )
                        }
                    )
                }
                else -> stringBuilder.append(c)
            }

            ++index
        }
    }

    /**
     * ```
     * [{"foo": "bar"}]
     *  ^---------------- start
     *                ^-- returned
     * ```
     *
     * ```
     * {"foo": ["bar"]}
     *         ^--------- start
     *                ^-- returned
     * ```
     */
    private fun skipStructure(string: String, start: Int, open: Char, close: Char): Int {
        var index = start
        var depth = 0

        do {
            when (string[index]) {
                open -> {
                    ++depth
                }
                close -> {
                    --depth
                }
                '"' -> {
                    skipString@ while (true) {
                        ++index

                        when (string[index]) {
                            '\\' -> {
                                ++index
                            }
                            '"' -> break@skipString
                        }
                    }
                }
            }

            ++index
        } while (depth != 0)

        return index
    }

    /**
     * ```
     * "foo",    42
     *       ^------- start
     *           ^--- returned
     * ```
     */
    private fun skipWhitespace(string: String, start: Int): Int {
        var index = start

        while (string[index].isWhitespace()) {
            ++index
        }

        return index
    }

    private fun parseValueLazy(string: String, spanStart: Int): Pair<Int, Any?> {
        when (string[spanStart]) {
            '{' -> {
                val end = skipStructure(string, spanStart, '{', '}')

                return end to lazyObject(string, spanStart, end)
            }

            '[' -> {
                val end = skipStructure(string, spanStart, '[', ']')

                return end to lazyArray(string, spanStart, end)
            }

            '"' -> {
                return parseString(string, spanStart)
            }

            't' -> {
                if (string.regionMatches(spanStart, TRUE_STR, 0, TRUE_STR.length, false)) {
                    return spanStart + TRUE_STR.length to true
                }
            }

            'f' -> {
                if (string.regionMatches(spanStart, FALSE_STR, 0, FALSE_STR.length, false)) {
                    return spanStart + FALSE_STR.length to false
                }
            }

            'n' -> {
                if (string.regionMatches(spanStart, NULL_STR, 0, NULL_STR.length, false)) {
                    return spanStart + NULL_STR.length to null
                }
            }

            '-' -> {
                return parseNumber(string, spanStart)
            }

            in '0'..'9' -> {
                return parseNumber(string, spanStart)
            }
        }

        throw IllegalArgumentException(
            "Illegal character '${string[spanStart]}' at position $spanStart in JSON string"
        )
    }

    fun parse(string: String): Any? = parseValueLazy(string, skipWhitespace(string, 0)).second

    companion object {
        private const val TRUE_STR = "true"
        private const val FALSE_STR = "false"
        private const val NULL_STR = "null"
    }
}
