package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors.COLOR_BITS
import me.anno.remtext.colors.Colors.COLOR_MASK
import me.anno.remtext.colors.Colors.DEFAULT
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
import me.anno.remtext.formatting.XMLFormatter

/**
 * ChatGPT-generated, and slightly adjusted
 * */
class XMLLanguage(val isHTML: Boolean = false) : Language {

    companion object {
        private fun isNameChar(c: Char): Boolean {
            return c.isLetterOrDigit() || c == ':' || c == '_' || c == '-'
        }

        val js = CLikeLanguage(CLikeLanguageType.JAVASCRIPT)
        val css = CSSLanguage
        val php = CLikeLanguage(CLikeLanguageType.PHP)

        const val JS_INDEX = 1
        const val PHP_INDEX = 2
        const val CSS_INDEX = 3

        fun packState(langIndex: Int, langState: Byte): Byte {
            return ((langIndex shl COLOR_BITS) or (langState.toInt() and COLOR_MASK)).toByte()
        }

        fun unpackLang(state: Byte): Int {
            return state.toInt() shr COLOR_BITS
        }

        fun unpackLangState(state: Byte): Byte {
            return (state.toInt() and COLOR_MASK).toByte()
        }
    }

    override fun highlight(line: Line, state0: Byte): Byte {
        val colors = line.colors ?: return state0
        if (colors.isEmpty()) return state0

        val text = line.text
        var state = state0
        var i = line.i0

        fun handleTag() {
            // --- TAG START ---
            colors[i++] = SYMBOL
            if (i < line.i1 && text[i] == '/') {
                colors[i++] = SYMBOL
            }

            // Tag name
            val start = i
            while (i < line.i1 && isNameChar(text[i])) i++
            if (i > start) colors.fill(VARIABLE, start, i)

            // Inside tag: attributes
            while (i < line.i1) {
                when {
                    text[i].isWhitespace() -> i++

                    // Tag close
                    text[i] == '>' -> {
                        colors[i++] = SYMBOL
                        break
                    }

                    // Self-close or HTML void tag
                    line.startsWith("/>", i) -> {
                        colors.fill(SYMBOL, i, i + 2)
                        i += 2
                        break
                    }

                    // Attribute
                    isNameChar(text[i]) -> {
                        val attrStart = i
                        while (i < line.i1 && isNameChar(text[i])) i++
                        colors.fill(VARIABLE, attrStart, i)

                        // Equal and value
                        if (i < line.i1 && text[i] == '=') {
                            colors[i++] = SYMBOL
                            if (i < line.i1 && (text[i] == '"' || text[i] == '\'')) {
                                val quote = text[i++]
                                val valueStart = i
                                while (i < line.i1 && text[i] != quote) i++
                                colors.fill(STRING, valueStart - 1, minOf(i + 1, line.i1))
                                if (i < line.i1 && text[i] == quote) i++
                            }
                        }
                    }

                    else -> i++
                }
            }
        }

        loop@ while (i < line.i1) {
            val langIndex = unpackLang(state)
            val endIndex = when (langIndex) {
                JS_INDEX -> {
                    state = js.highlight(line.subLine(i, line.i1), unpackLangState(state))
                    detectEndScriptSafely(line, i, "</script>", JS_INDEX)
                }
                PHP_INDEX -> {
                    state = php.highlight(line.subLine(i, line.i1), unpackLangState(state))
                    detectEndScriptSafely(line, i, "?>", PHP_INDEX)
                }
                CSS_INDEX -> {
                    state = css.highlight(line.subLine(i, line.i1), unpackLangState(state))
                    detectEndScriptSafely(line, i, "</style>", CSS_INDEX)
                }
                else -> {
                    when (unpackLangState(state)) {
                        DEFAULT -> when {
                            line.startsWith("<!--", i) -> {
                                val end = line.indexOf("-->", i + 4)
                                if (end >= 0) {
                                    colors.fill(ML_COMMENT, i, end + 3)
                                    i = end + 3
                                } else {
                                    colors.fill(ML_COMMENT, i, line.i1)
                                    state = ML_COMMENT
                                    i = line.i1
                                }
                            }
                            line.startsWith("<![CDATA[", i, true) -> {
                                val end = line.indexOf("]]>", i + 9)
                                if (end >= 0) {
                                    colors.fill(ML_STRING, i, end + 3)
                                    i = end + 3
                                } else {
                                    colors.fill(ML_STRING, i, line.i1)
                                    state = ML_STRING
                                    i = line.i1
                                }
                            }
                            line.startsWith("<!DOCTYPE", i, true) -> {
                                val end = line.indexOf(">", i + 9)
                                if (end >= 0) {
                                    colors.fill(KEYWORD, i, end + 1)
                                    i = end + 1
                                } else {
                                    colors.fill(KEYWORD, i, line.i1)
                                    i = line.i1
                                }
                            }
                            line.startsWith("<?php", i, true) -> {
                                colors.fill(KEYWORD, i, i + 5)
                                state = packState(PHP_INDEX, DEFAULT)
                                i += 5
                            }
                            line.startsWith("<?=", i) -> {
                                colors.fill(KEYWORD, i, i + 3)
                                state = packState(PHP_INDEX, DEFAULT)
                                i += 3
                            }
                            line.startsWith("<?", i) -> {
                                // --- PROCESSING INSTRUCTION ---
                                val end = line.indexOf("?>", i + 2)
                                if (end >= 0) {
                                    colors.fill(KEYWORD, i, end + 2)
                                    i = end + 2
                                } else {
                                    colors.fill(KEYWORD, i, line.i1)
                                    i = line.i1
                                }
                            }
                            line.startsWith("{{", i) -> {
                                var end = line.indexOf("}}", i + 2)
                                end = if (end < 0) line.i1 else end + 2
                                // Delegate to JS/CLanguage highlighting for the inner expression
                                val subLine = line.subLine(i, end)
                                CLikeLanguage(CLikeLanguageType.JAVASCRIPT)
                                    .highlight(subLine, DEFAULT)
                                i = end
                            }
                            line.startsWith("<script", i, true) -> {
                                if (isHTML) {
                                    // Skip tag and attributes
                                    val tagEnd = line.indexOf(">", i)
                                    if (tagEnd >= 0) {
                                        colors.fill(KEYWORD, i, tagEnd + 1)
                                        state = packState(JS_INDEX, DEFAULT)
                                        i = tagEnd + 1
                                    } else handleTag()
                                } else handleTag()
                            }
                            // todo css-highlighting inside style-properties
                            line.startsWith("<style", i, true) -> {
                                if (isHTML) {
                                    // Skip tag and attributes
                                    val tagEnd = line.indexOf(">", i)
                                    if (tagEnd >= 0) {
                                        colors.fill(KEYWORD, i, tagEnd + 1)
                                        state = packState(CSS_INDEX, DEFAULT)
                                        i = tagEnd + 1
                                    } else {
                                        handleTag()
                                    }
                                } else handleTag()
                            }
                            text[i] == '<' -> handleTag()
                            text[i] == '&' -> {
                                // --- ENTITY (&lt;, &amp;, etc.) ---
                                val start = i++
                                while (i < line.i1 && text[i] != ';' && !text[i].isWhitespace()) i++
                                if (i < line.i1 && text[i] == ';') i++
                                colors.fill(NUMBER, start, i) // Entities as NUMBER color
                            }
                            else -> i++
                        }
                        ML_COMMENT -> {
                            val end = line.indexOf("-->", i)
                            if (end >= 0) {
                                colors.fill(ML_COMMENT, i, end + 3)
                                i = end + 3
                                state = DEFAULT
                            } else {
                                colors.fill(ML_COMMENT, i, line.i1)
                                i = line.i1
                            }
                        }
                        ML_STRING -> {
                            // =================================
                            // MULTILINE CDATA
                            // =================================
                            val end = line.indexOf("]]>", i)
                            if (end >= 0) {
                                colors.fill(ML_STRING, i, end + 3)
                                i = end + 3
                                state = DEFAULT
                            } else {
                                colors.fill(ML_STRING, i, line.i1)
                                i = line.i1
                            }
                        }
                        else -> i++
                    }
                    continue@loop
                }
            }
            if (endIndex >= 0) {
                i = endIndex
                state = DEFAULT // continue with HTML
            } else {
                // end not yet found
                state = packState(langIndex, state)
                i = line.i1
            }
        }

        colors[line.i1] = state
        return state
    }

    fun detectEndScriptSafely(line: Line, i0: Int, suffix: String, langIndex: Int): Int {
        var i = i0
        while (true) {
            i = line.indexOf(suffix, i, true)
            if (i < 0) return -1

            val colors = line.colors!!
            // Only if current position is DEFAULT in JS
            if (colors[i] != STRING && colors[i] != ML_COMMENT) {
                for (j in i0 until i) {
                    colors[j] = packState(langIndex, colors[j])
                }
                colors.fill(KEYWORD, i, i + suffix.length)
                return i + suffix.length
            }
        }
    }

    override fun format(lines: List<Line>, options: AutoFormatOptions): List<Line> {
        return XMLFormatter.format(lines, options.indentation, isHTML)
    }

    override fun toString(): String {
        return if (isHTML) "HTML" else "XML"
    }
}
