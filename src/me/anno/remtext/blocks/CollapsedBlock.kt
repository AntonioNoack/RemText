package me.anno.remtext.blocks

class CollapsedBlock(val startIndex: Int, val endIndexIncl: Int) {
    fun isCollapsed(lineIndex: Int): Boolean {
        return lineIndex in (startIndex + 1)..endIndexIncl
    }
}