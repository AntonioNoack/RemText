package me.anno.remtext.gfx

import me.anno.remtext.Rendering.gfxCheck
import org.lwjgl.opengl.GL11C.GL_FLOAT
import org.lwjgl.opengl.GL15C.*
import org.lwjgl.opengl.GL20C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20C.glVertexAttribPointer
import org.lwjgl.opengl.GL30C.*

class Quad {

    val vao: Int
    val vbo: Int

    init {

        gfxCheck()

        // Vertex data for a full-screen quad
        val vertices = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )
        vao = glGenVertexArrays()
        vbo = glGenBuffers()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L)
        glEnableVertexAttribArray(0)

        gfxCheck()

    }

    fun destroy() {
        glDeleteVertexArrays(vao)
        glDeleteBuffers(vbo)
    }

}