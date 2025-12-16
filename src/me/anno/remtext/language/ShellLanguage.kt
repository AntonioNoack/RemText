package me.anno.remtext.language

import me.anno.remtext.Colors
import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.Colors.NUMBER
import me.anno.remtext.Colors.STRING
import me.anno.remtext.Colors.VARIABLE
import me.anno.remtext.language.CLikeLanguage.Companion.readKeywords
import me.anno.remtext.language.CLikeLanguage.Companion.splitKeywords
import me.anno.remtext.font.Line

object ShellLanguage : Language {

    val keywords = ("if,then,else,elif,fi,for,while,do,done,case,esac,function,select,until,break,continue," +
            "return,in,export,readonly,local,declare,typeset,unset,trap,exit,eval")
        .splitKeywords(false)

    override fun highlight(line: Line, state0: Byte): Byte {

        val text = line.text
        val colors = line.colors ?: return DEFAULT
        if (colors.isEmpty()) return DEFAULT
        var i = line.i0

        while (i < line.i1) {
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
                    val end = CLikeLanguage.Companion.findEndOfString(line, i, text[i])
                    colors.fill(STRING, i, end)
                    i = end
                }
                text[i] == '`' -> {
                    val end = CLikeLanguage.Companion.findEndOfString(line, i, '`')
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
                    val end = CLikeLanguage.Companion.findEndOfNumber(line, i)
                    colors.fill(NUMBER, i, end)
                    i = end
                }
                else -> {
                    i = line.readKeywords(i, keywords[text[i]], false, i + 1)
                }
            }
        }

        val state = DEFAULT
        colors[line.i1] = state
        return state
    }

    override fun toString(): String = "Shell"
}