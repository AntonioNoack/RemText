package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors
import me.anno.remtext.colors.Colors.BRACKET
import me.anno.remtext.colors.Colors.COMMENT
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.DOC_COMMENT
import me.anno.remtext.colors.Colors.KEYWORD
import me.anno.remtext.colors.Colors.ML_COMMENT
import me.anno.remtext.colors.Colors.ML_STRING
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.SYMBOL
import me.anno.remtext.colors.Colors.VARIABLE
import me.anno.remtext.colors.Language
import me.anno.remtext.font.Line
import me.anno.remtext.formatting.AutoFormatOptions
import me.anno.remtext.formatting.JsonFormatter

/**
 * Unified syntax highlighter for multiple C-like languages
 */
class CLikeLanguage(private val type: CLikeLanguageType) : Language {
    companion object {

        fun isLetter(c: Char) = c.isLetterOrDigit() || c in "_$"

        fun findEndOfString(line: Line, start: Int, symbol: Char): Int {
            val text = line.text
            var i = start + 1
            while (i < line.i1) {
                when (text[i++]) {
                    '\\' -> i++
                    symbol -> return i
                }
            }
            return line.i1
        }

        fun isHexChar(c: Char) = c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'

        fun findEndOfNumber(line: Line, start: Int): Int {
            val text = line.text
            var j = start
            if (text[j] in "+-") j++
            when {
                line.startsWith("0x", j, true) -> {
                    j += 2
                    while (j < line.i1 && isHexChar(text[j])) j++
                    return j
                }
                line.startsWith("0b", j, true) -> {
                    j += 2
                    while (j < line.i1 && text[j] in '0'..'1') j++
                    return j
                }
                line.startsWith("0o", j, true) -> { // Python
                    j += 2
                    while (j < line.i1 && text[j] in '0'..'7') j++
                    return j
                }
            }
            while (j < line.i1 && text[j] in '0'..'9') j++
            if (line.startsWith(".", j)) {
                j++
                while (j < line.i1 && text[j] in '0'..'9') j++
            }
            if (line.startsWith("e", j, true)) {
                j++
                if (j < line.i1 && text[j] in "+-") j++
                while (j < line.i1 && text[j] in '0'..'9') j++
            }
            return j
        }

        /**
         * Kotlin/Swift """...""" strings
         * */
        private fun findTripleQuoteStringEnd(line: Line, start: Int): Int {
            val text = line.text
            val type = text[start]
            var i = start + 3
            while (i < line.i1) {
                if (i + 2 < line.i1 &&
                    text[i] == type &&
                    text[i + 1] == type &&
                    text[i + 2] == type
                ) return i + 3
                i++
            }
            return line.i1
        }

        /**
         * JS backtick strings
         * */
        private fun findBacktickStringEnd(line: Line, start: Int): Int {
            val text = line.text
            var i = start + 1
            while (i < line.i1) {
                when (text[i++]) {
                    '\\' -> i++
                    '`' -> return i
                }
            }
            return line.i1
        }

        /**
         * Rust raw strings r#"..."# or r##"..."##
         * */
        private fun findRustRawStringEnd(line: Line, start: Int): Int {
            val text = line.text
            var i = start + 1
            // Count #s
            var hashCount = 0
            while (i < line.i1 && text[i] == '#') {
                hashCount++
                i++
            }
            if (i >= line.i1 || text[i] != '"') return line.i1
            i++ // skip opening quote

            while (i < line.i1) {
                if (text[i] == '"') {
                    var j = i + 1
                    var matchedHashes = 0
                    while (matchedHashes < hashCount && j < line.i1 && text[j] == '#') {
                        j++
                        matchedHashes++
                    }
                    if (matchedHashes == hashCount) return j
                }
                i++
            }
            return line.i1
        }

        fun Line.readKeywords(
            i0: Int, keywords: List<String>?, ignoreCase: Boolean,
            elseInt: Int
        ): Int {
            val lineI1 = i1
            if (keywords != null) for (keyword in keywords) {
                val i1 = i0 + keyword.length
                if (startsWith(keyword, i0, ignoreCase) &&
                    (i1 >= lineI1 || !text[i1].isLetterOrDigit())
                ) {
                    colors?.fill(KEYWORD, i0, i1)
                    return i1
                }
            }
            return elseInt
        }

        fun String.splitKeywords(ignoreCase: Boolean) = KeywordMap(split(','), ignoreCase)

    }

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = if (state0 == ML_COMMENT || state0 == ML_STRING) state0 else DEFAULT
        val text = line.text
        val colors = line.colors ?: return state
        if (colors.isEmpty()) return state

        var i = line.i0

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> {
                    // --- Keyword detection ---
                    if (i == line.i0 || !isLetter(text[i - 1])) {
                        i = line.readKeywords(i, type.keywords[text[i]], false, i)
                    }

                    // --- Comments ---
                    when {
                        line.startsWith("/**", i) -> {
                            val end = line.indexOf("*/", i + 2, false)
                            if (end >= 0) {
                                colors.fill(DOC_COMMENT, i, end + 2)
                                i = end + 2
                            } else {
                                colors.fill(DOC_COMMENT, i, line.i1)
                                i = line.i1
                                state = ML_COMMENT
                            }
                        }
                        line.startsWith("/*", i) -> {
                            val end = line.indexOf("*/", i + 2, false)
                            if (end >= 0) {
                                colors.fill(ML_COMMENT, i, end + 2)
                                i = end + 2
                            } else {
                                colors.fill(ML_COMMENT, i, line.i1)
                                i = line.i1
                                state = ML_COMMENT
                            }
                        }
                        line.startsWith("//", i) && type.supportsDoubleSlashComment -> {
                            val todoIndex = line.indexOf("TODO", i + 2, true)
                            if (todoIndex >= 0) {
                                colors.fill(COMMENT, i, todoIndex)
                                colors.fill(Colors.TODO, todoIndex, line.i1)
                            } else {
                                colors.fill(COMMENT, i, line.i1)
                            }
                            i = line.i1
                        }
                        line.startsWith("#", i) && type.supportsHashTagComment -> {
                            val todoIndex = line.indexOf("TODO", i + 1, true)
                            if (todoIndex >= 0) {
                                colors.fill(COMMENT, i, todoIndex)
                                colors.fill(Colors.TODO, todoIndex, line.i1)
                            } else {
                                colors.fill(COMMENT, i, line.i1)
                            }
                            i = line.i1
                        }
                        line.startsWith("#", i) && type.supportsPreprocessor -> {
                            colors.fill(VARIABLE, i, line.i1)
                            i = line.i1
                        }

                        // Strings
                        line.startsWith("\"\"\"", i) && type.supportsTriangleStrings -> {
                            val end = findTripleQuoteStringEnd(line, i)
                            colors.fill(STRING, i, end)
                            i = end
                        }
                        line.startsWith("'''", i) && type == CLikeLanguageType.PYTHON -> {
                            val end = findTripleQuoteStringEnd(line, i)
                            colors.fill(STRING, i, end)
                            i = end
                        }
                        line.startsWith("`", i) && type.supportsBacktickStrings -> {
                            val end = findBacktickStringEnd(line, i)
                            colors.fill(ML_STRING, i, end)
                            i = end
                        }
                        line.startsWith("r", i) && type == CLikeLanguageType.RUST &&
                                i + 1 < line.i1 && text[i + 1] == '"' -> {
                            val end = findRustRawStringEnd(line, i)
                            colors.fill(STRING, i, end)
                            i = end
                        }

                        else -> when (text[i]) {
                            '"' -> {
                                val end = findEndOfString(line, i, '"')
                                colors.fill(STRING, i, end)
                                i = end
                            }
                            '\'' -> {
                                val end = findEndOfString(line, i, '\'')
                                colors.fill(STRING, i, end)
                                i = end
                            }
                            '`' -> if (type.supportsBacktickStrings) {
                                colors[i++] = ML_STRING
                                state = ML_STRING
                            } else colors[i++] = SYMBOL

                            in "+-" -> {
                                if (i + 1 < line.i1 && text[i + 1] in ".0123456789") {
                                    val end = findEndOfNumber(line, i)
                                    colors.fill(NUMBER, i, end)
                                    i = end
                                } else colors[i++] = SYMBOL
                            }
                            '.' -> {
                                if (i + 1 < line.i1 && text[i + 1] in '0'..'9') {
                                    val end = findEndOfNumber(line, i)
                                    colors.fill(NUMBER, i, end)
                                    i = end
                                } else colors[i++] = SYMBOL
                            }
                            in '0'..'9' -> {
                                val end = findEndOfNumber(line, i)
                                colors.fill(NUMBER, i, end)
                                i = end
                            }
                            in ";,/*<>%&|!?=:+-" -> colors[i++] = SYMBOL
                            in "([{" -> colors[i++] = BRACKET
                            in ")]}" -> colors[i++] = BRACKET
                            in 'A'..'Z', in 'a'..'z' -> {
                                var end = i + 1
                                while (end < line.i1 && (text[end].isLetterOrDigit() || text[end] in "_")) end++
                                colors.fill(VARIABLE, i, end)
                                i = end
                            }
                            else -> i++
                        }
                    }
                }
                ML_COMMENT -> {
                    if (line.startsWith("*/", i)) {
                        colors.fill(ML_COMMENT, i, i + 2)
                        i += 2
                        state = DEFAULT
                    } else colors[i++] = ML_COMMENT
                }
                ML_STRING -> {
                    val end = line.indexOf('`', i, false)
                    if (end >= 0) {
                        colors.fill(ML_STRING, i, end + 1)
                        i = end + 1
                        state = DEFAULT
                    } else {
                        colors.fill(ML_STRING, i, line.i1)
                        i = line.i1
                    }
                }
                else -> i++
            }
        }

        colors[line.i1] = state
        return state
    }

    override fun format(lines: List<Line>, options: AutoFormatOptions): List<Line>? {
        return when (type) {
            CLikeLanguageType.JSON -> JsonFormatter
                .format(lines, options.indentation, options.lineBreakLength)
            else -> null
        }
    }

    override fun toString(): String = type.name
}
