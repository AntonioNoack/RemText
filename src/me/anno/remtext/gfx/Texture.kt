package me.anno.remtext.gfx

import org.lwjgl.opengl.GL46C.*

class Texture(data: PixelData) {

    val width = data.width
    val height = data.height

    // Create texture
    val pointer = glGenTextures()

    init {
        glBindTexture(GL_TEXTURE_2D, pointer)

        // Upload to GPU
        glTexImage2D(
            GL_TEXTURE_2D, 0,
            GL_RGBA8,
            width, height, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, data.pixels
        )

        // Texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        // Unbind
        glBindTexture(GL_TEXTURE_2D, 0)
    }
}