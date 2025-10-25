package me.anno.remtext.colors.impl

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
class XMLLanguage(val isHtml: Boolean = false) : Language {

    companion object {
        private fun isNameChar(c: Char): Boolean {
            return c.isLetterOrDigit() || c == ':' || c == '_' || c == '-'
        }
    }

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = when (state0) {
            ML_COMMENT, STRING, ML_STRING -> state0
            else -> DEFAULT
        }

        val text = line.text
        val colors = line.colors ?: return state
        if (colors.isEmpty()) return state

        var i = line.i0

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> when {
                    line.startsWith("<!--", i) -> {
                        val end = line.indexOf("-->", i + 4, false)
                        if (end >= 0) {
                            colors.fill(ML_COMMENT, i, end + 3)
                            i = end + 3
                        } else {
                            colors.fill(ML_COMMENT, i, line.i1)
                            state = ML_COMMENT
                            i = line.i1
                        }
                    }
                    line.startsWith("<![CDATA[", i, false) -> {
                        val end = line.indexOf("]]>", i + 9, false)
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
                        val end = line.indexOf(">", i + 9, false)
                        if (end >= 0) {
                            colors.fill(KEYWORD, i, end + 1)
                            i = end + 1
                        } else {
                            colors.fill(KEYWORD, i, line.i1)
                            i = line.i1
                        }
                    }
                    line.startsWith("<?", i, false) -> {
                        // --- PROCESSING INSTRUCTION ---
                        val end = line.indexOf("?>", i + 2, false)
                        if (end >= 0) {
                            colors.fill(KEYWORD, i, end + 2)
                            i = end + 2
                        } else {
                            colors.fill(KEYWORD, i, line.i1)
                            i = line.i1
                        }
                    }
                    text[i] == '<' -> {
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
                    val end = line.indexOf("-->", i, false)
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
                    val end = line.indexOf("]]>", i, false)
                    if (end >= 0) {
                        colors.fill(ML_STRING, i, end + 3)
                        i = end + 3
                        state = DEFAULT
                    } else {
                        colors.fill(ML_STRING, i, line.i1)
                        i = line.i1
                    }
                }
                STRING -> throw IllegalStateException("STRING should not occur outside attributes")
            }
        }

        return colors[line.i1 - 1]
    }

    override fun format(lines: List<Line>, options: AutoFormatOptions): List<Line> {
        return XMLFormatter.format(lines, options.indentation, isHtml).colorize()
    }

    override fun toString(): String {
        return if (isHtml) "HTML" else "XML"
    }
}
