package me.anno.remtext.editing

import me.anno.remtext.Rendering.cursor0
import me.anno.remtext.Rendering.cursor1
import me.anno.remtext.Rendering.file
import me.anno.remtext.font.Line

class FormatChange(private val newLines: List<Line>) : Change {

    private val oldCursor0 = cursor0
    private val oldCursor1 = cursor1
    private val oldLines = ArrayList(file.lines)

    override fun redo() {
        file.lines.clear()
        file.lines.addAll(newLines)
        cursor0 = Cursor.ZERO
        cursor1 = Cursor.ZERO
        Editing.onChange()
    }

    override fun undo() {
        file.lines.clear()
        file.lines.addAll(oldLines)
        cursor0 = oldCursor0
        cursor1 = oldCursor1
        Editing.onChange()
    }
}