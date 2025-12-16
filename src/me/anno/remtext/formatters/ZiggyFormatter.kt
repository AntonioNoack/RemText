package me.anno.remtext.formatters

import me.anno.remtext.Colors.BRACKET
import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.Colors.KEYWORD
import me.anno.remtext.Colors.ML_COMMENT
import me.anno.remtext.Colors.ML_STRING
import me.anno.remtext.Colors.NUMBER
import me.anno.remtext.Colors.STRING
import me.anno.remtext.Colors.SYMBOL
import me.anno.remtext.Colors.VARIABLE
import me.anno.remtext.font.Line
import kotlin.math.max

object ZiggyFormatter {

    fun format(lines: List<Line>, indentation: String, lineBreakLength: Int): List<Line> {
        val newLines = ArrayList<Line>()
        var depth = 0

        val currLine = StringBuilder()
        var atLineStart = true

        fun removeDoubleWhitespace() {
            while (currLine.length >= 2 &&
                currLine[currLine.length - 1].isWhitespace() &&
                currLine[currLine.length - 2].isWhitespace()
            ) {
                currLine.setLength(currLine.length - 1)
            }
        }

        fun indentNow() {
            repeat(depth) { currLine.append(indentation) }
            atLineStart = false
        }

        fun flush() {
            currLine.trimEnd()
            val cleaned = currLine.toString()
            atLineStart = true
            if (cleaned.isNotBlank()) {
                newLines.add(Line(cleaned))
            }
            currLine.clear()
        }

        fun flushKeepEmpty() {
            // used only when formatting multiline comments
            val cleaned = currLine.toString().trimEnd()
            currLine.clear()
            atLineStart = true
            newLines.add(Line(cleaned))
        }

        for (line in lines) {
            val text = line.text
            val colors = line.colors ?: continue

            var i = line.i0
            val end = line.i1

            while (i < end) {
                val c = text[i]
                when (val color = colors[i]) {
                    STRING, ML_STRING -> {
                        if (atLineStart) indentNow()
                        val start = i
                        var j = i + 1
                        while (j < end && colors[j] == color) j++
                        currLine.append(text, start, j)
                        i = j
                    }
                    COMMENT -> {
                        // SINGLE-LINE COMMENT: exactly one flush before, no trailing blank line
                        flush()
                        indentNow()
                        currLine.append(text.substring(i, end))
                        flush() // produce comment line
                        i = end
                    }
                    ML_COMMENT -> {
                        // MULTILINE COMMENT: break and keep each line
                        flush()
                        indentNow()
                        currLine.append(text.substring(i, end))
                        flushKeepEmpty() // intentionally keeps blank lines
                        i = end
                    }
                    BRACKET -> {
                        when (c) {
                            '{' -> {
                                if (atLineStart) indentNow()
                                currLine.append('{')
                                flush()
                                depth++
                            }
                            '}' -> {
                                flush()
                                depth = max(0, depth - 1)
                                indentNow()
                                currLine.append('}')
                            }
                            else -> {
                                if (atLineStart) indentNow()
                                currLine.append(c)
                            }
                        }
                        i++
                    }
                    SYMBOL -> {
                        when (c) {
                            ',' -> {
                                currLine.append(',')
                                i++
                                flush()
                            }
                            '=' -> {
                                // remove whitespace around '=' BEFORE formatting
                                removeDoubleWhitespace()
                                currLine.append('=')
                                i++
                            }
                            else -> {
                                if (atLineStart) indentNow()
                                currLine.append(c)
                                i++
                            }
                        }
                    }
                    VARIABLE, KEYWORD, NUMBER, DEFAULT -> {
                        if (atLineStart) indentNow()
                        else removeDoubleWhitespace()
                        currLine.append(c)
                        i++
                    }
                    else -> i++
                }
            }

            flush()
        }

        return newLines
    }
}
