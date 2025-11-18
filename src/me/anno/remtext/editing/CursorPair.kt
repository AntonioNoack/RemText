package me.anno.remtext.editing

data class CursorPair(val first: Cursor, val second: Cursor) {

    val min: Cursor get() = if (first < second) first else second
    val max: Cursor get() = if (first < second) second else first

    fun isSelected(lineIndex: Int, relI: Int): Boolean {
        val minCursor = min
        val maxCursor = max
        return if (lineIndex in minCursor.lineIndex..maxCursor.lineIndex) {
            if (lineIndex == minCursor.lineIndex && lineIndex == maxCursor.lineIndex) {
                relI in minCursor.relI until maxCursor.relI
            } else if (lineIndex == minCursor.lineIndex) {
                relI >= minCursor.relI
            } else if (lineIndex == maxCursor.lineIndex) {
                relI < maxCursor.relI
            } else true // in-between
        } else false
    }

    fun show(lineIndex: Int, relI: Int): Boolean {
        return first == second &&
                first.lineIndex == lineIndex &&
                first.relI == relI
    }

    fun showEOL(lineIndex: Int, lineLength: Int): Boolean {
        return first == second &&
                first.lineIndex == lineIndex &&
                first.relI >= lineLength
    }

    constructor() : this(Cursor.ZERO, Cursor.ZERO)
}