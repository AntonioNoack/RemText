package me.anno.remtext.editing

import me.anno.remtext.Rendering.cursors
import me.anno.remtext.Rendering.file
import me.anno.remtext.font.Line

class FormatChange(private val newLines: List<Line>) : Change {

    private val oldCursors = ArrayList(cursors)
    private val oldLines = ArrayList(file.lines)

    override fun redo() {
        file.lines.clear()
        file.lines.addAll(newLines)
        cursors.clear() // cursors probably have become invalid
        Editing.onChange()
    }

    override fun undo() {
        file.lines.clear()
        file.lines.addAll(oldLines)
        cursors.clear()
        cursors.addAll(oldCursors)
        Editing.onChange()
    }
}