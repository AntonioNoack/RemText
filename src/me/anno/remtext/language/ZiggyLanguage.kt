package me.anno.remtext.language

import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.Colors.KEYWORD
import me.anno.remtext.Colors.ML_COMMENT
import me.anno.remtext.Colors.ML_STRING
import me.anno.remtext.Colors.NUMBER
import me.anno.remtext.Colors.STRING
import me.anno.remtext.Colors.SYMBOL
import me.anno.remtext.Colors.VARIABLE
import me.anno.remtext.font.Line
import me.anno.remtext.formatters.AutoFormatOptions
import me.anno.remtext.formatters.ZiggyFormatter

object ZiggyLanguage : Language {

    private val keywords = listOf("true", "false", "null")
    private fun isIdentifierChar(c: Char) = c.isLetterOrDigit() || c == '_' || c == '-'

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = if (state0 == ML_COMMENT || state0 == ML_STRING) state0 else DEFAULT
        val text = line.text
        val colors = line.colors ?: return state
        if (colors.isEmpty()) return state

        var i = line.i0

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> {
                    when {
                        line.startsWith("//", i) -> { // comment line
                            colors.fill(COMMENT, i, line.i1)
                            i = line.i1
                        }
                        line.startsWith("/*", i) -> {  // comment block
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
                        // multiline strings
                        text[i] == '\\' && i + 1 < line.i1 && text[i + 1] == '\\' -> {
                            // treat entire line as ML_STRING
                            colors.fill(ML_STRING, i, line.i1)
                            i = line.i1
                        }
                        // single-line strings
                        text[i] == '"' -> {
                            val end = CLikeLanguage.findEndOfString(line, i, '"')
                            colors.fill(STRING, i, end)
                            i = end
                        }
                        // numbers
                        text[i].isDigit() -> {
                            val end = CLikeLanguage.findEndOfNumber(line, i)
                            colors.fill(NUMBER, i, end)
                            i = end
                        }
                        // keywords
                        (i == line.i0 || !isIdentifierChar(text[i - 1])) &&
                                keywords.any { kw ->
                                    line.startsWith(kw, i, true) &&
                                            (i + kw.length >= line.i1 ||
                                                    !isIdentifierChar(text[i + kw.length]))
                                } -> {
                            val kw = keywords.first {
                                line.startsWith(it, i, true)
                            }
                            val end = i + kw.length
                            colors.fill(KEYWORD, i, end)
                            i = end
                        }
                        text[i] == '.' -> { // .field/.constructor syntax
                            var j = i + 1
                            while (j < line.i1 && isIdentifierChar(text[j])) j++
                            colors.fill(VARIABLE, i, j)
                            i = j
                        }
                        text[i].isLetter() || text[i] == '_' -> { // identifiers
                            var j = i + 1
                            while (j < line.i1 && isIdentifierChar(text[j])) j++
                            colors.fill(VARIABLE, i, j)
                            i = j
                        }
                        text[i] in "{}[]=(),:" -> { // symbols
                            colors[i++] = SYMBOL
                        }
                        else -> i++
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
                    // Ziggy ML strings end only by leaving block; treat one line as ML
                    colors.fill(ML_STRING, i, line.i1)
                    i = line.i1
                }
                else -> i++
            }
        }

        colors[line.i1] = state
        return state
    }

    override fun format(lines: List<Line>, options: AutoFormatOptions) =
        ZiggyFormatter.format(lines, options.indentation, options.lineBreakLength)

    override fun toString(): String = "Ziggy"
}
