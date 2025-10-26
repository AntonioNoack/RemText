package me.anno.remtext

import me.anno.remtext.Rendering.barWidth
import me.anno.remtext.Rendering.blink0
import me.anno.remtext.Rendering.cursor0
import me.anno.remtext.Rendering.cursor1
import me.anno.remtext.Rendering.file
import me.anno.remtext.Rendering.maxI
import me.anno.remtext.Rendering.maxScrollX
import me.anno.remtext.Rendering.maxScrollY
import me.anno.remtext.Rendering.minI
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
import me.anno.remtext.editing.Editing.findLineAt
import me.anno.remtext.editing.Editing.getCursorPosition
import me.anno.remtext.editing.Editing.getFullString
import me.anno.remtext.editing.Editing.getSelectedString
import me.anno.remtext.editing.Editing.highLevelDeleteSelection
import me.anno.remtext.editing.Editing.highLevelPaste
import me.anno.remtext.editing.Editing.sameX
import me.anno.remtext.editing.FormatChange
import me.anno.remtext.editing.InputMode
import me.anno.remtext.editing.TextBox
import me.anno.remtext.font.Font
import me.anno.remtext.font.Font.lineHeight
import me.anno.remtext.font.Line
import me.anno.remtext.formatting.AutoFormatOptions
import org.lwjgl.glfw.GLFW.*
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.concurrent.thread
import kotlin.math.max
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
                GLFW_KEY_PAGE_UP -> Font.inc()
                GLFW_KEY_PAGE_DOWN -> Font.dec()
                GLFW_KEY_ESCAPE -> when (inputMode) {
                    InputMode.TEXT -> glfwSetWindowShouldClose(window, true)
                    else -> inputMode = InputMode.TEXT
                }
                GLFW_KEY_A -> {
                    if (pressed && isControlDown) {
                        when (inputMode) {
                            InputMode.TEXT -> {
                                cursor0 = Cursor.ZERO
                                val lines = file.lines
                                val lastLine = lines.lastIndex
                                cursor1 = Cursor(lastLine, lines[lastLine].i1)
                            }
                            InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.selectAll()
                            InputMode.REPLACE -> replaced.selectAll()
                        }

                    }
                }
                GLFW_KEY_C -> {
                    if (pressed && isControlDown) {
                        val joined = when (inputMode) {
                            InputMode.TEXT -> getSelectedString()
                            InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.getSelectedString()
                            InputMode.REPLACE -> replaced.getSelectedString()
                        }
                        if (joined.isNotEmpty()) {
                            copyContents(joined)
                        }
                    }
                }
                GLFW_KEY_X -> {
                    if (pressed && isControlDown) {
                        val joined = when (inputMode) {
                            InputMode.TEXT -> getSelectedString()
                            InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.getSelectedString()
                            InputMode.REPLACE -> replaced.getSelectedString()
                        }
                        if (joined.isNotEmpty()) {
                            copyContents(joined)
                            when (inputMode) {
                                InputMode.TEXT -> highLevelDeleteSelection()
                                InputMode.SEARCH, InputMode.SEARCH_ONLY -> {
                                    searched.paste("")
                                    updateSearchResults()
                                }
                                InputMode.REPLACE -> replaced.paste("")
                            }
                        }
                    }
                }
                GLFW_KEY_V -> {
                    if (pressed && isControlDown && file.finished) {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        val toPaste = clipboard.getData(DataFlavor.stringFlavor).toString()
                        when (inputMode) {
                            InputMode.TEXT -> highLevelPaste(toPaste)
                            InputMode.SEARCH, InputMode.SEARCH_ONLY -> {
                                searched.paste(toPaste)
                                updateSearchResults()
                            }
                            InputMode.REPLACE -> replaced.paste(toPaste)
                        }
                    }
                }
                GLFW_KEY_S -> {
                    if (pressed && isControlDown && file.finished) {
                        file.file.parentFile.mkdirs()
                        file.file.writeText(getFullString())
                        println("Saved ${file.file.absolutePath}")
                        file.modified = false
                        glfwSetWindowTitle(window, "$WINDOW_TITLE - ${file.file.name}")
                    }
                }
                GLFW_KEY_F -> {
                    if (pressed && isControlDown) {
                        if (isShiftDown && file.finished) {
                            val language = file.language
                            if (language != null) {
                                val options = AutoFormatOptions.nextOption(file.language)
                                val newLines = language.format(file.lines, options)
                                if (newLines != null) {
                                    println("Formatted ${file.file.absolutePath} using $options")
                                    language.colorize(newLines)
                                    file.history.push(FormatChange(newLines))
                                } else {
                                    println("No valid formatter found!")
                                }
                            } else {
                                println("No language detected!")
                            }
                        } else {
                            inputMode = InputMode.SEARCH_ONLY
                        }
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
                    if (pressed) {
                        blink0 = System.nanoTime()
                        inputMode = when (inputMode) {
                            InputMode.SEARCH -> InputMode.REPLACE
                            InputMode.REPLACE -> InputMode.SEARCH
                            InputMode.TEXT -> {
                                val n = 4 - cursor1.i.and(3)
                                val added = " ".repeat(n)
                                highLevelPaste(added)
                                inputMode
                            }
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
                GLFW_KEY_BACKSPACE -> when (inputMode) {
                    InputMode.TEXT -> {
                        if (cursor0 == cursor1) {
                            cursor0 = cursorLeft(cursor0)
                        }
                        highLevelDeleteSelection()
                    }
                    InputMode.SEARCH, InputMode.SEARCH_ONLY -> {
                        searched.backspace()
                        updateSearchResults()
                    }
                    InputMode.REPLACE -> replaced.backspace()
                }
                GLFW_KEY_DELETE -> when (inputMode) {
                    InputMode.TEXT -> {
                        if (cursor0 == cursor1) {
                            cursor0 = cursorRight(cursor0)
                        }
                        highLevelDeleteSelection()
                    }
                    InputMode.SEARCH, InputMode.SEARCH_ONLY -> {
                        searched.delete()
                        updateSearchResults()
                    }
                    InputMode.REPLACE -> replaced.delete()
                }
                GLFW_KEY_ENTER -> when (inputMode) {
                    InputMode.SEARCH_ONLY -> {
                        if (isShiftDown) showPrevSearchResult()
                        else showNextSearchResult()
                    }
                    InputMode.SEARCH -> inputMode = InputMode.REPLACE
                    InputMode.REPLACE -> {
                        val cursor = isOnSearchResult()
                        if (cursor != null) {
                            replaceSearchResult(cursor)
                            showNextSearchResult()
                        } else if (isShiftDown) showPrevSearchResult()
                        else showNextSearchResult()
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
                        showPrevSearchResult()
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
                        showNextSearchResult()
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
                isDraggingText -> draggingCursor = getCursorPosition(xi, yi)
                isLeftDown -> {
                    blink0 = System.nanoTime()
                    if (leftDownLine < numHiddenLines) {
                        val element = if (leftDownLine == 0) searched else replaced
                        element.setCursorByX(mouseX)
                    } else {
                        cursor1 = getCursorPosition(xi, yi)
                        sameX = -1
                    }
                }
            }
        }
        glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            if (button == GLFW_MOUSE_BUTTON_1) {
                // todo drag selected text
                //  ... when a drag starts inside an existing selection...

                isLeftDown = action == GLFW_PRESS

                val isOnYScrollbar = mouseX - (windowWidth - barWidth)
                val isOnXScrollbar = mouseY - (windowHeight - barWidth)
                isDraggingX = isLeftDown && !file.wrapLines && isOnXScrollbar >= 0 && isOnXScrollbar > isOnYScrollbar
                isDraggingY = isLeftDown && !isDraggingX && isOnYScrollbar >= 0

                when {
                    isDraggingX -> dragX()
                    isDraggingY -> dragY()
                    isLeftDown -> {
                        val lineIndex = mouseY / lineHeight
                        leftDownLine = lineIndex
                        if (lineIndex < numHiddenLines) {
                            val element = if (lineIndex == 0) searched else replaced
                            element.setCursorByX(mouseX)
                            element.cursor0 = element.cursor1
                            if (inputMode != InputMode.SEARCH_ONLY) {
                                inputMode = if (lineIndex == 0) InputMode.SEARCH else InputMode.REPLACE
                            }
                        } else if (isInsideSelection()) {
                            inputMode = InputMode.TEXT // just in case
                            isDraggingText = true
                            draggingCursor = getCursorPosition(mouseX, mouseY)
                        } else {
                            inputMode = InputMode.TEXT // just in case
                            setCursorByMouse()
                        }
                    }
                    isDraggingText -> {
                        isDraggingText = false
                        inputMode = InputMode.TEXT // just in case
                        if (!mouseHasMoved || isInsideSelection()) {
                            setCursorByMouse()
                        } else {
                            val pastePosition = getCursorPosition(mouseX, mouseY)
                            val selected = getSelectedString()
                            if (selected.isNotEmpty()) {
                                highLevelDeleteSelection()
                                cursor0 = pastePosition
                                cursor1 = pastePosition
                                highLevelPaste(selected)
                            }
                        }
                    }
                }

                movedSinceLeftPress = 0f
            }
        }
    }

    val mouseHasMoved get() = movedSinceLeftPress >= 10f

    fun setCursorByMouse() {
        blink0 = System.nanoTime()
        cursor0 = getCursorPosition(mouseX, mouseY)
        cursor1 = cursor0
    }

    var draggingCursor: Cursor = Cursor.ZERO
    var isDraggingText = false

    fun isInsideSelection(): Boolean {
        val lineStart = findLineAt(mouseY) ?: return false
        val minCursor = minI(cursor0, cursor1)
        val maxCursor = maxI(cursor0, cursor1)
        if (lineStart.lineIndex < minCursor.lineIndex || lineStart.lineIndex > maxCursor.lineIndex) {
            return false
        }

        val cursor = getCursorPosition(mouseX, lineStart)
        if (cursor !in minCursor..<maxCursor) {
            return false
        }

        val line = file.lines[lineStart.lineIndex]
        if (line.i0 == line.i1) return false // empty line

        val firstOffset = line.getOffset(lineStart.i)
        val searched = (mouseX - lineStart.whereIIsDrawnX + firstOffset)
        if (searched <= 0) return false // too far left
        // could be a binary search, but it doesn't matter, because this will be at max width/charWidth
        for (i in lineStart.i until line.i1) {
            val xi = line.getOffset(i)
            if (xi >= searched) return true
        }
        // end -> too far right included
        return false
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
    var leftDownLine = 0

    var movedSinceLeftPress = 0f

    var inputMode = InputMode.TEXT

    val searched = TextBox()
    val replaced = TextBox()

    var searchResults: List<Cursor> = emptyList()
    var shownSearchResult = 0

    fun isOnSearchResult(): Cursor? {
        val cursor = cursor0
        return if (cursor in searchResults) cursor else null
    }

    fun showNextSearchResult() {
        val cursor = cursor0
        val index = max(searchResults.indexOfFirst { it > cursor }, 0)
        val nextSearchResult = searchResults.getOrNull(index) ?: return
        shownSearchResult = index
        showSearchResult(nextSearchResult)
    }

    fun showPrevSearchResult() {
        val cursor = cursor0
        val index = max(searchResults.indexOfLast { it < cursor }, 0)
        val prevSearchResult = searchResults.getOrNull(index) ?: return
        shownSearchResult = index
        showSearchResult(prevSearchResult)
    }

    fun showSearchResult(cursor: Cursor) {
        // set cursor
        cursor0 = cursor
        cursor1 = Cursor(cursor.lineIndex, cursor.i + searched.text.length)
        scrollTo(cursor)
    }

    fun replaceSearchResult(cursor: Cursor) {
        // todo is this working 100% correctly?? paste might make issues
        //  recalculating would be the easy, but potentially expensive way
        showSearchResult(cursor)
        highLevelPaste(replaced.text)
        val tmp = ArrayList<Cursor>(searchResults)
        tmp.removeIf { it.lineIndex == cursor.lineIndex }
        val line = file.lines[cursor.lineIndex]
        collectSearchResults(line, cursor.lineIndex, searched.text, tmp)
        tmp.sort()
        searchResults = tmp
    }

    fun scrollTo(cursor: Cursor) {
        val dstLineIndex = if (file.wrapLines) {
            val lines = file.lines
            var numLines = 0
            val available = Window.availableWidth
            for (lineIndex in lines.indices) {
                val line = lines[lineIndex]
                if (lineIndex < cursor.lineIndex) {
                    numLines += line.getNumLines(available)
                } else {
                    // find the correct sub-index
                    val subLine = line.subList(line.i0, cursor.i)
                    numLines += subLine.getNumLines(available) - 1
                    break
                }
            }
            numLines
        } else {
            cursor.lineIndex
        }
        scrollY = dstLineIndex * lineHeight.toLong()
    }

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
                if (searched != Controls.searched.text) break

                val line = lines[lineIndex]
                collectSearchResults(line, lineIndex, searched, results)
            }
        }
    }

    fun collectSearchResults(
        line: Line, lineIndex: Int, searched: String,
        results: ArrayList<Cursor>
    ) {
        var i0 = line.i0
        while (true) {
            val i1 = line.text.indexOf(searched, i0, true)
            if (i1 < i0 || i1 >= line.i1) break

            results.add(Cursor(lineIndex, i1))
            i0 = i1 + searched.length
        }
    }

    val numHiddenLines: Int
        get() = when (inputMode) {
            InputMode.TEXT -> 0
            InputMode.SEARCH_ONLY -> 1
            InputMode.SEARCH, InputMode.REPLACE -> 2
        }

}