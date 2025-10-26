package me.anno.remtext.colors.impl

import me.anno.remtext.colors.Colors
import me.anno.remtext.colors.Colors.BRACKET
import me.anno.remtext.colors.Colors.COMMENT
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.KEYWORD
import me.anno.remtext.colors.Colors.ML_COMMENT
import me.anno.remtext.colors.Colors.ML_STRING
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.SYMBOL
import me.anno.remtext.colors.Colors.VARIABLE
import me.anno.remtext.colors.Language
import me.anno.remtext.colors.Languages
import me.anno.remtext.colors.impl.XMLLanguage.Companion.packState
import me.anno.remtext.colors.impl.XMLLanguage.Companion.unpackLang
import me.anno.remtext.colors.impl.XMLLanguage.Companion.unpackLangState
import me.anno.remtext.font.Line

object MarkdownLanguage : Language {

    private const val TYPE = SYMBOL // good choice???
    private val languages = ArrayList<Language?>()

    // only 15 languages are supported, because we store the state in a byte
    private fun lookupLanguage(code: String): Int {
        val language = Languages.highlighters[code] ?: return 0
        var index = languages.indexOf(language)
        if (index < 0 && languages.size < 16) {
            index = languages.size
            languages.add(language)
        }
        return index + 1
    }

    // Helper to detect a closing fence safely (similar to detectEndScriptSafely in XMLLanguage).
    fun detectEndFenceSafely(line: Line, i0: Int, fence: String, langIndex: Int): Int {
        var p = i0
        while (true) {
            p = line.indexOf(fence, p, false)
            if (p < 0) return -1
            val colors = line.colors!!
            // Only accept fence if delegate didn't mark this position as STRING or ML_COMMENT
            if (colors[p] != ML_COMMENT) {
                // pack delegate colors for the range (so they remain associated with the lang)
                for (j in i0 until p) {
                    colors[j] = packState(langIndex, colors[j])
                }
                // mark fence itself (e.g., ``` ) as KEYWORD
                colors.fill(KEYWORD, p, p + fence.length)
                return p + fence.length
            }
            p += fence.length
        }
    }

    override fun highlight(line: Line, state0: Byte): Byte {
        var state = state0
        val colors = line.colors ?: return state0
        if (colors.isEmpty()) return state0

        val text = line.text
        var i = line.i0

        loop@ while (i < line.i1) {
            val langIndex = unpackLang(state)
            if (langIndex != 0) {
                // Delegate to the selected language for this line segment.
                val lang = languages[langIndex - 1]
                state = unpackLangState(state)
                state = lang?.highlight(line.subList(i, line.i1), state) ?: state
                val end = detectEndFenceSafely(line, i, "```", langIndex)
                if (end > i) {
                    i = end
                    // after closing fence we go back to markdown DEFAULT
                    state = DEFAULT
                    continue@loop
                } else {
                    // no closing fence this line — store packed state and finish
                    state = packState(langIndex, state)
                    // i = line.i1
                    break@loop
                }
            }

            // Normal Markdown processing
            when {
                // HTML comment
                line.startsWith("<!--", i) -> {
                    var end = line.indexOf("-->", i + 4)
                    end = if (end >= 0) end + 3 else line.i1
                    val todoIndex = line.indexOf("todo", i + 4, true)
                    if (todoIndex in i until end) {
                        colors.fill(COMMENT, i, todoIndex)
                        colors.fill(Colors.TODO, todoIndex, end)
                    } else {
                        colors.fill(COMMENT, i, end)
                    }
                    i = end
                }
                // Code fence opening: ```lang or ```
                line.startsWith("```", i) -> {
                    val fenceStart = i
                    i += 3
                    // read optional language id
                    while (i < line.i1 && text[i].isWhitespace()) i++
                    val langStart = i
                    while (i < line.i1 && !text[i].isWhitespace()) i++
                    val langId = text.substring(langStart, i).lowercase()
                    val mappedIndex = lookupLanguage(langId)
                    if (mappedIndex > 0) {
                        // Delegate to the language mapped to langId
                        // Mark the opening fence as KEYWORD and pack the state
                        colors.fill(KEYWORD, fenceStart, i)
                        state = packState(mappedIndex, DEFAULT)
                        // the inner content will be handled on subsequent lines (or rest of this line)
                        break@loop
                    } else {
                        // Unknown language: mark fence and just enter ML_STRING (no delegate)
                        colors.fill(KEYWORD, fenceStart, fenceStart + 3)
                        colors.fill(DEFAULT, fenceStart + 3, line.i1)
                        state = ML_STRING
                        break@loop
                    }
                }

                // Inline code
                text[i] == '`' -> {
                    val index = line.indexOf("`", i + 1)
                    val end = if (index >= 0) index + 1 else line.i1
                    colors.fill(STRING, i, end)
                    i = end
                }

                // Headings
                text[i] == '#' -> {
                    var j = i
                    while (j < line.i1 && text[j] == '#') j++
                    if (j < line.i1 && text[j].isWhitespace()) {
                        colors.fill(KEYWORD, i, line.i1)
                        i = line.i1
                    } else i++
                }

                // Blockquote
                text[i] == '>' && text.substring(line.i0, i).isBlank() -> {
                    colors.fill(SYMBOL, i, i + 1)
                    i++
                }

                // Lists and GitHub task lists
                text[i] in "+*-" && text.substring(line.i0, i).isBlank() -> {
                    if (i + 1 < line.i1 && text[i + 1].isWhitespace()) {
                        colors.fill(SYMBOL, i, i + 1)
                        i++
                        var j = i
                        while (j < line.i1 && text[j].isWhitespace()) j++
                        if (j + 2 < line.i1 && text[j] == '[' && text[j + 2] == ']') {
                            colors.fill(BRACKET, j, j + 3)
                            if (text[j + 1] == 'x' || text[j + 1] == 'X') colors.fill(KEYWORD, j + 1, j + 2)
                            i = j + 3
                        }
                    } else i++
                }
                // Ordered list numbers
                text[i].isDigit() -> {
                    var j = i + 1
                    while (j < line.i1 && text[j].isDigit()) j++
                    if (j < line.i1 && text[j] == '.' && (j + 1 < line.i1 && text[j + 1].isWhitespace())) {
                        colors.fill(SYMBOL, i, j + 1)
                        i = j + 1
                    } else {
                        colors[i++] = NUMBER
                    }
                }

                // Tables (|)
                text[i] == '|' -> colors[i++] = BRACKET

                // Horizontal rules
                line.startsWith("---", i) ||
                        line.startsWith("***", i) ||
                        line.startsWith("___", i) -> {
                    colors.fill(BRACKET, i, line.i1)
                    i = line.i1
                    continue@loop
                }

                // Links and images
                text[i] == '[' || (text[i] == '!' && i + 1 < line.i1 && text[i + 1] == '[') -> {
                    val start = i
                    val closeBracket = line.indexOf(']', i + 1, false)
                    if (closeBracket > 0 && closeBracket + 1 < line.i1 && text[closeBracket + 1] == '(') {
                        val closeParen = line.indexOf(')', closeBracket + 2, false)
                        if (closeParen > 0) {
                            colors.fill(VARIABLE, start, closeBracket + 1)
                            colors.fill(STRING, closeBracket + 1, closeParen + 1)
                            i = closeParen + 1
                            continue@loop
                        } else i++
                    } else i++
                }

                // Bold/italic
                text[i] in "*_" && i + 1 < line.i1 -> {
                    val ch = text[i]
                    val isDouble = i + 1 < line.i1 && text[i + 1] == ch
                    val marker = if (isDouble) 2 else 1
                    val endMarker = if (isDouble) "$ch$ch" else "$ch"
                    val end = line.indexOf(endMarker, i + marker, false)
                    if (end >= 0) {
                        colors.fill(if (isDouble) KEYWORD else STRING, i, end + marker)
                        i = end + marker
                    } else i++
                }
                // Inline HTML tags
                text[i] == '<' -> {
                    val close = line.indexOf('>', i + 1, false)
                    if (close > 0) {
                        colors.fill(TYPE, i, close + 1)
                        i = close + 1
                    } else i++
                }
                else -> i++
            }
        }

        // ML_STRING handling for unknown language fences: continue until closing ```
        if (state == ML_STRING) {
            val end = line.indexOf("```", line.i0, false)
            if (end >= 0) {
                colors.fill(ML_STRING, line.i0, end + 3)
                state = DEFAULT
            } else {
                colors.fill(ML_STRING, line.i0, line.i1)
                colors[line.i1] = ML_STRING
                return ML_STRING
            }
        }

        colors[line.i1] = state
        return state
    }

    override fun toString(): String = "Markdown"
}
