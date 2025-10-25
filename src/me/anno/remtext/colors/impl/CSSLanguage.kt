package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.KEYWORD
import me.anno.remtext.colors.Colors.ML_COMMENT
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.SYMBOL
import me.anno.remtext.colors.Colors.VARIABLE
import me.anno.remtext.colors.Language
import me.anno.remtext.colors.impl.CLikeLanguage.Companion.findEndOfNumber
import me.anno.remtext.colors.impl.CLikeLanguage.Companion.findEndOfString
import me.anno.remtext.font.Line

object CSSLanguage : Language {

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = when (state0) {
            ML_COMMENT, STRING -> state0
            else -> DEFAULT
        }

        val text = line.text
        val colors = line.colors ?: return state
        if (colors.isEmpty()) return state

        var i = line.i0

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> {
                    when {
                        line.startsWith("/*", i) -> {
                            val end = line.indexOf("*/", i + 2, false)
                            if (end >= 0) {
                                colors.fill(ML_COMMENT, i, end + 2)
                                i = end + 2
                            } else {
                                colors.fill(ML_COMMENT, i, line.i1)
                                state = ML_COMMENT
                                i = line.i1
                            }
                        }

                        text[i] == '"' || text[i] == '\'' -> {
                            val end = findEndOfString(line, i, text[i])
                            colors.fill(STRING, i, end)
                            i = end
                        }

                        text[i] == '@' -> {
                            // At-rule like @media, @import
                            val start = i++
                            while (i < line.i1 && (text[i].isLetterOrDigit() || text[i] in "-_")) i++
                            colors.fill(KEYWORD, start, i)
                        }

                        text[i].isLetter() || text[i] in ".#&>[]" -> {
                            // Selector or property name
                            val start = i
                            while (i < line.i1 && (text[i].isLetterOrDigit() || text[i] in ".#&>[:_-")) i++
                            val word = text.substring(start, i)
                            if (i < line.i1 && line.startsWith(":", i)) {
                                // property name
                                colors.fill(KEYWORD, start, i)
                            } else {
                                // selector
                                colors.fill(VARIABLE, start, i)
                            }
                        }

                        text[i].isDigit() -> {
                            val end = findEndOfNumber(line, i)
                            // include unit (px, em, %, etc.)
                            var j = end
                            while (j < line.i1 && text[j].isLetter()) j++
                            colors.fill(NUMBER, i, j)
                            i = j
                        }

                        text[i] in "{}:;" -> {
                            colors[i++] = SYMBOL
                        }

                        text[i].isWhitespace() -> i++
                        else -> i++
                    }
                }
                ML_COMMENT -> {
                    val end = line.indexOf("*/", i, false)
                    if (end >= 0) {
                        colors.fill(ML_COMMENT, i, end + 2)
                        state = DEFAULT
                        i = end + 2
                    } else {
                        colors.fill(ML_COMMENT, i, line.i1)
                        i = line.i1
                    }
                }
                else -> i++
            }
        }

        return colors[line.i1 - 1]
    }

    override fun toString(): String = "CSS"
}