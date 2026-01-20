package me.anno.remtext.blocks

import me.anno.remtext.font.Line
import kotlin.math.min

enum class BlockStyle {
    BRACKETS,
    INDENTATION,
    MARKDOWN,
    TOML,
    XML;

    fun findEndOfCollapse(lines: List<Line>, lineIndex: Int): Int {
        val endIndex = findEndOfCollapseImpl(lines, lineIndex)
        return min(endIndex, lines.lastIndex)
    }

    fun findEndOfCollapseImpl(lines: List<Line>, lineIndex: Int): Int {
        var i = lineIndex
        when (this) {
            XML -> {
                var depth = lines[i++].depth
                while (depth > 0 && i < lines.size) {
                    depth += lines[i++].depth
                }
                return i - 1
            }
            BRACKETS -> {
                var depth = lines[i++].depth
                while (depth > 0 && i < lines.size) {
                    depth += lines[i++].depth
                }
                return i - 1
            }
            INDENTATION -> {
                val depth = lines[i++].depth
                if (depth < 0) return -1
                while (i < lines.size) {
                    if (lines[i++].depth in 0..depth) return i - 2
                }
                return lines.size
            }
            MARKDOWN -> {
                val depth = lines[i++].depth
                if (depth == 0) return -1
                while (i < lines.size) {
                    val depthI = lines[i++].depth
                    if (depthI in 1..depth) return i - 2 // found end
                }
                return lines.size
            }
            TOML -> {
                if (lines[i++].depth == 0) return -1
                while (i < lines.size) {
                    if (lines[i++].depth > 0) return i - 2
                }
                return lines.size
            }
        }
    }

    companion object {
        fun BlockStyle?.canCollapseBlock(lines: List<Line>, lineIndex: Int): Boolean {
            val line = lines[lineIndex]
            return when (this) {
                null -> false
                // todo HTML is special in that some types may not need a closing tag...
                INDENTATION -> {
                    val depth = line.depth
                    if (depth < 0) return false

                    // skip empty lines
                    var nextDepth = -1
                    var lineIndex = lineIndex + 1
                    while (nextDepth < 0 && lineIndex < lines.size) {
                        nextDepth = lines[lineIndex++].depth
                    }
                    nextDepth > depth
                }
                XML, BRACKETS, MARKDOWN, TOML -> line.depth > 0
            }
        }

        fun BlockStyle?.calculateDepth(line: Line): Int {
            return when (this) {
                null -> -1
                // todo HTML is special in that some types may not need a closing tag...
                XML -> line.countXMLDelta()
                INDENTATION -> line.countIndentation()
                BRACKETS -> line.countBracketDeltaDepth()
                MARKDOWN -> line.countMarkdownDepth()
                TOML -> if (line.startsWith("[")) 1 else 0
            }
        }
    }

}