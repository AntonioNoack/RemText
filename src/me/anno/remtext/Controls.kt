package me.anno.remtext

import me.anno.remtext.Rendering.barWidth
import me.anno.remtext.Rendering.blink0
import me.anno.remtext.Rendering.cursors
import me.anno.remtext.Rendering.file
import me.anno.remtext.Rendering.maxScrollX
import me.anno.remtext.Rendering.maxScrollY
import me.anno.remtext.Window.WINDOW_TITLE
import me.anno.remtext.Window.isDarkTheme
import me.anno.remtext.Window.lightThemeFile
import me.anno.remtext.Window.window
import me.anno.remtext.Window.windowHeight
import me.anno.remtext.Window.windowWidth
import me.anno.remtext.editing.*
import me.anno.remtext.editing.Editing.cursorDown
import me.anno.remtext.editing.Editing.cursorLeft
import me.anno.remtext.editing.Editing.cursorRight
import me.anno.remtext.editing.Editing.cursorUp
import me.anno.remtext.editing.Editing.findLineAt
import me.anno.remtext.editing.Editing.getCursorPosition
import me.anno.remtext.editing.Editing.getFullString
import me.anno.remtext.editing.Editing.getSelectedStrings
import me.anno.remtext.editing.Editing.highLevelDeleteSelection
import me.anno.remtext.editing.Editing.highLevelPaste
import me.anno.remtext.editing.Editing.sameX
import me.anno.remtext.editing.StringListTransferable.Companion.stringListFlavor
import me.anno.remtext.font.Font
import me.anno.remtext.font.Font.lineHeight
import me.anno.remtext.font.Line
import me.anno.remtext.formatters.AutoFormatOptions
import org.lwjgl.glfw.GLFW.*
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.sqrt

object Controls {

    private var cursor0: Cursor
        get() = file.cursor0
        set(value) {
            file.cursor0 = value
        }

    private var cursor1: Cursor
        get() = file.cursor1
        set(value) {
            file.cursor1 = value
        }

    var scrollX = 0L
    var scrollY = 0L

    var mouseX = 0
    var mouseY = 0

    var isAltDown = false
    var isShiftDown = false
    var isControlDown = false

    var lastPressedControl = 0L
    var isDoubleControlDown = false

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
                if (!typed) { // key up
                    lastPressedControl = System.nanoTime()
                    isDoubleControlDown = false
                } else {
                    // key down
                    if (System.nanoTime() - lastPressedControl < 300e6) {
                        isDoubleControlDown = true
                    }
                }
            }
            if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT) {
                isAltDown = typed
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
                    InputMode.TEXT -> {
                        if (cursors.size > 1) cursors.subList(0, cursors.lastIndex).clear()
                        else glfwSetWindowShouldClose(window, true)
                    }
                    else -> inputMode = InputMode.TEXT
                }
                GLFW_KEY_A -> {
                    if (pressed && isControlDown) {
                        when (inputMode) {
                            InputMode.TEXT -> {
                                val first = Cursor.ZERO
                                val lines = file.lines
                                val lastLineI = lines.lastIndex
                                val second = Cursor(lastLineI, lines[lastLineI].length)
                                cursors.clear()
                                cursors.add(CursorPair(first, second))
                            }
                            InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.selectAll()
                            InputMode.REPLACE -> replaced.selectAll()
                        }
                    }
                }
                GLFW_KEY_C -> {
                    if (pressed && isControlDown) {
                        val joined = getJoinedForCopy()
                        if (joined.any { it.isNotEmpty() }) {
                            copyContents(joined)
                        }
                    }
                }
                GLFW_KEY_X -> {
                    if (pressed && isControlDown) {
                        val joined = getJoinedForCopy()
                        if (joined.any { it.isNotEmpty() }) {
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
                        val toPaste = getClipboardStrings()
                        when (inputMode) {
                            InputMode.TEXT -> highLevelPaste(toPaste)
                            InputMode.SEARCH, InputMode.SEARCH_ONLY -> {
                                searched.paste(toPaste.joinToString("\n"))
                                updateSearchResults()
                            }
                            InputMode.REPLACE -> replaced.paste(toPaste.joinToString("\n"))
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
                                val added = cursors.map { pair ->
                                    val n = 4 - pair.second.relI.and(3)
                                    " ".repeat(n)
                                }
                                highLevelPaste(added)
                                inputMode
                            }
                            else -> inputMode
                        }
                    }
                }
                GLFW_KEY_Z, GLFW_KEY_Y -> {
                    if (isControlDown) {
                        if (isShiftDown) file.history.redo()
                        else file.history.undo()
                    }
                }
                GLFW_KEY_BACKSPACE -> when (inputMode) {
                    InputMode.TEXT -> {
                        cursors.replaceAll { pair ->
                            if (pair.first == pair.second) {
                                val newFirst = cursorLeft(pair.first)
                                CursorPair(newFirst, pair.second)
                            } else pair
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
                        cursors.replaceAll { pair ->
                            if (pair.first == pair.second) {
                                val newFirst = cursorRight(pair.first)
                                CursorPair(newFirst, pair.second)
                            } else pair
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
                    InputMode.TEXT -> highLevelPaste(listOf("\n"))
                }
                GLFW_KEY_LEFT -> when (inputMode) {
                    InputMode.TEXT -> moveCursors(::cursorLeft)
                    InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.cursorLeft()
                    InputMode.REPLACE -> replaced.cursorRight()
                }
                GLFW_KEY_RIGHT -> when (inputMode) {
                    InputMode.TEXT -> moveCursors(::cursorRight)
                    InputMode.SEARCH, InputMode.SEARCH_ONLY -> searched.cursorRight()
                    InputMode.REPLACE -> replaced.cursorRight()
                }
                GLFW_KEY_UP -> {
                    if (inputMode == InputMode.TEXT) {
                        if (isDoubleControlDown) {
                            duplicateCursorUp()
                        } else if (!isControlDown) {
                            if (moveCursors(::cursorUp)) {
                                scrollY -= lineHeight
                            }
                        } else scrollY -= lineHeight
                    } else showPrevSearchResult()
                }
                GLFW_KEY_DOWN -> {
                    if (inputMode == InputMode.TEXT) {
                        if (isDoubleControlDown) {
                            duplicateCursorDown()
                        } else if (!isControlDown) {
                            if (moveCursors(::cursorDown)) {
                                scrollY += lineHeight
                            }
                        } else scrollY += lineHeight
                    } else showNextSearchResult()
                }
            }
        }
        glfwSetCharCallback(window) { _, char ->
            val added = String(Character.toChars(char))
            when (inputMode) {
                InputMode.TEXT -> highLevelPaste(listOf(added))
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
                        } else if (isAltDown && isShiftDown) {
                            val newCursor = getCursorPosition(mouseX, mouseY)
                            file.cursors.add(CursorPair(newCursor, newCursor))
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
                            // todo this is probably weird with multi-selections
                            val pastePosition = getCursorPosition(mouseX, mouseY)
                            val selected = getSelectedStrings()
                            if (selected.any { it.isNotEmpty() }) {
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

    fun moveCursors(moveCursor: (Cursor) -> Cursor): Boolean {
        var changed = false
        if (cursors.isEmpty()) cursors.add(CursorPair())
        cursors.replaceAll { (first, second) ->
            val newSecond = moveCursor(second)
            if (!changed && newSecond != second) changed = true
            val newFirst = if (!isShiftDown) newSecond else first
            if (!changed && first != newFirst) changed = true
            CursorPair(newFirst, newSecond)
        }
        blink0 = System.nanoTime()
        sameX = -1
        return changed
    }

    fun duplicateCursorUp() {
        val newCursor = cursorUp(cursor1)
        file.cursors.add(CursorPair(newCursor, newCursor))
    }

    fun duplicateCursorDown() {
        val newCursor = cursorDown(cursor1)
        file.cursors.add(CursorPair(newCursor, newCursor))
    }

    fun getJoinedForCopy(): List<String> {
        return when (inputMode) {
            InputMode.TEXT -> getSelectedStrings()
            InputMode.SEARCH, InputMode.SEARCH_ONLY -> listOf(searched.getSelectedString())
            InputMode.REPLACE -> listOf(replaced.getSelectedString())
        }
    }

    fun getClipboardStrings(): List<String> {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        try {
            @Suppress("UNCHECKED_CAST")
            return clipboard.getData(stringListFlavor) as List<String>
        } catch (_: UnsupportedFlavorException) {
            val text = clipboard.getData(DataFlavor.stringFlavor)
                .toString().replace("\r", "")
            if (cursors.size == text.count { it == '\n' } + 1) {
                // match for multi-editing :)
                return text.split('\n')
            }
            return listOf(text)
        }
    }

    val mouseHasMoved get() = movedSinceLeftPress >= 10f

    fun setCursorByMouse() {
        blink0 = System.nanoTime()

        val newCursor = getCursorPosition(mouseX, mouseY)
        cursors.clear()
        cursors.add(CursorPair(newCursor, newCursor))
    }

    var draggingCursor: Cursor = Cursor.ZERO
    var isDraggingText = false

    fun isInsideSelection(): Boolean {
        val lineStart = findLineAt(mouseY) ?: return false
        return cursors.any { pair -> isInsideSelection(lineStart, pair.min, pair.max) }
    }

    private fun isInsideSelection(lineStart: LineStart, minCursor: Cursor, maxCursor: Cursor): Boolean {
        if (lineStart.lineIndex < minCursor.lineIndex || lineStart.lineIndex > maxCursor.lineIndex) {
            return false
        }

        val cursor = getCursorPosition(mouseX, lineStart)
        if (cursor !in minCursor..<maxCursor) {
            return false
        }

        val line = file.lines[lineStart.lineIndex]
        if (line.i0 == line.i1) return false // empty line

        val firstOffset = line.getOffset(line.i0 + lineStart.relI)
        val searched = (mouseX - lineStart.whereIIsDrawnX + firstOffset)
        if (searched <= 0) return false // too far left
        // could be a binary search, but it doesn't matter, because this will be at max width/charWidth
        for (i in line.i0 + lineStart.relI until line.i1) {
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

    fun copyContents(joined: List<String>) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val transferable = StringListTransferable(joined)
        clipboard.setContents(transferable, null)
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
        cursor1 = Cursor(cursor.lineIndex, cursor.relI + searched.text.length)
        scrollTo(cursor)
    }

    fun replaceSearchResult(cursor: Cursor) {
        showSearchResult(cursor)
        highLevelPaste(listOf(replaced.text))
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
                    val subLine = line.subLine(line.i0, line.i0 + cursor.relI)
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

            results.add(Cursor(lineIndex, i1 - line.i0))
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