package me.anno.remtext.editing

import me.anno.remtext.Rendering.cursor0
import me.anno.remtext.Rendering.cursor1
import me.anno.remtext.Rendering.maxI
import me.anno.remtext.Rendering.minI

class Change(val added: String) {

    // required for redo
    var minCursor = minI(cursor0, cursor1)
    var maxCursor = maxI(cursor0, cursor1)
    val minIs0 = minCursor == cursor0

    // required for undo
    val removed = Editing.getStringFromRange(minCursor, maxCursor)
    lateinit var newCursor0I: Cursor
    lateinit var newCursor1I: Cursor

    fun redo() {
        val newCursor0 = Editing.deleteRangeImpl(minCursor, maxCursor)
        val (newCursorI0, newCursorI1) = Editing.pasteImpl(added, newCursor0)
        this.newCursor0I = newCursorI0
        this.newCursor1I = newCursorI1
        cursor0 = newCursorI1
        cursor1 = newCursorI1
        Editing.onChange()
    }

    fun undo() {
        val undoCursor0 = Editing.deleteRangeImpl(newCursor0I, newCursor1I)
        val (minCursorI, maxCursorI) = Editing.pasteImpl(removed, undoCursor0)

        // for redo after undo
        minCursor = minCursorI
        maxCursor = maxCursorI

        cursor0 = if (minIs0) minCursorI else maxCursorI
        cursor1 = if (minIs0) maxCursorI else minCursorI
        Editing.onChange()
    }
}