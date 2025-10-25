package me.anno.remtext.colors

import me.anno.remtext.font.Line
import me.anno.remtext.formatting.AutoFormatOptions

interface Language {
    fun highlight(line: Line, state0: Byte): Byte
    fun format(lines: List<Line>, options: AutoFormatOptions): List<Line> = lines

    fun List<Line>.colorize(): List<Line> {
        var state = Colors.DEFAULT
        for (i in indices) {
            state = highlight(this[i], state)
        }
        return this
    }

}