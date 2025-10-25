package me.anno.remtext

import me.anno.remtext.Rendering.countedLinesW
import me.anno.remtext.colors.Colors
import me.anno.remtext.colors.HighlighterLib
import me.anno.remtext.editing.Cursor
import me.anno.remtext.editing.History
import me.anno.remtext.font.Line
import me.anno.remtext.font.Line.Companion.fillOffsets
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.min

class OpenFile(val file: File) {

    companion object {
        const val MAX_FILE_LENGTH = 1_000_000_000
    }

    var cursor0 = Cursor(0, 0)
    var cursor1 = cursor0

    val highlighter = HighlighterLib.highlighters[file.extension.lowercase()]

    val lines = ArrayList<Line>()
    val history = History()

    var wrapLines = true
    var finished = false
    var modified = false

    var length = getFileLength()

    val loading = lazy { reload() }

    fun reload() {
        if (!file.exists() || file.isDirectory) {
            length = 0
            lines.clear()
            finished = true
            return
        }

        val reasonablySized = getFileLength() < MAX_FILE_LENGTH
        val text = if (reasonablySized) file.readText() else "File too big :("
        length = text.length
        finished = false
        lines.clear()

        thread(name = file.name) {
            val offsets = IntArray(text.length + 1)
            val colors = ByteArray(text.length)

            var i0 = 0
            var state = Colors.DEFAULT
            while (i0 < text.length) {
                var i1 = text.indexOf('\n', i0)
                if (i1 < 0) i1 = text.length
                val nextI0 = i1 + 1

                if (i1 > 0 && text[i1 - 1] == '\r') i1--

                fillOffsets(text, i0, i1, offsets)

                val line = Line(text, i0, i1, offsets, colors)
                if (i1 > i0) {
                    highlighter?.highlight(line, state)
                    state = line.colors[line.i1 - 1]
                }

                lines.add(line)
                countedLinesW = -1

                i0 = nextI0
            }
            if (text.endsWith('\n') || lines.isEmpty()) {
                lines.add(Line(""))
            }

            finished = reasonablySized
        }
    }

    private fun getFileLength(): Int = min(file.length(), Int.MAX_VALUE.toLong()).toInt()

}