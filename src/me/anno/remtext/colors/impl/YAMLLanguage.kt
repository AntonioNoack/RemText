package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors
import me.anno.remtext.colors.Colors.COMMENT
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.KEYWORD
import me.anno.remtext.colors.Colors.ML_STRING
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.SYMBOL
import me.anno.remtext.colors.Colors.VARIABLE
import me.anno.remtext.colors.Language
import me.anno.remtext.colors.impl.CLikeLanguage.Companion.findEndOfNumber
import me.anno.remtext.colors.impl.CLikeLanguage.Companion.findEndOfString
import me.anno.remtext.font.Line
import kotlin.math.max

object YAMLLanguage : Language {

    private fun isWordChar(c: Char) = c.isLetterOrDigit() || c in "_-"
    private val KEYWORDS = setOf("true", "false", "yes", "no", "on", "off", "null", "~")
    private const val TYPE = SYMBOL // good choice???

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = if (state0 == ML_STRING) state0 else DEFAULT
        val text = line.text
        val colors = line.colors ?: return state
        if (colors.isEmpty()) return state

        var i = line.i0

        // ChatGPT: If we're continuing a block scalar (ML_STRING), we'll consider any line
        // that is indented (starts with space or tab) as part of the scalar. When
        // a non-indented line is seen we end ML_STRING. This is a best-effort
        // heuristic — YAML block scalars depend on the indentation relative to
        // the parent, which would require preserving the scalar's indent across
        // lines. The simple heuristic below works well for common cases.
        if (state == ML_STRING) {
            if (i < line.i1 && (text[i] == ' ' || text[i] == '\t')) {
                // whole line is part of the block scalar
                colors.fill(ML_STRING, i, line.i1)
                colors[line.i1] = ML_STRING
                return ML_STRING
            } else {
                // scalar ended
                state = DEFAULT
            }
        }

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> {
                    // Comments
                    if (text[i] == '#') {
                        val todoIndex = line.indexOf("todo", i + 1, true)
                        if (todoIndex >= 0) {
                            colors.fill(COMMENT, i, todoIndex)
                            colors.fill(Colors.TODO, todoIndex, line.i1)
                        } else {
                            colors.fill(COMMENT, i, line.i1)
                        }
                        i = line.i1
                        continue@loop
                    }

                    // Document start/end markers
                    if (line.startsWith("---", i) || line.startsWith("...", i)) {
                        colors.fill(KEYWORD, i, i + 3)
                        i += 3
                        continue@loop
                    }

                    // Anchors & aliases (&name, *alias) and tags (!tag)
                    when (text[i]) {
                        '&', '*' -> {
                            // word following
                            val start = i++
                            while (i < line.i1 && isWordChar(text[i])) i++
                            colors.fill(VARIABLE, start, i)
                            continue@loop
                        }
                        '!' -> {
                            val start = i++
                            // tag can be complex — grab until whitespace or punctuation
                            while (i < line.i1 && !text[i].isWhitespace() && text[i] != ',' && text[i] != ':' && text[i] != '#') i++
                            colors.fill(TYPE, start, i) // use TYPE color for tags
                            continue@loop
                        }
                    }

                    // Quoted strings
                    if (text[i] == '"' || text[i] == '\'') {
                        val quote = text[i]
                        val end = findEndOfString(line, i, quote)
                        colors.fill(STRING, i, end)
                        i = max(i + 1, end)
                        continue@loop
                    }

                    // Block scalars (| and >): best-effort: mark rest of line and set ML_STRING
                    if ((text[i] == '|' || text[i] == '>') && (i == line.i0 || text.take(i).trimEnd().lastOrNull()
                            ?.let { it == ':' } == true || (i > 0 && text[i - 1] == ' '))
                    ) {
                        // mark the indicator and any following chomping/indent indicators
                        var j = i + 1
                        while (j < line.i1 && (text[j] == '+' || text[j] == '-' || text[j].isDigit())) j++
                        colors.fill(STRING, i, j)
                        i = j
                        // mark the rest of this line as STRING too (often there's a comment or indicator)
                        if (i < line.i1) colors.fill(STRING, i, line.i1)
                        // enter ML_STRING to continue on subsequent indented lines
                        state = ML_STRING
                        break@loop
                    }

                    // Detect keys: a bare word (or quoted) followed by optional spaces and a ':'
                    if (isWordChar(text[i]) || text[i] == '"' || text[i] == '\'') {
                        val tokenStart = i
                        if (text[i] == '"' || text[i] == '\'') {
                            i = findEndOfString(line, i, text[i])
                        } else {
                            while (i < line.i1 && isWordChar(text[i])) i++
                        }
                        // save token end, then skip spaces
                        val tokenEnd = i
                        var j = i
                        while (j < line.i1 && text[j].isWhitespace()) j++
                        if (j < line.i1 && text[j] == ':') {
                            // treat token as key
                            colors.fill(VARIABLE, tokenStart, tokenEnd)
                            // color the colon as SYMBOL
                            colors[j] = SYMBOL
                            i = j + 1
                            continue@loop
                        } else {
                            // Not a key — maybe a bare boolean/null/number
                            val token = text.substring(tokenStart, tokenEnd)
                            val lower = token.lowercase()
                            when {
                                token.isNotEmpty() && (token[0].isDigit() || token[0] == '+' || token[0] == '-') -> {
                                    val end = findEndOfNumber(line, tokenStart)
                                    colors.fill(NUMBER, tokenStart, end)
                                    i = max(i + 1, tokenEnd)
                                    continue@loop
                                }
                                lower in KEYWORDS -> {
                                    colors.fill(KEYWORD, tokenStart, tokenEnd)
                                    i = max(i + 1, tokenEnd)
                                    continue@loop
                                }
                                else -> {
                                    // leave uncolored for now
                                    i = max(i + 1, tokenEnd)
                                    continue@loop
                                }
                            }
                        }
                    }

                    // Flow collection punctuation and sequence dash
                    when (text[i]) {
                        '-', ':', ',', '[', ']', '{', '}', '?' -> {
                            colors[i++] = SYMBOL
                        }
                        '~' -> { // null
                            colors[i++] = KEYWORD
                        }
                        else -> i++
                    }
                }
                else -> i++
            }
        }

        colors[line.i1] = state
        return state
    }

    override fun toString(): String = "YAML"
}
