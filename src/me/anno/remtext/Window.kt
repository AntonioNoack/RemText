package me.anno.remtext

import me.anno.remtext.Rendering.file
import me.anno.remtext.Rendering.gfxCheck
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL
import java.awt.MouseInfo
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

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

        // for this to work, there must be a RemText.desktop
        val appName = "RemText"
        glfwWindowHintString(GLFW_X11_CLASS_NAME, appName)
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, appName)

        val width = 800
        val height = 600
        window = glfwCreateWindow(width, height, "LWJGL Quad Example", NULL, NULL)
        if (window == NULL) error("Failed to create GLFW window")

        // Center the window at cursor
        val mouse0 = MouseInfo.getPointerInfo().location
        val windowX = mouse0.x - width.shr(1)
        val windowY = mouse0.y - height.shr(1)
        glfwSetWindowPos(window, windowX, windowY)
        glfwSetWindowTitle(window, "$WINDOW_TITLE - ${file.file.name}")
        // todo execute the setIcon() function if on Windows
        if (false) setIcon()

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) // vsync
        glfwShowWindow(window)
        GL.createCapabilities()

        gfxCheck()
    }

    /**
     * For Ubuntu, there must be a .desktop file with name RemText.desktop, and the proper linked icon, and you must re-log,
     * for Windows, the following code should work:
     * */
    private fun setIcon() {
        val icon = ImageIO.read(Window.javaClass.getResourceAsStream("/Icon256.png"))
        val image = GLFWImage.malloc()
        val pixelBytes = ByteBuffer
            .allocateDirect(icon.width * icon.height * 4)
            .order(ByteOrder.nativeOrder())
        for (y in 0 until icon.height) {
            for (x in 0 until icon.width) {
                val rgb = icon.getRGB(x, y)
                // channels are not tested yet
                pixelBytes.put((rgb shr 16).toByte())
                pixelBytes.put((rgb shr 8).toByte())
                pixelBytes.put((rgb).toByte())
                pixelBytes.put((rgb shr 24).toByte())
            }
        }
        pixelBytes.flip()
        image.set(icon.width, icon.height, pixelBytes)

        val buffer = GLFWImage.malloc(1)
        buffer.put(0, image)
        glfwSetWindowIcon(window, buffer)
        buffer.free()
    }
}