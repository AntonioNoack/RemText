package me.anno.remtext.gfx

import org.lwjgl.opengl.GL46C.glGetUniformLocation

class FlatColorShader : Shader(
    """
        #version 330 core
        layout(location = 0) in vec2 aPos;
        uniform vec4 bounds;
        void main() {
            gl_Position = vec4(aPos * bounds.xy + bounds.zw, 0.0, 1.0);
        }
    """, """
        #version 330 core
        out vec4 FragColor;
        uniform vec4 color;
        void main() {
            FragColor = color;
        }
    """
) {
    val bounds = glGetUniformLocation(pointer, "bounds")
    val color = glGetUniformLocation(pointer, "color")
}