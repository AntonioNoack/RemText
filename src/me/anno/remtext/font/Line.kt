package me.anno.remtext.font

import kotlin.math.max
import kotlin.math.min

class Line(
    val text: String, val i0: Int, val i1: Int,
    private val offsets: IntArray
) {

    constructor(text: String) : this(text, 0, text.length, IntArray(text.length + 1)) {
        fillOffsets(text, i0, i1, offsets)
    }

    init {
        if (i0 !in 0..i1 || i1 > text.length) throw IllegalArgumentException()
        if (offsets.size <= i1) throw IllegalArgumentException()
    }

    companion object {
        fun fillOffsets(text: String, i0: Int, i1: Int, offsets: IntArray) {
            if (i0 >= offsets.size) return
            var x = offsets[i0]
            for (i in i0 + 1..i1) {
                val curr = text[i - 1]
                val next = if (i < i1) text[i] else ' '
                x += Font.getOffset(curr, next)
                offsets[i] = x
            }
        }

        fun validateOffsets(text: String, i0: Int, i1: Int, offsets: IntArray) {
            if (i0 >= offsets.size) return
            var x = offsets[i0]
            for (i in i0 + 1..i1) {
                val curr = text[i - 1]
                val next = if (i < i1) text[i] else ' '
                x += Font.getOffset(curr, next)
                if (offsets[i] != x) throw IllegalArgumentException("Mismatch! $i:$x vs ${offsets[i]}")
            }
        }
    }

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
        val joinedOffset = copyOfRange(offsets, j0, j1 + 1, joinedText.length + 1)
        val sizeJ = j1 - j0
        val sizeJM = sizeJ + middle.length
        // -1 and +1 are overlaps to correct the transitions from characters to their next
        val fillI0 = max(sizeJ - 1, j0 - i0)
        val fillI1 = min(sizeJM + 1, joinedText.length)
        fillOffsets(joinedText, fillI0, fillI1, joinedOffset)

        // println("j0,j1: $j0,$j1, k0,k1: $k0,$k1, sizeJ: $sizeJ, sizeJM: $sizeJM, joined: ${joinedText.length}")
        // println("Join ranges ${j1 - j0} + ${middle.length} + ${k1 - k0}, fill: $fillI0-$fillI1")

        val delta = joinedOffset[sizeJM] - other.offsets[k0]
        for (i in 0..k1 - k0) {
            joinedOffset[i + sizeJM] = other.offsets[k0 + i] + delta
        }
        validateOffsets(joinedText, 0, joinedText.length, joinedOffset)
        return Line(joinedText, 0, joinedText.length, joinedOffset)
    }

    var countedLinesAtW = 0
    var countedLinesW = 0

    fun subList(i0: Int, i1: Int) = Line(text, i0, i1, offsets)

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
}