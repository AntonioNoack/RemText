package me.anno.remtext.language

import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.font.Line
import me.anno.remtext.formatters.AutoFormatOptions

interface Language {
    fun highlight(line: Line, state0: Byte): Byte
    fun format(lines: List<Line>, options: AutoFormatOptions): List<Line>? = null

    fun colorize(lines: List<Line>) {
        var state = DEFAULT
        for (i in lines.indices) {
            val line = lines[i]
            val lineColors = line.colors ?: continue
            lineColors.fill(DEFAULT, line.i0, line.i1 + 1)
            state = highlight(line, state)
        }
    }
}