package me.anno.remtext.language

import me.anno.remtext.Colors.BRACKET
import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.Colors.KEYWORD
import me.anno.remtext.Colors.NUMBER
import me.anno.remtext.Colors.STRING
import me.anno.remtext.Colors.SYMBOL
import me.anno.remtext.Colors.VARIABLE
import me.anno.remtext.font.Line
import me.anno.remtext.formatters.AutoFormatOptions
import me.anno.remtext.language.CLikeLanguage.Companion.findEndOfNumber
import me.anno.remtext.language.CLikeLanguage.Companion.findEndOfString
import me.anno.remtext.language.CLikeLanguage.Companion.splitKeywords

/**
 * Syntax highlighter for RenPy (.rpy) files
 */
object RenPyLanguage : Language {

    private val python = CLikeLanguage(CLikeLanguageType.PYTHON)
    private val keywords = ("as,at,call,default,define,elif,else,for,from,hide,if," +
            "image,in,init,jump,label,menu,on,play,python,queue,return,scene," +
            "screen,show,stop,style,transform,while,with")
        .splitKeywords(false)

    private fun isLetter(c: Char) = c.isLetterOrDigit() || c == '_'

    override fun highlight(line: Line, state0: Byte): Byte {
        val text = line.text
        val colors = line.colors ?: return DEFAULT

        var i = line.i0
        while (i < line.i1) {
            when (text[i]) {
                '#' -> { // Comments
                    colors.fill(COMMENT, i, line.i1)
                    break
                }
                '"' -> { // Strings
                    val end = findEndOfString(line, i, '"')
                    colors.fill(STRING, i, end)
                    i = end
                }
                in '0'..'9' -> { // Numbers
                    val end = findEndOfNumber(line, i)
                    colors.fill(NUMBER, i, end)
                    i = end
                }
                '$' -> { // Python line ($ ...)
                    colors[i++] = KEYWORD
                    python.highlight(line.subLine(i, line.i1), DEFAULT)
                }

                // Brackets
                in "([{<" -> colors[i++] = BRACKET
                in ")]}>" -> colors[i++] = BRACKET

                // Operators / symbols
                in "=:+-*/%!&|.,?" -> colors[i++] = SYMBOL

                else -> {
                    // Identifiers / keywords / speaker names
                    if (isLetter(text[i])) {
                        val start = i++
                        while (i < line.i1 && isLetter(text[i])) i++
                        val word = text.substring(start, i)

                        when {
                            keywords[word.first()]?.contains(word) == true ->
                                colors.fill(KEYWORD, start, i)

                            // Speaker name before a string:  e "Hello"
                            i < line.i1 &&
                                    text.drop(i).trimStart().startsWith("\"") ->
                                colors.fill(VARIABLE, start, i)

                            else ->
                                colors.fill(VARIABLE, start, i)
                        }
                    } else i++
                }
            }
        }

        return DEFAULT
    }

    override fun format(lines: List<Line>, options: AutoFormatOptions): List<Line>? = null
    override fun toString(): String = "RenPy"
}
