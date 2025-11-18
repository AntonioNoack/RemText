package me.anno.remtext.editing

import me.anno.remtext.Controls.isControlDown
import me.anno.remtext.Rendering
import me.anno.remtext.Rendering.countedLinesW
import me.anno.remtext.Rendering.cursors
import me.anno.remtext.Rendering.file
import me.anno.remtext.Window.WINDOW_TITLE
import me.anno.remtext.Window.window
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.font.Font.lineHeight
import me.anno.remtext.font.Line
import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import kotlin.math.max
import kotlin.math.min

object Editing {

    fun highLevelPaste(added: List<String>) {
        if (!file.finished) return // not ready yet
        deduplicateCursors()
        file.history.push(PasteChange(added))
    }

    fun highLevelDeleteSelection() {
        highLevelPaste(listOf(""))
    }

    fun deduplicateCursors() {
        val uniqueCursors = cursors.distinct()
        cursors.clear()
        cursors.addAll(uniqueCursors)
    }

    fun onChange() {
        sameX = -1
        countedLinesW = -1
        if (!file.modified) {
            glfwSetWindowTitle(window, "$WINDOW_TITLE - ${file.file.name}*")
        }
    }

    /**
     * after change, maxCursor will be set on minCursor
     * */
    fun deleteRangeImpl(minCursor: Cursor, maxCursor: Cursor) {

        val lines = file.lines
        val minLine = lines[minCursor.lineIndex]
        if (minCursor.lineIndex == maxCursor.lineIndex) {
            if (minCursor.relI == maxCursor.relI) return
            // delete within line
            lines[minCursor.lineIndex] = minLine.joinRanges(
                minLine.i0, minLine.i0 + minCursor.relI, "",
                minLine.i0 + maxCursor.relI, minLine.i1, minLine
            )
        } else {
            val maxLine = lines[maxCursor.lineIndex]
            lines[minCursor.lineIndex] = minLine.joinRanges(
                minLine.i0, minLine.i0 + minCursor.relI, "",
                maxLine.i0 + maxCursor.relI, maxLine.i1, maxLine
            )
            lines.subList(minCursor.lineIndex + 1, maxCursor.lineIndex + 1).clear() // remove remaining lines
        }

        validateColorsInRange(minCursor.lineIndex)
    }

    /**
     * returns end cursor, start cursor is provided and stays the same
     * */
    fun pasteImpl(text: String, cursor: Cursor): Cursor {
        if (text.isEmpty()) return cursor

        val lines = file.lines
        val oldLine = lines[cursor.lineIndex]
        if ('\n' in text) {
            val added = text.split('\n')
            val firstNewLine = oldLine.joinRanges(
                oldLine.i0, oldLine.i0 + cursor.relI, // start of line
                added.first(), // inserted
                oldLine.i0, oldLine.i0, oldLine // actually nothing
            )
            lines[cursor.lineIndex] = firstNewLine

            val lastNewLine = oldLine.joinRanges(
                oldLine.i0, oldLine.i0, // actually nothing
                added.last(), // inserted
                oldLine.i0 + cursor.relI, oldLine.i1, oldLine // end of line
            )
            lines.add(cursor.lineIndex + 1, lastNewLine)

            // add lines in-between
            for (li in added.size - 2 downTo 1) {
                val middleNewLine = Line(added[li])
                lines.add(cursor.lineIndex + 1, middleNewLine)
            }

            // move cursors to the end
            val lastNewLineIndex = cursor.lineIndex + added.size - 1
            if (lines[lastNewLineIndex] != lastNewLine) throw IllegalStateException()
            val newCursorI1 = added.last().length
            if (newCursorI1 !in 0..lastNewLine.length)
                throw IllegalStateException("newCursor out of bounds: $newCursorI1 !in ${lastNewLine.i0}-${lastNewLine.i1}")

            validateColorsInRange(cursor.lineIndex, lastNewLineIndex + 1)
            return Cursor(lastNewLineIndex, newCursorI1)
        } else {

            val newLine = oldLine.joinRanges(
                oldLine.i0, oldLine.i0 + cursor.relI, text,
                oldLine.i0 + cursor.relI, oldLine.i1, oldLine
            )
            lines[cursor.lineIndex] = newLine

            validateColorsInRange(cursor.lineIndex)
            return Cursor(cursor.lineIndex, cursor.relI + text.length)
        }
    }

    fun validateColorsInRange(i0: Int, i1: Int = i0 + 1) {
        val hl = file.language ?: return
        val lines = file.lines
        val prevLine = lines.getOrNull(i0 - 1)
        var state = if (prevLine != null) prevLine.colors!![prevLine.i1] else DEFAULT
        for (i in i0 until i1) {
            val line = lines.getOrNull(i) ?: return
            val lineColors = line.colors ?: continue
            lineColors.fill(DEFAULT, line.i0, line.i1 + 1)
            state = hl.highlight(line, state)
        }
        // while state != lines[i].colors[i0], validate more lines
        for (i in i1 until lines.size) {
            val line = lines.getOrNull(i) ?: return
            if (line.i1 > line.i0) {
                val lineColors = line.colors ?: continue
                if (state == lineColors[line.i0]) return // should be done
                lineColors.fill(DEFAULT, line.i0, line.i1 + 1)
                state = hl.highlight(line, state)
            }
        }
    }

    fun isWord(c: Char): Boolean {
        return c.isLetterOrDigit() || c in "_"
    }

    fun cursorLeft(cursor: Cursor): Cursor {
        val lines = file.lines
        val line = lines[cursor.lineIndex]
        return if (cursor.relI > 0) {
            // all is fine
            var newI = cursor.relI - 1
            if (isControlDown) {
                while (newI > 0 && !isWord(line.text[line.i0 + newI])) newI--
                while (newI > 0 && isWord(line.text[line.i0 + newI])) newI--
            }
            Cursor(cursor.lineIndex, newI)
        } else if (cursor.lineIndex > 0) {
            // move up by one line
            val prevLine = lines[cursor.lineIndex - 1]
            Cursor(cursor.lineIndex - 1, prevLine.length)
        } else cursor // stay at start
    }

    fun cursorRight(cursor: Cursor): Cursor {
        val lines = file.lines
        val line = lines[cursor.lineIndex]
        val lineLength = line.length
        return if (cursor.relI < lineLength) {
            // all is fine
            var newI = cursor.relI + 1
            if (isControlDown) {
                while (newI < lineLength && !isWord(line.text[line.i0 + newI])) newI++
                while (newI < lineLength && isWord(line.text[line.i0 + newI])) newI++
            }
            Cursor(cursor.lineIndex, newI)
        } else if (cursor.lineIndex + 1 < lines.size) {
            // move down by one line
            Cursor(cursor.lineIndex + 1, 0)
        } else cursor // stay at end
    }

    fun cursorUp(cursor: Cursor): Cursor {
        return cursorDY(cursor, -1)
    }

    fun cursorDown(cursor: Cursor): Cursor {
        return cursorDY(cursor, 1)
    }

    var sameX = -1

    fun cursorDY(cursor: Cursor, dy: Int): Cursor {
        // find cursor x
        val lineStarts = Rendering.lineStarts
        var lineStart: LineStart? = null
        for (ls in lineStarts) {
            if (ls.lineIndex > cursor.lineIndex) break
            if (ls.lineIndex == cursor.lineIndex) {
                if (ls.relI > cursor.relI) break
                lineStart = ls
            }
        }
        lineStart ?: return cursor
        val line = file.lines[cursor.lineIndex]
        val x = if (sameX == -1) {
            lineStart.whereIIsDrawnX - line.getOffset(line.i0 + lineStart.relI) +
                    line.getOffset(line.i0 + cursor.relI)
        } else sameX
        sameX = x
        val y = lineStart.y + dy * lineHeight
        // find the closest thing at x
        return getCursorPosition(x, y)
    }

    fun getSelectedStrings(): List<String> {
        return cursors.map { cursor ->
            getStringFromRange(cursor.min, cursor.max)
        }
    }

    fun getStringFromRange(minCursor: Cursor, maxCursor: Cursor): String {
        val lines = file.lines
        if (minCursor.lineIndex == maxCursor.lineIndex) {
            val line = lines[minCursor.lineIndex]
            return line.text.substring(
                line.i0 + minCursor.relI,
                line.i0 + maxCursor.relI
            )
        } else {
            val minLine = lines[minCursor.lineIndex]
            val maxLine = lines[maxCursor.lineIndex]

            val builder = StringBuilder()
            builder.append(minLine.text, minLine.i0 + minCursor.relI, minLine.i1).append('\n')
            for (lineIndex in minCursor.lineIndex + 1 until maxCursor.lineIndex) {
                val line = lines[lineIndex]
                builder.append(line.text, line.i0, line.i1).append('\n')
            }
            builder.append(maxLine.text, maxLine.i0, maxLine.i0 + maxCursor.relI)
            return builder.toString()
        }
    }

    fun getFullString(): String {
        val lines = file.lines
        if (lines.isEmpty()) return ""

        val maxCursorLineIndex = lines.lastIndex
        val maxLine = lines[maxCursorLineIndex]
        val minCursor = Cursor(0, 0)
        val maxCursor = Cursor(maxCursorLineIndex, maxLine.length)
        return getStringFromRange(minCursor, maxCursor)
    }

    fun getCursorPosition(x: Int, y: Int): Cursor {
        val lineStart = findLineAt(y) ?: return Cursor.ZERO
        return getCursorPosition(x, lineStart)
    }

    fun getCursorPosition(x: Int, lineStart: LineStart): Cursor {
        val line = file.lines[lineStart.lineIndex]

        val firstOffset = line.getOffset(line.i0 + lineStart.relI)
        val searched = (x - lineStart.whereIIsDrawnX + firstOffset) shl 1
        // could be a binary search, but it doesn't matter, because this will be at max width/charWidth
        for (i in line.i0 + lineStart.relI until line.i1) {
            val x0 = line.getOffset(i)
            val x1 = line.getOffset(i + 1)
            if (x0 + x1 >= searched) {
                return Cursor(lineStart.lineIndex, i - line.i0)
            }
        }
        return Cursor(lineStart.lineIndex, line.length)
    }

    fun findLineAt(y: Int): LineStart? {
        val lineStarts = Rendering.lineStarts
        if (lineStarts.isEmpty()) return null

        var li = lineStarts.binarySearch { it.y - y }
        if (li < 0) li = -li - 2
        li = min(max(li, 0), lineStarts.lastIndex)

        return lineStarts[li]
    }

}