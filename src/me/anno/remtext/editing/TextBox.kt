package me.anno.remtext.editing

import me.anno.remtext.Controls.isShiftDown
import me.anno.remtext.font.Font
import kotlin.math.max
import kotlin.math.min

class TextBox {

    var text = ""
    var cursor0 = 0
    var cursor1 = 0

    fun getSelectedString(): String {
        val minCursor = min(cursor0, cursor1)
        val maxCursor = max(cursor0, cursor1)
        return text.substring(minCursor, maxCursor)
    }

    fun selectAll() {
        cursor0 = 0
        cursor1 = text.length
    }

    fun paste(str: String) {
        val minCursor = min(cursor0, cursor1)
        val maxCursor = max(cursor0, cursor1)
        paste(str, minCursor, maxCursor)
    }

    fun paste(str: String, minCursor: Int, maxCursor: Int) {
        text = text.substring(0, minCursor) + str + text.substring(maxCursor)
        cursor0 = minCursor + str.length
        cursor1 = cursor0
    }

    fun backspace() {
        val minCursor = max(min(cursor0, cursor1) - 1, 0)
        val maxCursor = max(cursor0, cursor1)
        paste("", minCursor, maxCursor)
    }

    fun delete() {
        val minCursor = min(cursor0, cursor1)
        val maxCursor = min(max(cursor0, cursor1) + 1, text.length)
        paste("", minCursor, maxCursor)
    }

    fun cursorLeft() {
        cursor1 = max(cursor1 - 1, 0)
        if (!isShiftDown) cursor0 = cursor1
    }

    fun cursorRight() {
        cursor1 = min(cursor1 + 1, text.length)
        if (!isShiftDown) cursor0 = cursor1
    }

    fun setCursorByX(mouseX: Int) {
        var x = 0
        val text = text
        for (i in text.indices) {
            val char = text[i]
            val nextChar = if (i + 1 < text.length) text[i + 1] else ' '
            val offset = Font.getOffset(char, nextChar)
            if (x + offset.shr(1) > mouseX) {
                // found it :)
                cursor1 = i
                return
            }
            x += offset
        }
        cursor1 = text.length
    }
}
