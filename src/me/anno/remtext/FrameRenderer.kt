package me.anno.remtext

import me.anno.remtext.Controls.draggingCursor
import me.anno.remtext.Controls.inputMode
import me.anno.remtext.Controls.isDraggingText
import me.anno.remtext.Controls.mouseHasMoved
import me.anno.remtext.Controls.mouseX
import me.anno.remtext.Controls.mouseY
import me.anno.remtext.Controls.numHiddenLines
import me.anno.remtext.Controls.scrollX
import me.anno.remtext.Controls.scrollY
import me.anno.remtext.Controls.searchResults
import me.anno.remtext.Controls.searched
import me.anno.remtext.Rendering.autoScrollOnBorder
import me.anno.remtext.Rendering.barWidth
import me.anno.remtext.Rendering.binarySearch
import me.anno.remtext.Rendering.blink0
import me.anno.remtext.Rendering.bright
import me.anno.remtext.Rendering.brightSr
import me.anno.remtext.Rendering.cursors
import me.anno.remtext.Rendering.dark
import me.anno.remtext.Rendering.darkSr
import me.anno.remtext.Rendering.file
import me.anno.remtext.Rendering.findMaxScroll
import me.anno.remtext.Rendering.findNextSearchIndex
import me.anno.remtext.Rendering.heightI
import me.anno.remtext.Rendering.lastMaxLineWidth
import me.anno.remtext.Rendering.lastNumLines
import me.anno.remtext.Rendering.lineStarts
import me.anno.remtext.Rendering.maxScrollX
import me.anno.remtext.Rendering.maxScrollY
import me.anno.remtext.Rendering.middle
import me.anno.remtext.Rendering.widthI
import me.anno.remtext.Window.isDarkTheme
import me.anno.remtext.Window.windowHeight
import me.anno.remtext.Window.windowWidth
import me.anno.remtext.blocks.BlockStyle.Companion.canCollapseBlock
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
import org.lwjgl.opengl.GL46C.*
import kotlin.math.max
import kotlin.math.min

class FrameRenderer(
    private val texShader: TextureColorShader,
    private val flatShader: FlatColorShader,
    private val quad: Quad
) {

    fun drawFrame() {

        val width = widthI[0]
        val height = heightI[0]

        updateViewport()
        updateColors()

        autoScrollOnBorder()

        clearScreen()
        glBindVertexArray(quad.vao)

        dx = 2f / width
        dy = 2f / height

        updateCursorVisibility()

        bindFlatColor()
        bindTexColor()

        val file = file
        file.loading.value // ensure file loads

        val lines = file.lines
        val charWidth = Font.getOffset('o', ' ')
        val lineNumberWidth = lines.size.toString().length + 2
        val lineNumberOffset = lineNumberWidth * charWidth
        val collapsedBlocks = file.collapsedBlocks
        val blockStyle = file.language?.getBlockStyle()
        Window.availableWidth = width - lineNumberOffset

        prepareState(lineNumberOffset)

        // line up/down won't work, when not all lines are on screen, which CAN be the case when multi-line editing is used...
        //  -> when that is the case, we need to always draw the respective lines, too... cursors.map{it.second}
        val uniqueCursorLineIndices = calculateCursorLineIndices()
        val maxCursorLineIndex = uniqueCursorLineIndices.maxOrNull() ?: -1

        var maxLineWidth = 0
        lines@ for (lineIndex in lines.indices) {
            val line = lines[lineIndex]

            // calculate whether we must skip a line
            val isCollapsedLine = collapsedBlocks.any { block -> block.isCollapsed(lineIndex) }
            if (isCollapsedLine) continue

            val canCollapseBlock = blockStyle.canCollapseBlock(lines, lineIndex)
            maxLineWidth = max(maxLineWidth, line.getOffset(line.i1))

            val y0 = y
            if (file.wrapLines) {
                val numWrappedLines = line.getNumLines(width - lineNumberOffset)
                val drawnHeight = numWrappedLines * lineHeight
                val isVisible = y < height && y + drawnHeight >= minY0
                val isSpecialLine = !isVisible && lineIndex in uniqueCursorLineIndices
                if (isVisible || isSpecialLine) {
                    drawWrappingLine(lineIndex, lineNumberOffset, line, width, height, isVisible, canCollapseBlock)
                } else {
                    skipWrappingLine(numWrappedLines)
                }
            } else {
                val isVisible = y < height && y + lineHeight >= minY0
                val isSpecialLine = !isVisible && lineIndex in uniqueCursorLineIndices
                if (isVisible || isSpecialLine) {
                    drawSimpleLine(lineIndex, lineNumberOffset, line, width, height, isVisible, canCollapseBlock)
                }
            }

            handleLineNumber(y0, height, lineIndex, lineNumberOffset, charWidth, canCollapseBlock)
            nextLine()

            if (y >= height && lineIndex >= maxCursorLineIndex) break@lines
            // else we need to continue drawing/checking lines
        }

        drawSearchWindow(width)

        updateLineStats(maxLineWidth)

        findMaxScroll(width, lineNumberOffset)
        drawScrollBars(flatShader, width, height, textColor)
    }

    private var bgColor = dark
    private var srBgColor = darkSr
    private var textColor = bright

    private fun drawCursor(x0: Int, y: Int) {
        flatShader.use()
        color4(flatShader.color, middle, 1f)
        drawQuad(flatShader.bounds, x0, y, 1, lineHeight)
        color4(flatShader.color, textColor, 1f)
        texShader.use()
    }

    private fun updateColors() {
        bgColor = if (isDarkTheme) dark else bright
        srBgColor = if (isDarkTheme) darkSr else brightSr
        textColor = if (isDarkTheme) bright else dark
    }

    private fun updateViewport() {
        val width = widthI[0]
        val height = heightI[0]
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glViewport(0, 0, width, height)
        windowWidth = width
        windowHeight = height
    }

    private fun updateCursorVisibility() {
        showCursor0 = ((System.nanoTime() - blink0) / 500_000_000L).and(1) == 0L
        showCursor = showCursor0 && inputMode == InputMode.TEXT //&& cursor0 == cursor1
        showDraggingCursor = isDraggingText && mouseHasMoved
    }

    private fun clearScreen() {
        val color = bgColor
        glClearColor(color.r, color.g, color.b, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
    }

    private var y = 0L
    private var minY0 = 0

    private var lastCharX = 0
    private var lastCharY = 0L

    private var wasSelected = false

    private var nextSearchIndex = 0
    private var searchedLength = 0

    private var showCursor0 = false
    private var showCursor = false
    private var showDraggingCursor = false

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

    private fun fillSelectionBg(x: Int, color: Color) {
        flatShader.use()
        color4(flatShader.color, color, 1f)
        drawQuad(flatShader.bounds, lastCharX, y, x - lastCharX, lineHeight)
        texShader.use()
    }

    private fun onChar(line: Line, lineIndex: Int, i: Int, x: Int, width: Int) {
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

    private fun drawText(textBox: TextBox, y: Int, ifEmpty: String) {
        val isEmpty = textBox.text.isEmpty()
        texShader.use()
        color3(texShader.textColor, if (isEmpty) middle else textColor)
        color3(texShader.bgColor, bgColor)
        var x = 0
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

    private fun drawTextRight(text: String, x: Int, textColor: Color) {
        texShader.use()
        color3(texShader.textColor, textColor)
        color3(texShader.bgColor, bgColor)
        var x = x
        val y = 0
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

    private fun drawLineNumber(
        y0: Int, lineNumberOffset: Int,
        lineIndex: Int, charWidth: Int,
        canCollapse: Boolean
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
        if (canCollapse) {
            val isCollapsed = file.collapsedBlocks.any { it.startIndex == lineIndex }
            val tex = Font.getTexture(if (isCollapsed) '+' else '-')
            glBindTexture(GL_TEXTURE_2D, tex.pointer)
            drawQuad(texShader.bounds, 2, y0, tex.width, tex.height)
        }

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

    private fun calculateCursorLineIndices(): Set<Int> {
        val uniqueCursorLineIndices0 =
            cursors.map { it.second.lineIndex }.toSet()
        return uniqueCursorLineIndices0 +
                uniqueCursorLineIndices0.map { it + 1 } + // for cursor-down
                uniqueCursorLineIndices0.map { it - 1 } // for cursor-up
    }

    private var dx = 1f
    private var dy = 1f

    private var numLines = 0

    private fun bindFlatColor() {
        flatShader.use()
        color4(flatShader.color, textColor, 1f)
    }

    private fun bindTexColor() {
        texShader.use()
        color3(texShader.textColor, textColor)
        color3(texShader.bgColor, bgColor)
    }

    private fun prepareState(lineNumberOffset: Int) {
        scrollX = max(min(scrollX, maxScrollX), 0)
        scrollY = max(min(scrollY, maxScrollY), 0)

        lineStarts.clear()

        numLines = 0

        y = -scrollY + numHiddenLines * lineHeight
        minY0 = numHiddenLines * lineHeight - 5 // -5, so arrow-up works

        lastCharX = lineNumberOffset
        lastCharY = 0L

        nextSearchIndex = searchResults.size
        searchedLength = searched.text.length

        wasSelected = false
    }

    private fun updateLineStats(maxLineWidth: Int) {
        lastNumLines = numLines
        lastMaxLineWidth = maxLineWidth.toLong()
    }

    private fun handleLineNumber(
        y0: Long, height: Int, lineIndex: Int, lineNumberOffset: Int,
        charWidth: Int, canCollapse: Boolean
    ) {
        if (y0 < height && y0 + lineHeight > minY0) {
            drawLineNumber(
                y0.toInt(), lineNumberOffset,
                lineIndex, charWidth, canCollapse
            )
            wasSelected = false
        }
    }

    private fun nextLine() {
        y += lineHeight
        numLines++
    }

    private fun skipWrappingLine(numWrappedLines: Int) {
        // +1 is added later
        numLines += (numWrappedLines - 1)
        y += lineHeight * (numWrappedLines - 1)
    }

    private fun drawSimpleLine(
        lineIndex: Int, lineNumberOffset: Int, line: Line,
        width: Int, height: Int, isVisible: Boolean,
        canCollapse: Boolean
    ) {
        val text = line.text
        // draw text
        val dxi = lineNumberOffset - scrollX.toInt()
        lineStarts.add(LineStart(0, lineIndex, dxi, y.toInt(), canCollapse))
        if (!isVisible) return

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

    private fun drawWrappingLine(
        lineIndex: Int, lineNumberOffset: Int, line: Line,
        width: Int, height: Int, isVisible: Boolean,
        canCollapse: Boolean
    ) {
        val text = line.text
        lineStarts.add(LineStart(0, lineIndex, lineNumberOffset, y.toInt(), canCollapse))
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
                val relI = i - line.i0
                val whereIIsDrawnX = x - dxi
                lineStarts.add(LineStart(relI, lineIndex, whereIIsDrawnX, y.toInt(), false))
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
    }

    /**
     * show the current search & replacement term
     * show the number of search results
     * */
    private fun drawSearchWindow(width: Int) {
        if (inputMode == InputMode.TEXT) return

        flatShader.use()
        color4(flatShader.color, bgColor, 1f)
        drawQuad(flatShader.bounds, 0, 0, width, lineHeight)

        drawText(searched, 0, "Search: ")

        val numResults = searchResults.size
        val index = min(Controls.shownSearchResult + 1, numResults)
        drawTextRight("$index/$numResults", width, middle)

        flatShader.use()
        color4(flatShader.color, middle, 1f)
        drawQuad(flatShader.bounds, 0, lineHeight, width, 1)

        if (inputMode != InputMode.SEARCH_ONLY) {
            color4(flatShader.color, bgColor, 1f)
            drawQuad(flatShader.bounds, 0, lineHeight + 1, width, lineHeight - 1)

            drawText(Controls.replaced, lineHeight, "Replace: ")

            flatShader.use()
            color4(flatShader.color, middle, 1f)
            drawQuad(flatShader.bounds, 0, lineHeight * 2, width, 1)
        }
    }

}
