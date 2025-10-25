package me.anno.remtext.editing

interface Change {
    fun redo()
    fun undo()
}