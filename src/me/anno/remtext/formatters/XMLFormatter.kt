package me.anno.remtext.formatters

import me.anno.remtext.font.Line
import kotlin.math.max
import kotlin.math.min

/**
 * ChatGPT-generated, and slightly adjusted
 * */
object XMLFormatter {

    private class FormatHelper(
        capacityGuess: Int,
        val indentation: String,
        val isHtml: Boolean
    ) {

        val result = ArrayList<Line>()
        val builder = StringBuilder(capacityGuess)
        val pretty = indentation.isNotEmpty()

        var depth = 0
        var lineStartIndex = 0

        // Inline elements that should *not* cause line breaks in HTML mode
        private val htmlInlineTags = setOf(
            "a", "abbr", "acronym", "b", "bdo", "big", "br", "button", "cite", "code", "dfn", "em",
            "i", "img", "input", "kbd", "label", "map", "mark", "object", "output", "q", "samp",
            "script", "select", "small", "span", "strong", "sub", "sup", "textarea", "time", "tt", "var"
        )

        // Void (self-closing) tags in HTML
        private val htmlVoidTags = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
            "param", "source", "track", "wbr"
        )

        fun breakLine() {
            if (!pretty) return
            if (builder.isNotEmpty()) result.add(Line(builder.toString()))
            builder.clear()
            repeat(depth) { builder.append(indentation) }
            lineStartIndex = builder.length
        }

        fun isInlineTag(tagName: String): Boolean {
            return isHtml && tagName.lowercase() in htmlInlineTags
        }

        fun isVoidTag(tagName: String): Boolean {
            return isHtml && tagName.lowercase() in htmlVoidTags
        }

        fun Int.ifNegative(size: Int): Int {
            return if (this >= 0) this else size
        }

        fun format(lines: List<Line>): List<Line> {
            for (li in lines.indices) {
                val line = lines[li]
                val str = line.text
                var i = line.i0
                while (i < line.i1) {
                    when {
                        str.startsWith("<!--", i) -> {
                            val end = line.indexOf("-->", i + 4).ifNegative(line.i1)
                            if (pretty) breakLine()
                            builder.append(str, i, min(end + 3, line.i1))
                            if (pretty) breakLine()
                            i = end + 3
                        }
                        str.startsWith("<![CDATA[", i) -> {
                            val end = line.indexOf("]]>", i + 9).ifNegative(line.i1)
                            if (pretty) breakLine()
                            builder.append(str, i, min(end + 3, line.i1))
                            if (pretty) breakLine()
                            i = end + 3
                        }
                        str.startsWith("<?", i) -> {
                            // --- Processing instruction ---
                            val end = line.indexOf("?>", i + 2).ifNegative(line.i1)
                            if (pretty) breakLine()
                            builder.append(str, i, min(end + 2, line.i1))
                            if (pretty) breakLine()
                            i = end + 2
                        }
                        str[i] == '<' -> {
                            val closeIdx = line.indexOf('>', i + 1)
                            val close = closeIdx.ifNegative(line.i1)
                            val tagContent = str.substring(i + 1, close).trim()
                            val tagName = tagContent
                                .trim('/')
                                .takeWhile { it.isLetterOrDigit() || it in ":_-" }
                                .lowercase()

                            val isClosed = closeIdx >= 0

                            when {
                                tagContent.startsWith("/") -> {
                                    // Closing tag
                                    depth = max(depth - 1, 0)
                                    if (!isInlineTag(tagName) && pretty) breakLine()
                                    builder.append('<').append(tagContent)
                                    if (isClosed) builder.append('>')
                                    if (!isInlineTag(tagName) && pretty) breakLine()
                                }
                                tagContent.endsWith("/") || isVoidTag(tagName) -> {
                                    // Self-closing or void tag
                                    if (!isInlineTag(tagName) && pretty) breakLine()
                                    builder.append('<').append(tagContent)
                                    if (isClosed) builder.append('>')
                                    if (!isInlineTag(tagName) && pretty) breakLine()
                                }
                                tagContent.startsWith("!") -> {
                                    // Doctype
                                    if (pretty) breakLine()
                                    builder.append('<').append(tagContent)
                                    if (isClosed) builder.append('>')
                                    if (pretty) breakLine()
                                }
                                else -> {
                                    // Opening tag
                                    if (!isInlineTag(tagName) && pretty) breakLine()
                                    builder.append('<').append(tagContent)
                                    if (isClosed) builder.append('>')
                                    if (!isInlineTag(tagName)) depth++
                                }
                            }
                            i = close + 1
                        }

                        // --- Text content ---
                        else -> {
                            val nextTag = line.indexOf('<', i).ifNegative(line.i1)
                            val textContent = str.substring(i, nextTag)
                            if (textContent.isNotBlank()) {
                                val trimmed =
                                    if (isHtml) textContent.replace(Regex("\\s+"), " ")
                                    else textContent.trim()
                                builder.append(trimmed)
                            }
                            i = nextTag
                        }
                    }
                }
            }

            if (builder.isNotEmpty() || result.isEmpty()) {
                result.add(Line(builder.toString()))
            }
            return result
        }
    }

    fun format(
        lines: List<Line>,
        indentation: String,
        isHtml: Boolean
    ): List<Line> {
        val totalLength = lines.sumOf { it.i1 - it.i0 } + (5 * indentation.length + 1) * lines.size
        return FormatHelper(totalLength, indentation, isHtml).format(lines)
    }
}