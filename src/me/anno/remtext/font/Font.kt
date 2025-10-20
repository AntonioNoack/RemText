package me.anno.remtext.font

import me.anno.remtext.gfx.PixelData
import me.anno.remtext.gfx.Texture
import org.lwjgl.opengl.GL11C.glDeleteTextures
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

object Font {

    private val awtFont = Font("Verdana", 0, 15)
    private val renderContext = FontRenderContext(null, true, true)

    val baselineY: Int
    val lineHeight: Int

    init {
        val exampleLayout = TextLayout("o", awtFont, renderContext)
        baselineY = exampleLayout.ascent.toInt()
        lineHeight = (exampleLayout.ascent + exampleLayout.descent).toInt()
    }

    private val textures = arrayOfNulls<Texture>(65535)

    private const val ASCII0 = 32
    private const val NUM_ASCIIS = 128 - ASCII0

    private val asciiOffsets = IntArray(NUM_ASCIIS * NUM_ASCIIS)
    private val otherOffsets = HashMap<Int, Int>()

    fun getTexture(char: Char): Texture {
        var texture = textures[char.code]
        if (texture != null) return texture
        texture = createTexture(char)
        textures[char.code] = texture
        return texture
    }

    // |ab| - |b|
    fun getOffset(charA: Char, charB: Char): Int {
        val c0 = charA.code - ASCII0
        val c1 = charB.code - ASCII0
        if (c0 in 0 until NUM_ASCIIS && c1 in 0 until NUM_ASCIIS) {
            val index = c0 + c1 * NUM_ASCIIS
            var width = asciiOffsets[index]
            if (width == 0) {
                width = calculateOffset(charA, charB)
                asciiOffsets[index] = width
            }
            return width
        }
        val index = charA.code.shl(16) + charB.code
        return otherOffsets.getOrPut(index) {
            calculateOffset(charA, charB)
        }
    }

    private fun createTexture(char: Char): Texture {

        val width = getOffset(char, ' ') + 1

        val image = BufferedImage(width, lineHeight, BufferedImage.TYPE_INT_RGB)
        val gfx = image.graphics as Graphics2D
        gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        gfx.background = Color.BLACK
        gfx.color = Color.WHITE

        val text = String(charArrayOf(char))
        gfx.font = awtFont
        gfx.drawString(text, 0, baselineY)
        gfx.dispose()

        // todo if image is blank, add char code as hex inside
        //  0xffff -> ff|ff

        return Texture(PixelData(image))
    }

    val spaceWidth = run {
        val xLength = max(len(charArrayOf('o')).toFloat(), 1f)
        (min(xLength, awtFont.size.toFloat()) * 0.667f).toInt()
    }

    private fun len(chars: CharArray): Double {
        return TextLayout(String(chars), awtFont, renderContext).bounds.maxX
    }

    private fun calculateOffset(charA: Char, charB: Char): Int {
        if (charA == ' ') return spaceWidth
        if (charB == ' ') return len(charArrayOf(charA)).toInt()

        val lenAB = len(charArrayOf(charA, charB))
        val lenB = len(charArrayOf(charB))
        return (lenAB - lenB).toInt()
    }

    fun destroyTextures() {
        val textures = textures.filterNotNull()
        glDeleteTextures(IntArray(textures.size) { textures[it].pointer })
    }

}