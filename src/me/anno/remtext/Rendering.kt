package me.anno.remtext

import me.anno.remtext.Controls.draggingCursor
import me.anno.remtext.Controls.inputMode
import me.anno.remtext.Controls.isDraggingText
import me.anno.remtext.Controls.isLeftDown
import me.anno.remtext.Controls.mouseHasMoved
import me.anno.remtext.Controls.mouseX
import me.anno.remtext.Controls.mouseY
import me.anno.remtext.Controls.numHiddenLines
import me.anno.remtext.Controls.scrollX
import me.anno.remtext.Controls.scrollY
import me.anno.remtext.Controls.searchResults
import me.anno.remtext.Controls.searched
import me.anno.remtext.Window.isDarkTheme
import me.anno.remtext.Window.window
import me.anno.remtext.Window.windowHeight
import me.anno.remtext.Window.windowWidth
import me.anno.remtext.colors.Colors
import me.anno.remtext.editing.Cursor
import me.anno.remtext.editing.InputMode
import me.anno.remtext.editing.LineStart
import me.anno.remtext.editing.TextBox
import me.anno.remtext.font.Font
import me.anno.remtext.font.Font.lineHeight
import me.anno.remtext.font.Line
import me.anno.remtext.gfx.Color
import me.anno.remtext.gfx.FlatColorShader
import me.anno.remtext.gfx.Quad
import me.anno.remtext.gfx.TextureColorShader
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL46C.*
import kotlin.math.max
import kotlin.math.min

object Rendering {

    lateinit var file: OpenFile

    val bright = Color(0.9f, 0.9f, 0.9f)
    val brightSr = Color(0.75f, 0.75f, 0.9f)

    val dark = Color(0.1f, 0.1f, 0.15f)
    val darkSr = Color(0.3f, 0.3f, 0.37f)

    val middle = Color(0.5f, 0.5f, 0.6f)

    // for each visible line, add an entry here
    val lineStarts = ArrayList<LineStart>()

    var blink0 = System.nanoTime()

    val cursors
        get() = file.cursors

    fun gfxCheck() {
        val error = glGetError()
        if (error != 0) throw IllegalStateException()
    }

    var lastMaxLineWidth = 0L
    var lastNumLines = 1

    var maxScrollX = 0L
    var maxScrollY = 0L

    fun getMaxScrollX(width: Int, lineNumberOffset: Int): Long {
        return width + lineNumberOffset + lastMaxLineWidth - lineHeight * 3
    }

    fun getMaxScrollY(numLines: Int): Long {
        return max(numLines - 1, 0).toLong() * lineHeight
    }

    fun autoScrollOnBorder() {
        // scroll when cursor is at border & down
        if (isLeftDown) {
            val maxScroll = 10
            if (!file.wrapLines) {
                val scrollMx = maxScroll - mouseX
                if (scrollMx > 0) scrollX -= scrollMx

                val scrollPx = maxScroll - (windowWidth - 1 - mouseX)
                if (scrollPx > 0) scrollX += scrollPx
            }

            val scrollMy = maxScroll - mouseY
            if (scrollMy > 0) scrollY -= scrollMy

            val scrollPy = maxScroll - (windowHeight - 1 - mouseY)
            if (scrollPy > 0) scrollY += scrollPy
        }
    }

    fun renderWindow() {

        val texShader = TextureColorShader()
        val flatShader = FlatColorShader()
        val quad = Quad()

        val widthI = IntArray(1)
        val heightI = IntArray(1)

        // Main loop
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents()
            glfwGetWindowSize(window, widthI, heightI)

            val width = widthI[0]
            val height = heightI[0]
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glViewport(0, 0, width, height)
            windowWidth = width
            windowHeight = height

            val bgColor = if (isDarkTheme) dark else bright
            val srBgColor = if (isDarkTheme) darkSr else brightSr
            val textColor = if (isDarkTheme) bright else dark

            autoScrollOnBorder()

            glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f)
            glClear(GL_COLOR_BUFFER_BIT)
            glBindVertexArray(quad.vao)

            dx = 2f / width
            dy = 2f / height

            val showCursor0 = ((System.nanoTime() - blink0) / 500_000_000L).and(1) == 0L
            val showCursor = showCursor0 && inputMode == InputMode.TEXT //&& cursor0 == cursor1
            val showDraggingCursor = isDraggingText && mouseHasMoved

            flatShader.use()
            color4(flatShader.color, textColor, 1f)

            texShader.use()
            color3(texShader.textColor, textColor)
            color3(texShader.bgColor, bgColor)

            val file = file
            file.loading.value

            val lines = file.lines
            val charWidth = Font.getOffset('o', ' ')
            val lineNumberWidth = lines.size.toString().length + 2
            val lineNumberOffset = lineNumberWidth * charWidth
            Window.availableWidth = width - lineNumberOffset

            scrollX = max(min(scrollX, maxScrollX), 0)
            scrollY = max(min(scrollY, maxScrollY), 0)

            lineStarts.clear()

            var numLines = 0
            var maxLineWidth = 0

            var y = -scrollY + numHiddenLines * lineHeight
            val minY0 = numHiddenLines * lineHeight - 5 // -5, so arrow-up works

            var lastCharX = lineNumberOffset
            var lastCharY = 0L

            val searchResults = searchResults
            var nextSearchIndex = searchResults.size
            val searchedLength = searched.text.length

            fun drawCursor(x0: Int, y: Int) {
                flatShader.use()
                color4(flatShader.color, middle, 1f)
                drawQuad(flatShader.bounds, x0, y, 1, lineHeight)
                color4(flatShader.color, textColor, 1f)
                texShader.use()
            }

            var wasSelected = false
            fun fillSelectionBg(x: Int, color: Color) {
                flatShader.use()
                color4(flatShader.color, color, 1f)
                drawQuad(flatShader.bounds, lastCharX, y, x - lastCharX, lineHeight)
                texShader.use()
            }

            fun onChar(line: Line, lineIndex: Int, i: Int, x: Int, width: Int) {
                val relI = i - line.i0
                val isSelected = cursors.any { pair -> pair.isSelected(lineIndex, relI) }
                if (isSelected != wasSelected) {
                    if (isSelected) {
                        lastCharX = x
                        lastCharY = y
                    }
                    wasSelected = isSelected
                }

                val searchResult = searchResults.getOrNull(nextSearchIndex)
                val isSearchResult = searchResult != null && searchResult.lineIndex == lineIndex
                        && relI in searchResult.relI until searchResult.relI + searchedLength

                val textColorI =
                    if (isSelected) bgColor
                    else if (line.colors != null) Colors[line.colors[i]]
                    else textColor

                val bgColorI =
                    if (isSelected) textColor
                    else if (isSearchResult) srBgColor
                    else bgColor

                color3(texShader.bgColor, bgColorI)
                color3(texShader.textColor, textColorI)

                if (searchResult != null && relI >= searchResult.relI + searchedLength) {
                    nextSearchIndex++
                }

                if (lastCharY != y) {
                    lastCharX = x
                    lastCharY = y
                }

                if (isSelected) {
                    fillSelectionBg(x + width, bgColorI)
                } else {

                    if (isSearchResult) {
                        fillSelectionBg(x + width, bgColorI)
                    }

                    if (showCursor && cursors.any { it.show(lineIndex, relI) }) {
                        drawCursor(x, y.toInt())
                    } else if (
                        showDraggingCursor &&
                        lineIndex == draggingCursor.lineIndex &&
                        relI == draggingCursor.relI
                    ) {
                        drawCursor(x, y.toInt())
                    }
                }

                lastCharX = x + width
            }

            // line up/down won't work, when not all lines are on screen, which CAN be the case when multi-line editing is used...
            //  -> when that is the case, we need to always draw the respective lines, too... cursors.map{it.second}
            //  -> todo we should find a cleaner way
            val uniqueCursorLineIndices0 =
                cursors.map { it.second.lineIndex }.toSet()
            val uniqueCursorLineIndices = uniqueCursorLineIndices0 +
                    uniqueCursorLineIndices0.map { it + 1 } + // for cursor-down
                    uniqueCursorLineIndices0.map { it - 1 } // for cursor-up
            val maxCursorLineIndex = uniqueCursorLineIndices.maxOrNull() ?: -1

            lines@ for (lineIndex in lines.indices) {
                val line = lines[lineIndex]
                val text = line.text
                maxLineWidth = max(maxLineWidth, line.getOffset(line.i1))

                val y0 = y
                if (file.wrapLines) {

                    val wrappedLines = line.getNumLines(width - lineNumberOffset)
                    val drawnHeight = wrappedLines * lineHeight
                    val isVisible = y < height && y + drawnHeight >= minY0
                    val isSpecialLine = !isVisible && lineIndex in uniqueCursorLineIndices
                    if (isVisible || isSpecialLine) {

                        lineStarts.add(LineStart(0, lineIndex, lineNumberOffset, y.toInt()))
                        nextSearchIndex = findNextSearchIndex(lineIndex, line.i0)

                        // draw text
                        var dxi = 0
                        line@ for (i in line.i0 until line.i1) {
                            val curr = text[i]
                            val x = line.getOffset(i) + lineNumberOffset
                            if (curr == ' ') {
                                onChar(line, lineIndex, i, x - dxi, Font.spaceWidth)
                                continue
                            }

                            val texWidth = line.getWidth(i)
                            if (x > dxi && x - dxi + texWidth > width) {
                                dxi = x - lineNumberOffset
                                y += lineHeight
                                numLines++
                                if (y >= height) break@line
                                lineStarts.add(LineStart(i - line.i0, lineIndex, x - dxi, y.toInt()))
                            }

                            if (isVisible) {
                                val tex = Font.getTexture(curr)
                                if (y + tex.height >= minY0) {
                                    onChar(line, lineIndex, i, x - dxi, tex.width)
                                    glBindTexture(GL_TEXTURE_2D, tex.pointer)
                                    drawQuad(texShader.bounds, x - dxi, y, tex.width, tex.height)
                                }
                            }
                        }

                        if (y + lineHeight >= minY0 && y < height &&
                            ((showCursor && file.cursors.any { it.showEOL(lineIndex, line.length) }) ||
                                    (showDraggingCursor && lineIndex == draggingCursor.lineIndex && draggingCursor.relI >= line.length))
                        ) {
                            val x = line.getOffset(line.i1) + lineNumberOffset - dxi
                            drawCursor(x, y.toInt())
                        }

                    } else {
                        // +1 is added later
                        numLines += (wrappedLines - 1)
                        y += lineHeight * (wrappedLines - 1)
                    }
                } else {
                    val isVisible = y < height && y + lineHeight >= minY0
                    val isSpecialLine = !isVisible && lineIndex in uniqueCursorLineIndices
                    if (isVisible || isSpecialLine) {

                        // draw text
                        val dxi = lineNumberOffset - scrollX.toInt()
                        lineStarts.add(LineStart(0, lineIndex, dxi, y.toInt()))
                        if (!isVisible) continue

                        // find the first character to be rendered
                        var i0 = binarySearch(0, line.i1 - line.i0) { idx ->
                            line.getOffset(idx + line.i0) + dxi
                        }
                        if (i0 < 0) i0 = -i0 - 2
                        i0 = line.i0 + max(0, i0)

                        nextSearchIndex = findNextSearchIndex(lineIndex, i0)

                        line@ for (i in i0 until line.i1) {
                            val curr = text[i]
                            val x = line.getOffset(i) + dxi
                            if (x >= width) break@line
                            if (curr == ' ') {
                                onChar(line, lineIndex, i, x, Font.spaceWidth)
                                continue
                            }

                            val tex = Font.getTexture(curr)
                            if (x + tex.width > 0) {
                                onChar(line, lineIndex, i, x, tex.width)
                                glBindTexture(GL_TEXTURE_2D, tex.pointer)
                                drawQuad(texShader.bounds, x, y, tex.width, tex.height)
                            }
                        }

                        if (y + lineHeight >= minY0 && y < height &&
                            ((showCursor && file.cursors.any { it.showEOL(lineIndex, line.length) }) ||
                                    (showDraggingCursor && lineIndex == draggingCursor.lineIndex && draggingCursor.relI >= line.length))
                        ) {
                            val x = line.getOffset(line.i1) + lineNumberOffset
                            drawCursor(x, y.toInt())
                        }
                    }
                }

                if (y0 < height && y0 + lineHeight > minY0) {
                    drawLineNumber(
                        flatShader, texShader, y0.toInt(), lineNumberOffset,
                        lineIndex, charWidth, textColor, bgColor
                    )
                    wasSelected = false
                }

                y += lineHeight
                numLines++

                if (y >= height && lineIndex >= maxCursorLineIndex) break@lines
                // else we need to continue drawing/checking lines
            }

            // show the current search & replacement term
            // show the number of search results

            fun drawText(
                textBox: TextBox,
                x: Int, y: Int,
                ifEmpty: String
            ) {
                val isEmpty = textBox.text.isEmpty()
                texShader.use()
                color3(texShader.textColor, if (isEmpty) middle else textColor)
                color3(texShader.bgColor, bgColor)
                var x = x
                val text = textBox.text.ifEmpty { ifEmpty }
                val cursor0 = if (isEmpty) ifEmpty.length else textBox.cursor0
                val cursor1 = if (isEmpty) ifEmpty.length else textBox.cursor1
                for (i in text.indices) {
                    val char = text[i]
                    if (char != ' ') {
                        val tex = Font.getTexture(char)
                        glBindTexture(GL_TEXTURE_2D, tex.pointer)
                        drawQuad(texShader.bounds, x, y, tex.width, tex.height)
                    }
                    if (if (i == cursor1) showCursor0 else i == cursor0) {
                        drawCursor(x, y)
                    }
                    val nextChar = if (i + 1 < text.length) text[i + 1] else ' '
                    x += Font.getOffset(char, nextChar)
                }

                val i = text.length
                if (if (i == cursor1) showCursor0 else i == cursor0) {
                    drawCursor(x, y)
                }
            }

            fun drawTextRight(text: String, x: Int, y: Int, textColor: Color) {
                texShader.use()
                color3(texShader.textColor, textColor)
                color3(texShader.bgColor, bgColor)
                var x = x
                for (i in text.indices.reversed()) {
                    val char = text[i]
                    val nextChar = if (i + 1 < text.length) text[i + 1] else ' '
                    x -= Font.getOffset(char, nextChar)
                    if (char != ' ') {
                        val tex = Font.getTexture(char)
                        glBindTexture(GL_TEXTURE_2D, tex.pointer)
                        drawQuad(texShader.bounds, x, y, tex.width, tex.height)
                    }
                }
            }

            if (inputMode != InputMode.TEXT) {
                flatShader.use()
                color4(flatShader.color, bgColor, 1f)
                drawQuad(flatShader.bounds, 0, 0, width, lineHeight)

                drawText(searched, 0, 0, "Search: ")

                val numResults = Controls.searchResults.size
                val index = min(Controls.shownSearchResult + 1, numResults)
                drawTextRight("$index/$numResults", width, 0, middle)

                flatShader.use()
                color4(flatShader.color, middle, 1f)
                drawQuad(flatShader.bounds, 0, lineHeight, width, 1)

                if (inputMode != InputMode.SEARCH_ONLY) {
                    color4(flatShader.color, bgColor, 1f)
                    drawQuad(flatShader.bounds, 0, lineHeight + 1, width, lineHeight - 1)

                    drawText(Controls.replaced, 0, lineHeight, "Replace: ")

                    flatShader.use()
                    color4(flatShader.color, middle, 1f)
                    drawQuad(flatShader.bounds, 0, lineHeight * 2, width, 1)
                }
            }

            lastNumLines = numLines
            lastMaxLineWidth = maxLineWidth.toLong()

            findMaxScroll(width, lineNumberOffset)
            drawScrollBars(flatShader, width, height, textColor)

            glFinish()
            glfwSwapBuffers(window)
        }

        glfwHideWindow(window)

        // Cleanup
        Font.destroyTextures()
        texShader.destroy()
        quad.destroy()
        glfwDestroyWindow(window)
        glfwTerminate()
    }

    private fun findNextSearchIndex(lineIndex: Int, i: Int): Int {
        val searchResults = searchResults
        val line = file.lines[lineIndex]
        val searched = Cursor(lineIndex, i - line.i0)
        var index = binarySearch(0, searchResults.lastIndex) { idx ->
            searchResults[idx].compareTo(searched)
        }
        if (index < 0) index = max(-index - 1, 0)
        return index
    }

    private fun drawLineNumber(
        flatShader: FlatColorShader, texShader: TextureColorShader,
        y0: Int, lineNumberOffset: Int,
        lineIndex: Int, charWidth: Int,
        textColor: Color, bgColor: Color,
    ) {
        if (scrollX > 0) {
            // clear left side
            flatShader.use()
            color4(flatShader.color, bgColor, 1f)
            drawQuad(flatShader.bounds, 0, y0, lineNumberOffset, lineHeight)
            color4(flatShader.color, textColor, 1f) // for selection background
        }

        texShader.use()
        color3(texShader.bgColor, bgColor)
        color3(texShader.textColor, middle)
        var dx0 = lineNumberOffset - 2 * charWidth
        var remainder = lineIndex + 1
        while (remainder > 0) {
            val curr = '0' + (remainder % 10)
            val tex = Font.getTexture(curr)
            glBindTexture(GL_TEXTURE_2D, tex.pointer)
            drawQuad(texShader.bounds, dx0, y0, tex.width, tex.height)
            dx0 -= charWidth
            remainder /= 10
        }
        color3(texShader.textColor, textColor)
    }

    private fun findMaxScroll(width: Int, lineNumberOffset: Int) {
        val lines = file.lines
        val availableWidth = width - lineNumberOffset

        val totalNumLines = if (availableWidth != countedLinesW) {
            countedLinesW = availableWidth
            val count = lines.indices.sumOf { li -> lines[li].getNumLines(availableWidth) }
            countedLinesAtW = count
            count
        } else countedLinesAtW

        maxScrollY = getMaxScrollY(totalNumLines)
        maxScrollX = if (file.wrapLines) 0 else getMaxScrollX(width, lineNumberOffset)
    }

    var countedLinesAtW = 0
    var countedLinesW = -1

    val barWidth = 10

    private fun drawScrollBars(
        flatShader: FlatColorShader,
        width: Int, height: Int, color: Color
    ) {
        glEnable(GL_BLEND)
        glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD)
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)

        val minBarLength = 4
        if (maxScrollX > 0) {
            // show scrollbar x
            val barLength = max(minBarLength, (width * width) / (width + maxScrollX.toInt()))
            val barPos = ((width - barLength) * scrollX / maxScrollX).toInt()
            flatShader.use()
            val alpha = if (mouseY >= height - barWidth) 0.5f else 0.3f
            color4(flatShader.color, color, alpha)
            drawQuad(flatShader.bounds, barPos, height - barWidth, barLength, barWidth)
        }

        if (maxScrollY > 0) {
            // show scrollbar y
            val barLength = max(minBarLength, ((height * height) / (height + maxScrollY)).toInt())
            val barPos = ((height - barLength) * scrollY / maxScrollY).toInt()
            flatShader.use()
            val alpha = if (mouseX >= width - barWidth) 0.5f else 0.3f
            color4(flatShader.color, color, alpha)
            drawQuad(flatShader.bounds, width - barWidth, barPos, barWidth, barLength)
        }
        glDisable(GL_BLEND)
    }

    private var dx = 1f
    private var dy = 1f

    private fun color3(i: Int, color: Color) {
        glUniform3f(i, color.r, color.g, color.b)
    }

    private fun color4(i: Int, color: Color, alpha: Float) {
        glUniform4f(i, color.r, color.g, color.b, alpha)
    }

    private fun drawQuad(bounds: Int, x: Int, y: Int, w: Int, h: Int) {
        val dyi = h * dy
        glUniform4f(
            bounds,
            w * dx, dyi,
            x * dx - 1f, -y * dy - dyi + 1f
        )
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun drawQuad(bounds: Int, x: Int, y: Long, w: Int, h: Int) {
        drawQuad(bounds, x, y.toInt(), w, h)
    }

    @Suppress("SameParameterValue")
    private inline fun binarySearch(minIndex: Int, maxIndex: Int, comparator: (index: Int) -> Int): Int {

        var min = minIndex
        var max = maxIndex

        while (max >= min) {
            val mid = (min + max).ushr(1)
            val cmp = comparator(mid)
            if (cmp == 0) return mid
            if (cmp < 0) {
                // right
                min = mid + 1
            } else {
                // left
                max = mid - 1
            }
        }
        return -1 - min
    }

}