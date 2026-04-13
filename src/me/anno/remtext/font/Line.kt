package me.anno.remtext.font

import me.anno.remtext.Colors
import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.ML_COMMENT
import me.anno.remtext.Colors.ML_STRING
import me.anno.remtext.Colors.ML_STRING2
import me.anno.remtext.Colors.STRING
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

class Line(
    val text: String, val i0: Int, val i1: Int,
    private val offsets: IntArray,
    val colors: ByteArray?
) {

    companion object {
        fun fillOffsets(text: String, i0: Int, i1: Int, offsets: IntArray) {
            if (i0 >= offsets.size) return
            var x = offsets[i0]
            var i = i0
            while (i < i1) {
                val match = Emojis.findMatch(text, i, i1)
                if (match != null) {
                    x += Font.emojiWidth
                    for (j in 1..match.length) {
                        offsets[i + j] = x
                    }
                    i += match.length
                } else {
                    val next = if (i + 1 < i1) text[i + 1] else ' '
                    x += Font.getOffset(text[i], next)
                    offsets[i + 1] = x
                    i++
                }
            }
        }

        fun validateOffsets(text: String, i0: Int, i1: Int, offsets: IntArray) {
            if (i0 >= offsets.size) return
            var x = offsets[i0]
            var i = i0
            while (i < i1) {
                val match = Emojis.findMatch(text, i, i1)
                if (match != null) {
                    x += Font.emojiWidth
                    for (j in 1..match.length) {
                        val k = i + j
                        if (offsets[k] != x) throw IllegalArgumentException("Mismatch! $k:$x vs ${offsets[k]}")
                    }
                    i += match.length
                } else {
                    val next = if (i + 1 < i1) text[i + 1] else ' '
                    x += Font.getOffset(text[i], next)
                    val k = i + 1
                    if (offsets[k] != x) throw IllegalArgumentException("Mismatch! $k:$x vs ${offsets[k]}")
                    i++
                }
            }
        }

        val COLLAPSE_LIMIT = 1 shl 20
    }

    constructor(text: String) : this(
        text, 0, text.length,
        IntArray(text.length + 1),
        ByteArray(text.length + 1)
    ) {
        fillOffsets(text, i0, i1, offsets)
    }

    init {
        if (i0 !in 0..i1 || i1 > text.length) throw IllegalArgumentException()
        if (offsets.size <= i1) throw IllegalArgumentException()
    }

    val length: Int get() = i1 - i0
    var depth = -1

    fun getOffset(i: Int): Int = offsets[i] - offsets[i0]
    fun getWidth(i: Int): Int = offsets[i + 1] - offsets[i]

    private fun copyOfRange(base: IntArray, i0: Int, i1: Int, size: Int): IntArray {
        val values = IntArray(size)
        base.copyInto(values, 0, i0, i1)
        return values
    }

    fun joinRanges(j0: Int, j1: Int, middle: String, k0: Int, k1: Int, other: Line): Line {
        if (j0 < i0) throw IllegalArgumentException()
        if (j1 !in j0..i1) throw IllegalArgumentException()
        if (k0 < other.i0) throw IllegalArgumentException()
        if (k1 !in k0..other.i1) throw IllegalArgumentException()

        val joinedText = text.substring(j0, j1) + middle + other.text.substring(k0, k1)
        val joinedOffset = IntArray(joinedText.length + 1)
        fillOffsets(joinedText, 0, joinedText.length, joinedOffset)
        validateOffsets(joinedText, 0, joinedText.length, joinedOffset)
        return Line(
            joinedText, 0, joinedText.length, joinedOffset,
            ByteArray(joinedText.length + 1)
        )
    }

    private var countedLinesAtW = 0
    private var countedLinesW = 0

    fun recalculateOffsets() {
        countedLinesW = 0
        fillOffsets(text, i0, i1, offsets)
    }

    fun subLine(i0: Int, i1: Int) = Line(text, i0, i1, offsets, colors)

    fun getNumLines(width: Int): Int {
        if (countedLinesW == width) {
            return countedLinesAtW
        } else {
            val numLines = countLines(width)
            countedLinesW = width
            countedLinesAtW = numLines
            return numLines
        }
    }

    private fun countLines(width: Int): Int {
        var dxi = 0
        val lineNumberOffset = 0
        var numLines = 1
        for (i in i0 until i1) {
            val curr = text[i]
            if (curr == ' ') continue

            val texWidth = getWidth(i)
            val x = getOffset(i) + lineNumberOffset
            if (x > dxi && x - dxi + texWidth > width) {
                dxi = x - lineNumberOffset
                numLines++
            }
        }
        return numLines
    }

    fun indexOf(part: Char, i0: Int, ignoreCase: Boolean = false): Int {
        for (i in max(i0, this.i0) until i1) {
            if (text[i].equals(part, ignoreCase)) return i
        }
        return -1
    }

    fun indexOf(part: String, i0: Int, ignoreCase: Boolean = false): Int {
        for (i in max(i0, this.i0)..i1 - part.length) {
            if (text.startsWith(part, i, ignoreCase)) return i
        }
        return -1
    }

    fun startsWith(prefix: String, i0: Int = this.i0, ignoreCase: Boolean = false): Boolean {
        return i0 >= this.i0 && i0 + prefix.length <= i1 && text.startsWith(prefix, i0, ignoreCase)
    }

    fun isString(i: Int): Boolean {
        if (i !in i0..<i1) return false
        val colors = colors ?: return false
        val color = colors[i] and Colors.COLOR_MASK.toByte()
        return when (color) {
            STRING,
            ML_STRING,
            ML_STRING2 -> true
            else -> false
        }
    }

    fun isComment(i: Int): Boolean {
        if (i !in i0..<i1) return false
        val colors = colors ?: return false
        val color = colors[i] and Colors.COLOR_MASK.toByte()
        return when (color) {
            COMMENT,
            ML_COMMENT,
            Colors.TODO -> true
            else -> false
        }
    }

    fun countIndentation(): Int {
        for (depth in 0 until min(i1 - i0, COLLAPSE_LIMIT)) {
            if (!text[i0 + depth].isWhitespace()) return depth
        }
        return -1
    }

    fun countBracketDeltaDepth(): Int {
        var delta = 0
        val i1 = min(i0 + COLLAPSE_LIMIT, i1)
        for (i in i0 until i1) {
            if (isComment(i) || isString(i)) continue
            when (text[i]) {
                '(', '[', '{' -> delta++
                ')', ']', '}' -> delta--
            }
        }
        return delta
    }

    fun countXMLDelta(): Int {
        var delta = 0
        val i1 = min(i0 + COLLAPSE_LIMIT, i1 - 1)
        for (i in i0 until i1) {
            if (isComment(i) || isString(i)) continue
            if (text[i] == '<' && text[i + 1] != '/') delta++
            else if (text[i] == '<') delta--
            else if (text[i] == '/' && text[i + 1] == '>') delta--
        }
        return delta
    }

    fun countMarkdownDepth(): Int {
        // todo for code blocks, this depends on the language
        val i1 = min(i0 + COLLAPSE_LIMIT, i1)
        for (i in i0 until i1) {
            if (isComment(i) || isString(i)) return -1
            if (text[i] != '#') return i - i0
        }
        return -1
    }

}