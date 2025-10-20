package me.anno.remtext.gfx

import org.lwjgl.BufferUtils
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class PixelData(image: BufferedImage) {

    val width = image.width
    val height = image.height

    // Allocate buffer for RGBA pixels
    val pixels: ByteBuffer = BufferUtils.createByteBuffer(width * height * 4)

    init {
        val raster = IntArray(width * height)
        image.getRGB(0, 0, width, height, raster, 0, width)

        // Convert ARGB (Java default) → RGBA (OpenGL expected)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = raster[y * width + x]
                val a = (pixel shr 24) and 0xFF
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = (pixel) and 0xFF
                pixels.put(r.toByte())
                pixels.put(g.toByte())
                pixels.put(b.toByte())
                pixels.put(a.toByte())
            }
        }
        pixels.flip()
    }
}