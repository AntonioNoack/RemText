package me.anno.remtext.formatting

import me.anno.remtext.font.Line

/**
 * Taken & adjusted from Rem's Engine
 * */
object JsonFormatter {

    private class FormatHelper(
        capacityGuess: Int,
        val indentation: String,
        val lineBreakLength: Int
    ) {

        val result = ArrayList<Line>()
        val builder = StringBuilder(capacityGuess)

        val closingBracketStack = StringBuilder()
        val pretty = indentation.isNotEmpty()

        var shouldSwitchLine = false
        var depth = 0
        var lineStartIndex = 0
        var closingBracketsInLine = 0

        fun breakLine() {
            shouldSwitchLine = false

            result.add(Line(builder.toString()))
            builder.clear()

            repeat(depth) {
                builder.append(indentation)
            }
            lineStartIndex = builder.length
            closingBracketsInLine = 0
        }

        fun breakIfHadClosingBracket() {
            if (closingBracketStack.isNotEmpty()) {
                if (pretty) breakLine()
                builder.append(closingBracketStack)
                closingBracketsInLine = closingBracketStack.length
                closingBracketStack.clear()
            }
        }

        fun format(lines: List<Line>): List<Line> {

            for (li in lines.indices) {
                val line = lines[li]
                val str = line.text
                var i = line.i0
                while (i < line.i1) {
                    when (val char = str[i++]) {
                        '[', '{' -> {
                            if (pretty && builder.endsWith(',')) builder.append(' ')
                            builder.append(char)
                            depth++
                            // quicker ascent than directly switching lines
                            shouldSwitchLine = true
                        }
                        ']', '}' -> {
                            // quicker descent
                            if (closingBracketStack.length > 1) breakIfHadClosingBracket()
                            closingBracketStack.append(char)
                            depth--
                        }
                        ' ', '\t', '\r', '\n' -> {
                        } // skip, done automatically
                        ':' -> builder.append(if (pretty) ": " else ":")
                        '"', '\'' -> {
                            // skip a string
                            breakIfHadClosingBracket()
                            if (pretty) {
                                if (shouldSwitchLine || closingBracketsInLine > 1) breakLine()
                                else if (builder.endsWith(',')) builder.append(' ')
                            }
                            builder.append(char)
                            while (i < line.i1) {
                                when (val c2 = str[i++]) {
                                    char -> break
                                    // just skip the next '"', could throw an IndexOutOfBoundsException
                                    '\\' -> {
                                        builder.append(c2)
                                        builder.append(str[i++])
                                    }
                                    else -> builder.append(c2)
                                }
                            }
                            builder.append(char)
                        }
                        // todo support multi-line strings for JavaScript formatting
                        // todo support comments for JavaScript formatting
                        ',' -> {
                            breakIfHadClosingBracket()
                            builder.append(char)
                            if (builder.length - lineStartIndex > lineBreakLength) {
                                shouldSwitchLine = true
                            }
                        }
                        else -> {
                            breakIfHadClosingBracket()
                            if (pretty) {
                                if (shouldSwitchLine) breakLine()
                                else if (builder.endsWith(',')) builder.append(' ')
                            }
                            builder.append(char)
                        }
                    }
                }
            }

            breakIfHadClosingBracket()
            result.add(Line(builder.toString()))
            return result
        }
    }

    fun format(lines: List<Line>, indentation: String, lineBreakLength: Int) =
        FormatHelper(
            lines.sumOf { it.i1 - it.i0 } + (5 * indentation.length + 1) * lines.size,
            indentation, lineBreakLength
        ).format(lines)
}