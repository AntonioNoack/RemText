package me.anno.remtext.formatting

import me.anno.remtext.font.Line

/**
 * Mix from JsonFormatter and ChatChy generated
 * */
object CLikeFormatter {

    private class FormatHelper(
        capacityGuess: Int,
        val indentation: String,
        val lineBreakLength: Int
    ) {

        val result = ArrayList<Line>()
        val builder = StringBuilder(capacityGuess)

        var depth = 0
        var lineStartIndex = 0
        var shouldBreak = false
        val pretty = indentation.isNotEmpty()

        fun breakLine() {
            val text = builder.toString().trimEnd()
            if (text.isNotEmpty()) {
                result.add(Line(text))
            }
            builder.clear()
            repeat(depth) { builder.append(indentation) }
            lineStartIndex = builder.length
            shouldBreak = false
        }

        fun format(lines: List<Line>): List<Line> {
            if (lines.any { it.colors == null }) return lines

            for (li in lines.indices) {
                val line = lines[li]
                val text = line.text
                var i = line.i0
                while (i < line.i1) {
                    val ch = text[i++]

                    if (line.isComment(i - 1)) {
                        // Append the rest of the comment as-is
                        builder.append(ch)
                        while (i < line.i1 && line.isComment(i)) {
                            builder.append(text[i++])
                        }
                        continue
                    }

                    if (line.isString(i - 1)) {
                        // Append the full string literal as-is
                        builder.append(ch)
                        while (i < line.i1 && line.isString(i)) {
                            builder.append(text[i++])
                        }
                        continue
                    }

                    when (ch) {
                        '{' -> {
                            if (pretty) builder.append(' ')
                            builder.append(ch)
                            depth++
                        }
                        '}' -> {
                            depth--
                            if (depth < 0) depth = 0
                            breakLine()
                            builder.append(ch)
                            if (pretty) breakLine()
                        }
                        ';' -> {
                            builder.append(ch)
                            shouldBreak = true
                            if (pretty) breakLine()
                        }
                        '(' -> builder.append(ch)
                        ')' -> builder.append(ch)
                        ',' -> {
                            builder.append(ch)
                            if (builder.length - lineStartIndex > lineBreakLength) {
                                shouldBreak = true
                                if (pretty) breakLine()
                            } else if (pretty) builder.append(' ')
                        }
                        '\n', '\r' -> {
                            // ignore raw newlines, handled by formatter
                        }
                        else -> {
                            if (shouldBreak && !ch.isWhitespace()) {
                                breakLine()
                                shouldBreak = false
                            }
                            if (ch.isWhitespace()) {
                                if (!builder.isEmpty() && !builder.last().isWhitespace()) {
                                    builder.append(' ')
                                }
                            } else {
                                builder.append(ch)
                            }
                        }
                    }
                }

                // end of line
                if (!builder.isBlank() && !builder.endsWith(",")) {
                    breakLine()
                    shouldBreak = false
                }
            }

            if (builder.isNotEmpty()) {
                result.add(Line(builder.toString()))
            }
            return result
        }
    }

    fun format(lines: List<Line>, indentation: String, lineBreakLength: Int) =
        FormatHelper(
            lines.sumOf { it.i1 - it.i0 } + (5 * indentation.length + 1) * lines.size,
            indentation, lineBreakLength
        ).format(lines)
}