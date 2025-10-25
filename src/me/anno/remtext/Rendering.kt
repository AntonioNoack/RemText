package me.anno.remtext

import me.anno.remtext.Controls.inputMode
import me.anno.remtext.Controls.mouseX
import me.anno.remtext.Controls.mouseY
import me.anno.remtext.Controls.numHiddenLines
import me.anno.remtext.Controls.scrollX
import me.anno.remtext.Controls.scrollY
import me.anno.remtext.Window.isDarkTheme
import me.anno.remtext.Window.window
import me.anno.remtext.editing.Cursor
import me.anno.remtext.editing.InputMode
import me.anno.remtext.editing.LineStart
import me.anno.remtext.editing.TextBox
import me.anno.remtext.font.Font
import me.anno.remtext.font.Font.lineHeight
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
    val dark = Color(0.1f, 0.1f, 0.15f)
    val middle = Color(0.5f, 0.5f, 0.6f)

    // for each visible line, add an entry here
    val lineStarts = ArrayList<LineStart>()

    var blink0 = System.nanoTime()

    var cursor0: Cursor
        get() = file.cursor0
        set(value) {
            file.cursor0 = value
        }

    var cursor1: Cursor
        get() = file.cursor1
        set(value) {
            file.cursor1 = value
        }

    fun gfxCheck() {
        val error = glGetError()
        if (error != 0) throw IllegalStateException()
    }

    fun minI(c0: Cursor, c1: Cursor): Cursor {
        return if (c0.lineIndex == c1.lineIndex) {
            if (c0.i < c1.i) c0 else c1
        } else if (c0.lineIndex < c1.lineIndex) {
            c0
        } else c1
    }

    fun maxI(c0: Cursor, c1: Cursor): Cursor {
        val min = minI(c0, c1)
        return if (c0 == min) c1 else c0
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
            Window.windowWidth = width
            Window.windowHeight = height

            val bgColor = if (isDarkTheme) dark else bright
            val textColor = if (isDarkTheme) bright else dark

            glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f)
            glClear(GL_COLOR_BUFFER_BIT)
            glBindVertexArray(quad.vao)

            dx = 2f / width
            dy = 2f / height

            val showCursor0 = ((System.nanoTime() - blink0) / 500_000_000L).and(1) == 0L
            val showCursor = showCursor0 && inputMode == InputMode.TEXT && cursor0 == cursor1

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

            val minCursor = minI(cursor0, cursor1)
            val maxCursor = maxI(cursor0, cursor1)

            var y = -scrollY + numHiddenLines * lineHeight

            var selectionStartX = lineNumberOffset
            var selectionStartY = 0L

            // todo faintly show all search results...
            //  it could be lots and lots...
            //  -> binary search over them, and only show what's visible

            fun drawCursor(x0: Int, y: Int) {
                flatShader.use()
                color4(flatShader.color, middle, 1f)
                drawQuad(flatShader.bounds, x0, y, 1, lineHeight)
                color4(flatShader.color, textColor, 1f)
                texShader.use()
            }

            var wasSelected = false
            fun fillSelectionBg(x: Int) {
                flatShader.use()
                drawQuad(flatShader.bounds, selectionStartX, y, x - selectionStartX, lineHeight)
                texShader.use()
            }

            fun onChar(lineIndex: Int, i: Int, x: Int, width: Int) {
                val isSelected =
                    if (lineIndex in minCursor.lineIndex..maxCursor.lineIndex) {
                        if (lineIndex == minCursor.lineIndex && lineIndex == maxCursor.lineIndex) {
                            i in minCursor.i until maxCursor.i
                        } else if (lineIndex == minCursor.lineIndex) {
                            i >= minCursor.i
                        } else if (lineIndex == maxCursor.lineIndex) {
                            i < maxCursor.i
                        } else true // in-between
                    } else false

                if (isSelected != wasSelected) {
                    if (isSelected) {
                        selectionStartX = x
                        selectionStartY = y
                        color3(texShader.bgColor, textColor)
                        color3(texShader.textColor, bgColor)
                    } else {
                        color3(texShader.textColor, textColor)
                        color3(texShader.bgColor, bgColor)
                    }
                    wasSelected = isSelected
                }
                if (isSelected) {
                    if (selectionStartY != y) {
                        selectionStartX = x
                        selectionStartY = y
                    }
                    fillSelectionBg(x + width)
                    selectionStartX = x + width
                } else if (showCursor &&
                    lineIndex == cursor0.lineIndex &&
                    i == cursor1.i
                ) {
                    drawCursor(x, y.toInt())
                }
            }

            lines@ for (lineIndex in lines.indices) {
                val line = lines[lineIndex]
                val text = line.text
                maxLineWidth = max(maxLineWidth, line.getOffset(line.i1))

                val y0 = y
                if (file.wrapLines) {

                    val wrappedLines = line.getNumLines(width - lineNumberOffset)
                    val drawnHeight = wrappedLines * lineHeight
                    if (y < height && y + drawnHeight > 0) {

                        lineStarts.add(LineStart(line.i0, lineIndex, lineNumberOffset, y.toInt()))

                        // draw text
                        var dxi = 0
                        line@ for (i in line.i0 until line.i1) {
                            val curr = text[i]
                            val x = line.getOffset(i) + lineNumberOffset
                            if (curr == ' ') {
                                onChar(lineIndex, i, x - dxi, Font.spaceWidth)
                                continue
                            }

                            val texWidth = line.getWidth(i)
                            if (x > dxi && x - dxi + texWidth > width) {
                                dxi = x - lineNumberOffset
                                y += lineHeight
                                numLines++
                                if (y >= height) break@line
                                lineStarts.add(LineStart(i, lineIndex, x - dxi, y.toInt()))
                            }

                            val tex = Font.getTexture(curr)
                            if (y + tex.height > 0) {
                                onChar(lineIndex, i, x - dxi, tex.width)
                                glBindTexture(GL_TEXTURE_2D, tex.pointer)
                                drawQuad(texShader.bounds, x - dxi, y, tex.width, tex.height)
                            }
                        }

                        if (showCursor && y + lineHeight >= 0 && y < height && lineIndex == cursor0.lineIndex && cursor0.i >= line.i1) {
                            val x = line.getOffset(line.i1) + lineNumberOffset - dxi
                            drawCursor(x, y.toInt())
                        }

                    } else {
                        // +1 is added later
                        numLines += (wrappedLines - 1)
                        y += lineHeight * (wrappedLines - 1)
                    }
                } else if (y < height && y + lineHeight > 0) {

                    // draw text
                    val dxi = lineNumberOffset - scrollX.toInt()
                    lineStarts.add(LineStart(line.i0, lineIndex, dxi, y.toInt()))

                    // find the first character to be rendered
                    var i0 = binarySearch(0, line.i1 - line.i0) { idx ->
                        line.getOffset(idx + line.i0) + dxi
                    }
                    if (i0 < 0) i0 = -i0 - 2
                    i0 = line.i0 + max(0, i0)

                    line@ for (i in i0 until line.i1) {
                        val curr = text[i]
                        val x = line.getOffset(i) + dxi
                        if (x >= width) break@line
                        if (curr == ' ') {
                            onChar(lineIndex, i, x, Font.spaceWidth)
                            continue
                        }

                        val tex = Font.getTexture(curr)
                        if (x + tex.width > 0) {
                            onChar(lineIndex, i, x, tex.width)
                            glBindTexture(GL_TEXTURE_2D, tex.pointer)
                            drawQuad(texShader.bounds, x, y, tex.width, tex.height)
                        }
                    }

                    if (showCursor && y + lineHeight >= 0 && y < height && lineIndex == cursor0.lineIndex && cursor0.i >= line.i1) {
                        val x = line.getOffset(line.i1) + lineNumberOffset
                        drawCursor(x, y.toInt())
                    }
                }

                if (y0 < height && y0 + lineHeight > 0) {
                    drawLineNumber(
                        flatShader, texShader, y0.toInt(), lineNumberOffset,
                        lineIndex, charWidth, textColor, bgColor
                    )
                    wasSelected = false
                }

                // todo show current line number and cursor position faintly

                y += lineHeight
                numLines++
                if (y >= height) break@lines
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

                drawText(Controls.searched, 0, 0, "Search: ")

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