package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.ML_STRING
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.SYMBOL
import me.anno.remtext.colors.Language
import me.anno.remtext.font.Line

/**
 * CSV syntax highlighter
 * Highlights commas and semicolons as separators, and quoted strings.
 * Supports multiline quoted strings (ML_STRING) and simple numeric detection.
 * */
object CSVLanguage : Language {

    private fun findEndOfQuotedString(line: Line, start: Int): Int {
        val text = line.text
        var i = start + 1
        while (i < line.i1) {
            if (text[i] == '"') {
                // Double quote inside a quoted field means escaped quote
                if (i + 1 < line.i1 && text[i + 1] == '"') {
                    i += 2
                    continue
                }
                return i + 1
            }
            i++
        }
        return line.i1
    }

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = state0
        val colors = line.colors ?: return state0
        if (colors.isEmpty()) return state0

        val text = line.text
        var i = line.i0

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> when (text[i]) {
                    ',', ';', '|' -> colors[i++] = SYMBOL
                    '"' -> {
                        val end = findEndOfQuotedString(line, i)
                        colors.fill(STRING, i, end)
                        i = end
                        if (end >= line.i1) state = ML_STRING
                    }
                    in '0'..'9', '+', '-' -> {
                        var j = i + 1
                        while (j < line.i1 && (text[j].isDigit() || text[j] == '.' || text[j] == 'e' || text[j] == 'E' || text[j] == '+' || text[j] == '-')) j++
                        colors.fill(NUMBER, i, j)
                        i = j
                    }
                    else -> i++
                }
                ML_STRING -> {
                    val end = findEndOfQuotedString(line, i - 1)
                    colors.fill(STRING, i, end)
                    i = end
                    state = DEFAULT
                }
                else -> i++
            }
        }

        colors[line.i1] = state
        return state
    }

    override fun toString(): String = "CSV"
}