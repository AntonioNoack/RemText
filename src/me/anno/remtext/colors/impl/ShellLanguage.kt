package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors
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

object ShellLanguage : Language {

    val keywords = ("if,then,else,elif,fi,for,while,do,done,case,esac,function,select,until,break,continue," +
            "return,in,export,readonly,local,declare,typeset,unset,trap,exit,eval")
        .split(',').groupBy { it[0] }

    override fun highlight(line: Line, state0: Byte): Byte {

        val text = line.text
        val colors = line.colors ?: return DEFAULT
        if (colors.isEmpty()) return DEFAULT
        var i = line.i0

        loop@ while (i < line.i1) {
            when {
                text[i] == '#' -> {
                    // Single-line comment
                    val todoIndex = line.indexOf("TODO", i + 1, true)
                    val end = line.i1
                    if (todoIndex >= 0) {
                        colors.fill(COMMENT, i, todoIndex)
                        colors.fill(Colors.TODO, todoIndex, end)
                    } else {
                        colors.fill(COMMENT, i, end)
                    }
                    i = end
                }
                text[i] == '\'' || text[i] == '"' -> {
                    val end = findEndOfString(line, i, text[i])
                    colors.fill(STRING, i, end)
                    i = end
                }
                text[i] == '`' -> {
                    val end = findEndOfString(line, i, '`')
                    colors.fill(STRING, i, end)
                    i = end
                }
                line.startsWith("$(", i) -> {
                    val end = line.indexOf(")", i + 2).let { if (it < 0) line.i1 else it + 1 }
                    colors.fill(STRING, i, end)
                    i = end
                }
                text[i] == '$' && i + 1 < line.i1 -> {
                    val start = i++
                    if (text[i] == '{') {
                        i++
                        while (i < line.i1 && text[i] != '}') i++
                        if (i < line.i1) i++
                    } else {
                        while (i < line.i1 && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    }
                    colors.fill(VARIABLE, start, i)
                }
                text[i] in '0'..'9' -> {
                    val end = findEndOfNumber(line, i)
                    colors.fill(NUMBER, i, end)
                    i = end
                }
                text[i].isLetter() -> {
                    val start = i
                    while (i < line.i1 && text[i].isLetterOrDigit()) i++
                    val word = text.substring(start, i)
                    if (keywords[word[0]]?.contains(word) == true) {
                        colors.fill(KEYWORD, start, i)
                    }
                }
                else -> i++
            }
        }
        return colors[line.i1 - 1]
    }

    override fun toString(): String = "Shell"
}