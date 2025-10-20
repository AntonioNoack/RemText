package me.anno.remtext

import me.anno.remtext.Rendering.barWidth
import me.anno.remtext.Rendering.blink0
import me.anno.remtext.Rendering.cursor0
import me.anno.remtext.Rendering.cursor1
import me.anno.remtext.Rendering.file
import me.anno.remtext.Rendering.maxScrollX
import me.anno.remtext.Rendering.maxScrollY
import me.anno.remtext.Window.WINDOW_TITLE
import me.anno.remtext.Window.isDarkTheme
import me.anno.remtext.Window.lightThemeFile
import me.anno.remtext.Window.window
import me.anno.remtext.Window.windowHeight
import me.anno.remtext.Window.windowWidth
import me.anno.remtext.editing.Cursor
import me.anno.remtext.editing.Editing.cursorDown
import me.anno.remtext.editing.Editing.cursorLeft
import me.anno.remtext.editing.Editing.cursorRight
import me.anno.remtext.editing.Editing.cursorUp
import me.anno.remtext.editing.Editing.getCursorPosition
import me.anno.remtext.editing.Editing.getFullString
import me.anno.remtext.editing.Editing.getSelectedString
import me.anno.remtext.editing.Editing.highLevelDeleteSelection
import me.anno.remtext.editing.Editing.highLevelPaste
import me.anno.remtext.editing.Editing.sameX
import me.anno.remtext.font.Font.lineHeight
import org.lwjgl.glfw.GLFW.*
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object Controls {

    var scrollX = 0L
    var scrollY = 0L

    var mouseX = 0
    var mouseY = 0

    var isShiftDown = false
    var isControlDown = false

    fun addListeners() {
        glfwSetScrollCallback(window) { _, _, dy ->
            val delta = (dy.toFloat() * 20f).toInt()
            if (isShiftDown) scrollX -= delta
            else scrollY -= delta
        }
        glfwSetKeyCallback(window) { _, key, _, action, _ ->
            // action: GLFW_PRESS, GLFW_RELEASE or GLFW_REPEAT
            // key: The keyboard key that was pressed or released.
            val pressed = action == GLFW_PRESS
            val typed = pressed || action == GLFW_REPEAT

            if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT) {
                isShiftDown = typed
            }
            if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL) {
                isControlDown = typed
            }

            if (typed) when (key) {
                GLFW_KEY_F1 -> if (pressed) {
                    isDarkTheme = !isDarkTheme
                    if (isDarkTheme) lightThemeFile.delete()
                    else lightThemeFile.createNewFile()
                }
                GLFW_KEY_F2 -> if (pressed) {
                    file.wrapLines = !file.wrapLines
                }
                GLFW_KEY_ESCAPE -> when (inputMode) {
                    InputMode.TEXT -> glfwSetWindowShouldClose(window, true)
                    else -> inputMode = InputMode.TEXT
                }
                GLFW_KEY_A -> {
                    if (pressed && isControlDown) {
                        cursor0 = Cursor(0, 0)
                        val lines = file.lines
                        val lastLine = lines.lastIndex
                        cursor1 = Cursor(lastLine, lines[lastLine].i1)
                    }
                }
                GLFW_KEY_C -> {
                    if (pressed && isControlDown) {
                        val joined = getSelectedString()
                        if (joined.isNotEmpty()) {
                            copyContents(joined)
                        }
                    }
                }
                GLFW_KEY_X -> {
                    if (pressed && isControlDown) {
                        val joined = getSelectedString()
                        if (joined.isNotEmpty()) {
                            copyContents(joined)
                            highLevelDeleteSelection()
                        }
                    }
                }
                GLFW_KEY_V -> {
                    if (pressed && isControlDown && file.finished) {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        val toPaste = clipboard.getData(DataFlavor.stringFlavor).toString()
                        highLevelPaste(toPaste)
                    }
                }
                GLFW_KEY_S -> {
                    if (pressed && isControlDown && file.finished) {
                        file.file.parentFile.mkdirs()
                        file.file.writeText(getFullString())
                        file.modified = false
                        glfwSetWindowTitle(window, "$WINDOW_TITLE - ${file.file.name}")
                    }
                }
                GLFW_KEY_F -> {
                    if (pressed && isControlDown) {
                        inputMode = InputMode.SEARCH_ONLY
                    }
                }
                GLFW_KEY_R -> {
                    if (pressed && isControlDown) {
                        inputMode =
                            if (inputMode != InputMode.SEARCH) InputMode.SEARCH
                            else InputMode.REPLACE
                    }
                }
                GLFW_KEY_TAB -> {
                    if (pressed && isControlDown) {
                        inputMode = when (inputMode) {
                            InputMode.SEARCH -> InputMode.REPLACE
                            InputMode.REPLACE -> InputMode.SEARCH
                            else -> inputMode
                        }
                    }
                }
                GLFW_KEY_N -> {
                    if (pressed && isControlDown) {
                        // todo create a new file
                    }
                }
                GLFW_KEY_O -> {
                    if (pressed && isControlDown) {
                        // todo open a file...
                    }
                }
                GLFW_KEY_Z, GLFW_KEY_Y -> {
                    if (pressed && isControlDown) {
                        if (isShiftDown) file.history.redo()
                        else file.history.undo()
                    }
                }
                GLFW_KEY_BACKSPACE -> {
                    if (cursor0 == cursor1) {
                        cursor0 = cursorLeft(cursor0)
                    }
                    highLevelDeleteSelection()
                }
                GLFW_KEY_DELETE -> {
                    if (cursor0 == cursor1) {
                        cursor0 = cursorRight(cursor0)
                    }
                    highLevelDeleteSelection()
                }
                GLFW_KEY_ENTER -> when (inputMode) {
                    InputMode.SEARCH_ONLY -> {
                        // todo show next result
                    }
                    InputMode.SEARCH -> inputMode = InputMode.REPLACE
                    InputMode.REPLACE -> {
                        // todo if already at next result, replace
                        // todo else show next result
                    }
                    InputMode.TEXT -> highLevelPaste("\n")
                }
                GLFW_KEY_LEFT -> when (inputMode) {
                    InputMode.TEXT -> {
                        val newCursor = cursorLeft(cursor1)
                        cursor1 = newCursor
                        if (!isShiftDown) cursor0 = newCursor
                        blink0 = System.nanoTime()
                        sameX = -1
                    }
                    InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.cursorLeft()
                    InputMode.REPLACE -> replaced.cursorRight()
                }
                GLFW_KEY_RIGHT -> when (inputMode) {
                    InputMode.TEXT -> {
                        val newCursor = cursorRight(cursor1)
                        cursor1 = newCursor
                        if (!isShiftDown) cursor0 = newCursor
                        blink0 = System.nanoTime()
                        sameX = -1
                    }
                    InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.cursorRight()
                    InputMode.REPLACE -> replaced.cursorRight()
                }
                GLFW_KEY_UP -> {
                    if (inputMode == InputMode.TEXT) {
                        val newCursor = cursorUp(cursor1)
                        if (newCursor != cursor1) {
                            cursor1 = newCursor
                            if (!isShiftDown) cursor0 = newCursor
                            blink0 = System.nanoTime()
                            scrollY -= lineHeight
                        }
                    } else {
                        // todo show previous search result
                    }
                }
                GLFW_KEY_DOWN -> {
                    if (inputMode == InputMode.TEXT) {
                        val newCursor = cursorDown(cursor1)
                        if (newCursor != cursor1) {
                            cursor1 = newCursor
                            if (!isShiftDown) cursor0 = newCursor
                            blink0 = System.nanoTime()
                            scrollY += lineHeight
                        }
                    } else {
                        // todo show next search result
                    }
                }
            }
        }
        glfwSetCharCallback(window) { _, char ->
            val added = String(Character.toChars(char))
            when (inputMode) {
                InputMode.TEXT -> highLevelPaste(added)
                InputMode.SEARCH, InputMode.SEARCH_ONLY -> {
                    searched.paste(added)
                    updateSearchResults()
                }
                InputMode.REPLACE -> replaced.paste(added)
            }
        }
        glfwSetCursorPosCallback(window) { _, x, y ->
            val xi = x.toInt()
            val yi = y.toInt()
            val dx = xi - mouseX
            val dy = yi - mouseY
            val distance = sqrt((dx * dx + dy * dy).toFloat())
            movedSinceLeftPress += distance
            mouseX = xi
            mouseY = yi
            when {
                isDraggingX -> dragX()
                isDraggingY -> dragY()
                isLeftDown -> {
                    cursor1 = getCursorPosition(xi, yi)
                    blink0 = System.nanoTime()
                    sameX = -1
                }
            }
        }
        glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            if (button == GLFW_MOUSE_BUTTON_1) {
                // todo drag selected text???
                //  ... when a drag starts inside an existing selection...
                isLeftDown = action == GLFW_PRESS

                val scrollY = mouseX - (windowWidth - barWidth)
                val scrollX = mouseY - (windowHeight - barWidth)
                isDraggingX = isLeftDown && scrollX >= 0 && scrollX > scrollY
                isDraggingY = isLeftDown && !isDraggingX && scrollY >= 0

                when {
                    isDraggingX -> dragX()
                    isDraggingY -> dragY()
                    isLeftDown -> {
                        blink0 = System.nanoTime()
                        cursor0 = getCursorPosition(mouseX, mouseY)
                        cursor1 = cursor0
                    }
                }

                val now = System.nanoTime()
                if (!isLeftDown && movedSinceLeftPress < 10f && now - downTime < 500_000_000L) {
                    // a click
                }
                downTime = now
                movedSinceLeftPress = 0f
            }
        }
    }

    fun dragX() {
        scrollX = (mouseX.toLong() * maxScrollX / windowWidth)
    }

    fun dragY() {
        scrollY = (mouseY.toLong() * maxScrollY / windowHeight)
    }

    fun copyContents(joined: String) {
        val stringSelection = StringSelection(joined)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }

    var isLeftDown = false
    var isDraggingX = false
    var isDraggingY = false

    var movedSinceLeftPress = 0f
    var downTime = 0L

    enum class InputMode {
        SEARCH_ONLY,
        SEARCH,
        REPLACE,
        TEXT
    }

    class TextBox {
        var text = ""
        var cursor = 0

        fun paste(str: String) {
            text = text.substring(0, cursor) + str + text.substring(cursor)
            cursor += str.length
        }

        fun cursorLeft() {
            cursor = max(cursor - 1, 0)
        }

        fun cursorRight() {
            cursor = min(cursor + 1, text.length)
        }
    }

    var inputMode = InputMode.TEXT

    val searched = TextBox()
    val replaced = TextBox()

    var searchResults: List<Cursor> = emptyList()

    fun updateSearchResults() {
        val searched = searched.text
        if (searched.isEmpty()) {
            searchResults = emptyList()
            return
        }

        // todo support searches with line-breaks

        val lines = file.lines
        val results = ArrayList<Cursor>()
        searchResults = results

        thread(name = "Search") {
            for (lineIndex in lines.indices) {
                val line = lines[lineIndex]
                if (searched != Controls.searched.text) break

                var i0 = line.i0
                while (true) {
                    val i1 = line.text.indexOf(searched, i0)
                    if (i1 < i0 || i1 >= line.i1) break

                    results.add(Cursor(lineIndex, i1))
                    i0 = i1 + searched.length
                }
            }
        }
    }

}