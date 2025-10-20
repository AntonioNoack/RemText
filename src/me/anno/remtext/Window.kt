package me.anno.remtext

import me.anno.remtext.Rendering.file
import me.anno.remtext.Rendering.gfxCheck
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import java.awt.MouseInfo
import java.io.File

object Window {

    const val WINDOW_TITLE = "RemText"
    const val NULL = 0L

    val lightThemeFile = File("./UseLightTheme.txt")

    var isDarkTheme = !lightThemeFile.exists()

    var window = 0L
    var windowWidth = 0
    var windowHeight = 0
    var availableWidth = 0

    fun createWindow() {
        // Initialize GLFW
        if (!glfwInit()) error("Failed to init GLFW")
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

        val width = 400
        val height = 300
        window = glfwCreateWindow(width, height, "LWJGL Quad Example", NULL, NULL)
        if (window == NULL) error("Failed to create GLFW window")

        // Center the window at cursor
        val mouse0 = MouseInfo.getPointerInfo().location
        val windowX = mouse0.x - width.shr(1)
        val windowY = mouse0.y - height.shr(1)
        glfwSetWindowPos(window, windowX, windowY)

        glfwSetWindowTitle(window, "$WINDOW_TITLE - ${file.file.name}")

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) // vsync
        glfwShowWindow(window)
        GL.createCapabilities()

        gfxCheck()
    }

}