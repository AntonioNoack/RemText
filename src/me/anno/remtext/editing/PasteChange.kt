package me.anno.remtext.editing

import me.anno.remtext.Rendering.cursors
import me.anno.remtext.Rendering.file
import kotlin.math.min

class PasteChange(private val added: List<String>) : Change {

    private val oldCursors: List<CursorPair> = ArrayList(cursors)
    private val removed = oldCursors.map { pair ->
        Editing.getStringFromRange(pair.min, pair.max)
    }

    lateinit var afterPasteCursors: ArrayList<CursorPair>

    override fun redo() {
        // always copy list to not lose original cursor state
        val newCursors = apply(added, ArrayList(oldCursors), true)
        afterPasteCursors = newCursors
        setCursors(newCursors)
    }

    override fun undo() {
        apply(removed, afterPasteCursors, false)
        // newCursors would be the same as oldCursors, just with lost length info
        setCursors(oldCursors)
    }

    private fun setCursors(newCursors: List<CursorPair>) {
        cursors.clear()
        cursors.addAll(newCursors.map {
            CursorPair(it.second, it.second)
        })
    }

    private fun apply(
        added: List<String>,
        oldCursors: ArrayList<CursorPair>,
        needsNewCursors: Boolean
    ): ArrayList<CursorPair> {

        val newCursors = ArrayList<CursorPair>()
        // apply paste operations, and find new cursors
        for (ci in oldCursors.indices) {
            val cursor = oldCursors[ci]
            val added = added.getOrNull(ci) ?: added[0]

            Editing.deleteRangeImpl(cursor.min, cursor.max)
            val newCursor1 = Editing.pasteImpl(added, cursor.min)


            // we must also apply the changes to the remaining oldCursors
            for (ki in ci + 1 until oldCursors.size) {
                oldCursors[ki] = replace(oldCursors[ki], cursor, newCursor1)
            }

            if (needsNewCursors) {
                // adjust all current cursors...
                for (ki in newCursors.indices) {
                    newCursors[ki] = replace(newCursors[ki], cursor, newCursor1)
                }
                newCursors.add(CursorPair(cursor.min, newCursor1))
            }
        }

        Editing.onChange()
        return newCursors
    }

    fun replace(
        cursor: CursorPair,
        prev: CursorPair,
        currMax: Cursor
    ): CursorPair {
        val (min, max) = cursor
        val (pmin, pmax) = prev
        if (max.lineIndex < pmin.lineIndex) return cursor // unchanged
        if (min.lineIndex > pmax.lineIndex) {
            if (currMax.lineIndex == pmax.lineIndex) return cursor
        }

        return CursorPair(
            replace(min, prev, currMax),
            replace(max, prev, currMax)
        )
    }

    private fun replace(
        cursor: Cursor,
        prev: CursorPair,
        currMax: Cursor
    ): Cursor {

        val (pmin, pmax) = prev
        if (cursor <= pmin) return cursor
        if (cursor == pmax) return currMax

        if (cursor.lineIndex > pmax.lineIndex) {
            val deltaLines = currMax.lineIndex - pmax.lineIndex
            return Cursor(cursor.lineIndex + deltaLines, cursor.relI)
        }

        // we are within the selection lines...
        if (cursor >= pmax) {
            val deltaLineLength = currMax.relI - pmax.relI
            return Cursor(currMax.lineIndex, cursor.relI + deltaLineLength)
        }

        // we are within the selection
        return when (cursor.lineIndex) {
            pmin.lineIndex -> {
                val line = file.lines[cursor.lineIndex]
                val maxAllowedRelI = if (pmax.lineIndex == pmin.lineIndex) {
                    // clamp within pasted section
                    min(currMax.relI, line.length)
                } else {
                    // clamp until end
                    line.length
                }
                val newI = clamp(cursor.relI, 0, maxAllowedRelI)
                Cursor(cursor.lineIndex, newI)
            }
            pmax.lineIndex -> {
                // clamp from the start
                val deltaLineLength = currMax.relI - pmax.relI
                val newI = clamp(cursor.relI + deltaLineLength, 0, currMax.relI)
                Cursor(currMax.lineIndex, newI)
            }
            else -> {
                val newLineIndex = clamp(cursor.lineIndex, pmin.lineIndex, currMax.lineIndex)
                val line = file.lines[newLineIndex]
                Cursor(newLineIndex, clamp(cursor.relI, 0, line.length))
            }
        }
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
        return if (x < min) min else if (x < max) x else max
    }

}