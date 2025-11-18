package me.anno.remtext.editing

data class Cursor(val lineIndex: Int, val relI: Int) : Comparable<Cursor> {
    override fun compareTo(other: Cursor): Int {
        val c0 = lineIndex.compareTo(other.lineIndex)
        if (c0 != 0) return c0
        return relI.compareTo(other.relI)
    }

    companion object {
        val ZERO = Cursor(0, 0)
    }
}