package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors.COMMENT
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.KEYWORD
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.VARIABLE
import me.anno.remtext.colors.Language
import me.anno.remtext.colors.impl.CLikeLanguage.Companion.findEndOfNumber
import me.anno.remtext.colors.impl.CLikeLanguage.Companion.findEndOfString
import me.anno.remtext.font.Line

object BatchLanguage : Language {

    val keywords = ("IF,ELSE,FOR,IN,DO,CALL,GOTO,EXIT,SET,PAUSE,REM,ECHO").split(',').groupBy { it[0] }

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

                // Variables %VAR%
                text[i] == '%' -> {
                    val start = i++
                    while (i < line.i1 && text[i] != '%') i++
                    if (i < line.i1) i++ // include closing %
                    colors.fill(VARIABLE, start, i)
                }

                // Strings (double quotes)
                text[i] == '"' -> {
                    val end = findEndOfString(line, i, '"')
                    colors.fill(STRING, i, end)
                    i = end
                }

                // Numbers
                text[i].isDigit() -> {
                    val end = findEndOfNumber(line, i)
                    colors.fill(NUMBER, i, end)
                    i = end
                }

                // Keywords (case-insensitive)
                text[i].isLetter() -> {
                    val start = i
                    while (i < line.i1 && text[i].isLetterOrDigit()) i++
                    val word = text.substring(start, i).uppercase()
                    if (keywords[word[0]]?.contains(word) == true) {
                        colors.fill(KEYWORD, start, i)
                    }
                }

                else -> i++
            }
        }

        return colors[line.i1 - 1]
    }

    override fun toString(): String = "Batch"
}