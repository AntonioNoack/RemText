package me.anno.remtext

import me.anno.remtext.Controls.isLeftDown
import me.anno.remtext.Controls.mouseX
import me.anno.remtext.Controls.mouseY
import me.anno.remtext.Controls.scrollX
import me.anno.remtext.Controls.scrollY
import me.anno.remtext.Controls.searchResults
import me.anno.remtext.Window.window
import me.anno.remtext.Window.windowHeight
import me.anno.remtext.Window.windowWidth
import me.anno.remtext.editing.Cursor
import me.anno.remtext.editing.LineStart
import me.anno.remtext.font.Font
import me.anno.remtext.font.Font.lineHeight
import me.anno.remtext.gfx.Color
import me.anno.remtext.gfx.FlatColorShader
import me.anno.remtext.gfx.Quad
import me.anno.remtext.gfx.TextureColorShader
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL46C.glFinish
import org.lwjgl.opengl.GL46C.glGetError
import kotlin.math.max

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

    val widthI = IntArray(1)
    val heightI = IntArray(1)

    fun renderWindow() {

        val texShader = TextureColorShader()
        val flatShader = FlatColorShader()
        val quad = Quad()

        val renderer = FrameRenderer(texShader, flatShader, quad)

        // Main loop
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents()
            glfwGetWindowSize(window, widthI, heightI)

            renderer.drawFrame()

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

    fun findNextSearchIndex(lineIndex: Int, i: Int): Int {
        val searchResults = searchResults
        val line = file.lines[lineIndex]
        val searched = Cursor(lineIndex, i - line.i0)
        var index = binarySearch(0, searchResults.lastIndex) { idx ->
            searchResults[idx].compareTo(searched)
        }
        if (index < 0) index = max(-index - 1, 0)
        return index
    }

    fun findMaxScroll(width: Int, lineNumberOffset: Int) {
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

    @Suppress("SameParameterValue")
    inline fun binarySearch(minIndex: Int, maxIndex: Int, comparator: (index: Int) -> Int): Int {

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