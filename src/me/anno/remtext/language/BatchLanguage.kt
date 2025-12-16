package me.anno.remtext.language

import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.Colors.NUMBER
import me.anno.remtext.Colors.STRING
import me.anno.remtext.Colors.VARIABLE
import me.anno.remtext.language.CLikeLanguage.Companion.readKeywords
import me.anno.remtext.language.CLikeLanguage.Companion.splitKeywords
import me.anno.remtext.font.Line

object BatchLanguage : Language {

    val keywords = "if,else,for,in,do,call,goto,exit,set,pause,rem,echo".splitKeywords(true)

    override fun highlight(line: Line, state0: Byte): Byte {
        val text = line.text
        val colors = line.colors ?: return DEFAULT
        if (colors.isEmpty()) return DEFAULT
        var i = line.i0

        while (i < line.i1) {
            when {
                // Comment lines: REM or ::
                line.startsWith("REM", i, true) || line.startsWith("::", i) -> {
                    colors.fill(COMMENT, i, line.i1)
                    break
                }
                text[i] == '%' -> { // Variables %VAR%
                    val start = i++
                    while (i < line.i1 && text[i] != '%') i++
                    if (i < line.i1) i++ // include closing %
                    colors.fill(VARIABLE, start, i)
                }
                text[i] == '"' -> { // Strings (double quotes)
                    val end = CLikeLanguage.Companion.findEndOfString(line, i, '"')
                    colors.fill(STRING, i, end)
                    i = end
                }
                text[i].isDigit() -> { // Numbers
                    val end = CLikeLanguage.Companion.findEndOfNumber(line, i)
                    colors.fill(NUMBER, i, end)
                    i = end
                }
                else -> {
                    // Keywords (case-insensitive)
                    i = line.readKeywords(i, keywords[text[i]], true, i + 1)
                }
            }
        }

        val state = DEFAULT
        colors[line.i1] = state
        return state
    }

    override fun toString(): String = "Batch"
}