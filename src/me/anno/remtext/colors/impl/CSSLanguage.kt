package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors.BRACKET
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.ML_COMMENT
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.SYMBOL
import me.anno.remtext.colors.Colors.VARIABLE
import me.anno.remtext.colors.Language
import me.anno.remtext.colors.impl.CLikeLanguage.Companion.isHexChar
import me.anno.remtext.font.Line

object CSSLanguage : Language {

    private fun isIdentifierChar(c: Char) = c.isLetterOrDigit() || c in "_-"

    private fun findEndOfString(line: Line, start: Int, symbol: Char): Int {
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

    private fun findEndOfNumber(line: Line, start: Int): Int {
        val text = line.text
        var i = start
        if (text[i] in "+-") i++
        while (i < line.i1 && text[i].isDigit()) i++
        if (i < line.i1 && text[i] == '.') {
            i++
            while (i < line.i1 && text[i].isDigit()) i++
        }
        // Optional unit like px, em, %, but only directly after number
        while (i < line.i1 && (text[i].isLetter() || text[i] == '%')) i++
        return i
    }

    private fun findEndOfColor(line: Line, start: Int): Int {
        val text = line.text
        var i = start + 1
        while (i < line.i1 && isHexChar(text[i])) i++
        return i
    }

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = if (state0 == ML_COMMENT) state0 else DEFAULT
        val text = line.text
        val colors = line.colors ?: return state
        if (colors.isEmpty()) return state

        var i = line.i0

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> {
                    when {
                        // Comments
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
                        // Strings
                        text[i] == '"' || text[i] == '\'' -> {
                            val end = findEndOfString(line, i, text[i])
                            colors.fill(STRING, i, end)
                            i = end
                        }
                        // Color codes starting with #
                        text[i] == '#' && i + 1 < line.i1 && isHexChar(text[i + 1]) -> {

                            // not ideal...
                            val i0 = line.indexOf(';', i).let { if (it < 0) line.i1 else it }
                            val i1 = line.indexOf('{', i).let { if (it < 0) line.i1 else it }
                            val isColor = i0 < i1

                            val end = findEndOfColor(line, i)
                            colors.fill(if (isColor) NUMBER else VARIABLE, i, end)
                            i = end
                        }
                        // Numbers with optional units
                        (text[i] in '0'..'9' || text[i] in "+-") &&
                                (i == line.i0 || !isIdentifierChar(text[i - 1])) -> {
                            val end = findEndOfNumber(line, i)
                            colors.fill(NUMBER, i, end)
                            i = end
                        }
                        // Variables like var(--name)
                        line.startsWith("var(", i) -> {
                            val end = line.indexOf(')', i + 4)
                            if (end >= 0) {
                                colors.fill(VARIABLE, i, end + 1)
                                i = end + 1
                            } else {
                                colors.fill(VARIABLE, i, line.i1)
                                i = line.i1
                            }
                        }

                        // Symbols and brackets
                        text[i] in ";:," -> colors[i++] = SYMBOL
                        text[i] in "({[" -> colors[i++] = BRACKET
                        text[i] in ")}]" -> colors[i++] = BRACKET
                        else -> i++
                    }
                }
                ML_COMMENT -> {
                    val end = line.indexOf("*/", i)
                    if (end >= 0) {
                        colors.fill(ML_COMMENT, i, end + 2)
                        i = end + 2
                        state = DEFAULT
                    } else {
                        colors.fill(ML_COMMENT, i, line.i1)
                        i = line.i1
                    }
                }
                else -> i++
            }
        }

        colors[line.i1] = state
        return state
    }

    override fun toString(): String = "CSS"
}