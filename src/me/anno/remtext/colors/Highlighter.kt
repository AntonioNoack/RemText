package me.anno.remtext.colors

import me.anno.remtext.font.Line

interface Highlighter {
    fun highlight(line: Line, state0: Byte)
}