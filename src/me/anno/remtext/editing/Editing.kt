package me.anno.remtext.editing

import me.anno.remtext.Rendering
import me.anno.remtext.Rendering.countedLinesW
import me.anno.remtext.Rendering.cursor0
import me.anno.remtext.Rendering.cursor1
import me.anno.remtext.Rendering.file
import me.anno.remtext.Rendering.maxI
import me.anno.remtext.Rendering.minI
import me.anno.remtext.Window.WINDOW_TITLE
import me.anno.remtext.Window.window
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.font.Font.lineHeight
import me.anno.remtext.font.Line
import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import kotlin.math.max
import kotlin.math.min

object Editing {

    fun highLevelPaste(added: String) {
        if (!file.finished) return // not ready yet
        file.history.push(PasteChange(added))
    }

    fun highLevelDeleteSelection() {
        highLevelPaste("")
    }

    fun onChange() {
        sameX = -1
        countedLinesW = -1
        if (!file.modified) {
            glfwSetWindowTitle(window, "$WINDOW_TITLE - ${file.file.name}*")
        }
    }

    fun deleteRangeImpl(minCursor: Cursor, maxCursor: Cursor): Cursor {

        val lines = file.lines
        val minLine = lines[minCursor.lineIndex]
        if (minCursor.lineIndex == maxCursor.lineIndex) {
            if (minCursor.i == maxCursor.i) return minCursor
            // delete within line
            lines[minCursor.lineIndex] = minLine.joinRanges(
                minLine.i0, minCursor.i, "",
                maxCursor.i, minLine.i1, minLine
            )
        } else {
            val maxLine = lines[maxCursor.lineIndex]
            lines[minCursor.lineIndex] = minLine.joinRanges(
                minLine.i0, minCursor.i, "",
                maxCursor.i, maxLine.i1, maxLine
            )
            lines.subList(minCursor.lineIndex + 1, maxCursor.lineIndex + 1).clear() // remove remaining lines
        }

        validateColorsInRange(minCursor.lineIndex)
        // then move cursor to the end
        return Cursor(
            minCursor.lineIndex,
            minCursor.i - minLine.i0 + lines[minCursor.lineIndex].i0
        )
    }

    fun pasteImpl(text: String, cursor: Cursor): Pair<Cursor, Cursor> {
        if (text.isEmpty()) return cursor to cursor

        val lines = file.lines
        val oldLine = lines[cursor.lineIndex]
        if ('\n' in text) {
            val added = text.split('\n')
            val firstNewLine = oldLine.joinRanges(
                oldLine.i0, cursor.i, added.first(),
                oldLine.i0, oldLine.i0, oldLine
            )
            lines[cursor.lineIndex] = firstNewLine

            val lastNewLine = oldLine.joinRanges(
                oldLine.i0, oldLine.i0, added.last(),
                cursor.i, oldLine.i1, oldLine
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
            val newCursorI1 = lastNewLine.i0 + added.last().length
            if (newCursorI1 !in lastNewLine.i0..lastNewLine.i1)
                throw IllegalStateException("newCursor out of bounds: $newCursorI1 !in ${lastNewLine.i0}-${lastNewLine.i1}")

            validateColorsInRange(cursor.lineIndex, lastNewLineIndex + 1)
            val newCursor0 = Cursor(cursor.lineIndex, cursor.i - oldLine.i0 + firstNewLine.i0)
            val newCursor1 = Cursor(lastNewLineIndex, newCursorI1)
            return newCursor0 to newCursor1
        } else {

            val newLine = oldLine.joinRanges(
                oldLine.i0, cursor.i, text,
                cursor.i, oldLine.i1, oldLine
            )
            lines[cursor.lineIndex] = newLine

            validateColorsInRange(cursor.lineIndex)
            val newCursor0 = Cursor(cursor.lineIndex, cursor.i - oldLine.i0 + newLine.i0)
            val newCursor1 = Cursor(cursor.lineIndex, newCursor0.i + text.length)
            return newCursor0 to newCursor1
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

    fun cursorLeft(cursor: Cursor): Cursor {
        val lines = file.lines
        val line = lines[cursor.lineIndex]
        return if (cursor.i > line.i0) {
            Cursor(cursor.lineIndex, cursor.i - 1)
        } else if (cursor.lineIndex > 0) {
            val prevLine = lines[cursor.lineIndex - 1]
            Cursor(cursor.lineIndex - 1, prevLine.i1)
        } else cursor
    }

    fun cursorRight(cursor: Cursor): Cursor {
        val lines = file.lines
        val line = lines[cursor.lineIndex]
        return if (cursor.i < line.i1) {
            Cursor(cursor.lineIndex, cursor.i + 1)
        } else if (cursor.lineIndex + 1 < lines.size) {
            val nextLine = lines[cursor.lineIndex + 1]
            Cursor(cursor.lineIndex + 1, nextLine.i0)
        } else cursor
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
                if (ls.i > cursor.i) break
                lineStart = ls
            }
        }
        lineStart ?: return cursor
        val line = file.lines[cursor.lineIndex]
        val x = if (sameX == -1) {
            lineStart.whereIIsDrawnX - line.getOffset(lineStart.i) +
                    (line.getOffset(cursor.i) + line.getOffset(min(cursor.i + 1, line.i1))).shr(1)
        } else sameX
        sameX = x
        val y = lineStart.y + dy * lineHeight
        // find the closest thing at x
        return getCursorPosition(x, y)
    }

    fun getSelectedString(): String {
        val minCursor = minI(cursor0, cursor1)
        val maxCursor = maxI(cursor0, cursor1)
        return getStringFromRange(minCursor, maxCursor)
    }

    fun getStringFromRange(minCursor: Cursor, maxCursor: Cursor): String {
        val lines = file.lines
        if (minCursor.lineIndex == maxCursor.lineIndex) {
            val line = lines[minCursor.lineIndex]
            return line.text.substring(minCursor.i, maxCursor.i)
        } else {
            val minLine = lines[minCursor.lineIndex]
            val maxLine = lines[maxCursor.lineIndex]

            val builder = StringBuilder()
            builder.append(minLine.text, minCursor.i, minLine.i1).append('\n')
            for (lineIndex in minCursor.lineIndex + 1 until maxCursor.lineIndex) {
                val line = lines[lineIndex]
                builder.append(line.text, line.i0, line.i1).append('\n')
            }
            builder.append(maxLine.text, maxLine.i0, maxCursor.i)
            return builder.toString()
        }
    }

    fun getFullString(): String {
        val lines = file.lines
        if (lines.isEmpty()) return ""

        val maxCursorLineIndex = lines.lastIndex
        val minLine = lines[0]
        val maxLine = lines[maxCursorLineIndex]
        val minCursor = Cursor(0, minLine.i0)
        val maxCursor = Cursor(maxCursorLineIndex, maxLine.i1)
        return getStringFromRange(minCursor, maxCursor)
    }

    fun getCursorPosition(x: Int, y: Int): Cursor {
        val lineStart = findLineAt(y) ?: return Cursor.ZERO
        return getCursorPosition(x, lineStart)
    }

    fun getCursorPosition(x: Int, lineStart: LineStart): Cursor {
        val line = file.lines[lineStart.lineIndex]

        val firstOffset = line.getOffset(lineStart.i)
        val searched = (x - lineStart.whereIIsDrawnX + firstOffset) shl 1
        // could be a binary search, but it doesn't matter, because this will be at max width/charWidth
        for (i in lineStart.i until line.i1) {
            val x0 = line.getOffset(i)
            val x1 = line.getOffset(i + 1)
            if (x0 + x1 >= searched) return Cursor(lineStart.lineIndex, i)
        }
        return Cursor(lineStart.lineIndex, line.i1)
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