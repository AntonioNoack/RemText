package me.anno.remtext.language

import me.anno.remtext.Colors
import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.Colors.DOC_COMMENT
import me.anno.remtext.Colors.KEYWORD
import me.anno.remtext.Colors.ML_STRING
import me.anno.remtext.Colors.NUMBER
import me.anno.remtext.Colors.STRING
import me.anno.remtext.Colors.SYMBOL
import me.anno.remtext.Colors.VARIABLE
import me.anno.remtext.font.Line
import kotlin.math.max

object TOMLLanguage : Language {

    private fun isKeyChar(c: Char) =
        c.isLetterOrDigit() || c == '_' || c == '-'

    private val KEYWORDS = setOf(
        "true", "false"
    )

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = if (state0 == ML_STRING) state0 else DEFAULT
        val text = line.text
        val colors = line.colors ?: return state
        if (colors.isEmpty()) return state

        var i = line.i0

        // Continue multiline string """ or '''
        if (state == ML_STRING) {
            val end = when {
                line.startsWith("\"\"\"", i) -> i + 3
                line.startsWith("'''", i) -> i + 3
                else -> -1
            }
            colors.fill(ML_STRING, i, line.i1)
            if (end >= 0) {
                colors.fill(ML_STRING, i, end)
                colors[line.i1] = DEFAULT
                return DEFAULT
            }
            colors[line.i1] = ML_STRING
            return ML_STRING
        }

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> when (text[i]) {
                    '#' -> { // Comment
                        val todoIndex = line.indexOf("todo", i + 1, true)
                        if (todoIndex >= 0) {
                            colors.fill(COMMENT, i, todoIndex)
                            colors.fill(Colors.TODO, todoIndex, line.i1)
                        } else {
                            colors.fill(COMMENT, i, line.i1)
                        }
                        break@loop
                    }
                    '[' -> { // Tables [table] or [[array]]
                        val start = i
                        i++
                        if (i < line.i1 && text[i] == '[') i++
                        while (i < line.i1 && text[i] != ']') i++
                        if (i < line.i1) i++
                        if (i < line.i1 && text[i - 1] == ']' && text[i] == ']') i++
                        // idk...
                        colors.fill(DOC_COMMENT, start, i)
                    }
                    '"', '\'' -> { // Quoted strings
                        if (line.startsWith("\"\"\"", i) || line.startsWith("'''", i)) {  // Multiline strings
                            val start = i
                            i += 3
                            colors.fill(ML_STRING, start, line.i1)
                            state = ML_STRING
                            break@loop
                        } else {
                            val quote = text[i]
                            val end = CLikeLanguage.findEndOfString(line, i, quote)
                            colors.fill(STRING, i, end)
                            i = max(i + 1, end)
                        }
                    }
                    '=', ',', '.', ']', '{', '}' -> colors[i++] = SYMBOL
                    else -> {
                        if (isKeyChar(text[i])) { // Keys (bare or dotted)
                            val start = i
                            while (i < line.i1 && (isKeyChar(text[i]) || text[i] == '.')) i++
                            val keyEnd = i

                            var j = i
                            while (j < line.i1 && text[j].isWhitespace()) j++
                            if (j < line.i1 && text[j] == '=') {
                                colors.fill(VARIABLE, start, keyEnd)
                                colors[j] = SYMBOL
                                i = j + 1
                            } else {
                                val token = text.substring(start, keyEnd)
                                when {
                                    token.lowercase() in KEYWORDS -> {
                                        colors.fill(KEYWORD, start, keyEnd)
                                    }
                                    token[0].isDigit() || token[0] == '-' || token[0] == '+' -> {
                                        val end = CLikeLanguage.findEndOfNumber(line, start)
                                        colors.fill(NUMBER, start, end)
                                        i = end
                                        continue@loop
                                    }
                                }
                            }
                        } else if (text[i].isDigit() || text[i] == '-' || text[i] == '+') { // Numbers
                            val end = CLikeLanguage.findEndOfNumber(line, i)
                            colors.fill(NUMBER, i, end)
                            i = end
                            continue@loop
                        } else {
                            i++
                        }
                    }
                }
                else -> i++
            }
        }

        colors[line.i1] = state
        return state
    }

    override fun toString(): String = "TOML"
}
