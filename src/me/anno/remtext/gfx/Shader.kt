package me.anno.remtext.gfx

import me.anno.remtext.Rendering.gfxCheck
import org.lwjgl.opengl.GL11C.GL_FALSE
import org.lwjgl.opengl.GL20C.*

abstract class Shader(vertexSrc: String, fragmentSrc: String) {

    companion object {
        private var lastPointer = -1
    }

    val pointer: Int

    init {

        gfxCheck()

        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, vertexSrc)
        glCompileShader(vertexShader)
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
            error("Vertex shader error: " + glGetShaderInfoLog(vertexShader))

        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, fragmentSrc)
        glCompileShader(fragmentShader)
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
            error("Fragment shader error: " + glGetShaderInfoLog(fragmentShader))

        pointer = glCreateProgram()
        glAttachShader(pointer, vertexShader)
        glAttachShader(pointer, fragmentShader)
        glLinkProgram(pointer)
        if (glGetProgrami(pointer, GL_LINK_STATUS) == GL_FALSE)
            error("Shader link error: " + glGetProgramInfoLog(pointer))

        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
        use()

        gfxCheck()

    }

    fun destroy() {
        glDeleteProgram(pointer)
    }

    fun use() {
        if (lastPointer != pointer) {
            glUseProgram(pointer)
            lastPointer = pointer
        }
    }

}