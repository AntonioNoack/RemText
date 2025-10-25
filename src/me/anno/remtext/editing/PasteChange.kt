package me.anno.remtext.editing

import me.anno.remtext.Rendering.cursor0
import me.anno.remtext.Rendering.cursor1
import me.anno.remtext.Rendering.maxI
import me.anno.remtext.Rendering.minI

class PasteChange(private val added: String) : Change {

    // required for redo
    private var minCursor = minI(cursor0, cursor1)
    private var maxCursor = maxI(cursor0, cursor1)
    private val minIs0 = minCursor == cursor0

    // required for undo
    private val removed = Editing.getStringFromRange(minCursor, maxCursor)
    private lateinit var newCursor0I: Cursor
    private lateinit var newCursor1I: Cursor

    override fun redo() {
        val newCursor0 = Editing.deleteRangeImpl(minCursor, maxCursor)
        val (newCursorI0, newCursorI1) = Editing.pasteImpl(added, newCursor0)
        this.newCursor0I = newCursorI0
        this.newCursor1I = newCursorI1
        cursor0 = newCursorI1
        cursor1 = newCursorI1
        Editing.onChange()
    }

    override fun undo() {
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