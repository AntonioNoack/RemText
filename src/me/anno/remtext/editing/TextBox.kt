package me.anno.remtext.editing

import me.anno.remtext.Controls.isShiftDown
import me.anno.remtext.font.Emojis
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
        val minCursor0 = min(cursor0, cursor1)
        val minCursor = if (cursor0 == cursor1) {
            max(Emojis.findPreviousStart(text, minCursor0, 0), 0)
        } else minCursor0
        val maxCursor = max(cursor0, cursor1)
        paste("", minCursor, maxCursor)
    }

    fun delete() {
        val minCursor = min(cursor0, cursor1)
        val maxCursor = if (cursor0 == cursor1) {
            val match = Emojis.findMatch(text, minCursor, text.length)
            min(minCursor + (match?.length ?: 1), text.length)
        } else max(cursor0, cursor1)
        paste("", minCursor, maxCursor)
    }

    fun override(selectedText: String) {
        text = selectedText
        cursor0 = 0
        cursor1 = text.length
    }

    fun cursorLeft() {
        cursor1 = max(Emojis.findPreviousStart(text, cursor1, 0), 0)
        if (!isShiftDown) cursor0 = cursor1
    }

    fun cursorRight() {
        val match = Emojis.findMatch(text, cursor1, text.length)
        cursor1 = min(cursor1 + (match?.length ?: 1), text.length)
        if (!isShiftDown) cursor0 = cursor1
    }

    fun setCursorByX(mouseX: Int) {
        var x = 0
        val text = text
        var i = 0
        while (i < text.length) {
            val char = text[i]
            val match = Emojis.findMatch(text, i, text.length)
            val offset = if (match != null) Font.emojiWidth else {
                val nextChar = if (i + 1 < text.length) text[i + 1] else ' '
                Font.getOffset(char, nextChar)
            }
            if (x + offset.shr(1) > mouseX) {
                // found it :)
                cursor1 = i
                return
            }
            x += offset
            i += match?.length ?: 1
        }
        cursor1 = text.length
    }
}
