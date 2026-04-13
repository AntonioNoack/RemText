package me.anno.remtext.gfx

import org.lwjgl.opengl.GL46C.glGetUniformLocation

class TextureColorShader : Shader(
    """
        #version 330 core
        layout(location = 0) in vec2 aPos;
        uniform vec4 bounds;
        uniform vec4 uvBounds;
        out vec2 uv;
        void main() {
            gl_Position = vec4(aPos * bounds.xy + bounds.zw, 0.0, 1.0);
            vec2 baseUV = vec2(aPos.x, 1.0-aPos.y);
            uv = uvBounds.xy + baseUV * uvBounds.zw;
        }
    """, """
        #version 330 core
        in vec2 uv;
        out vec4 FragColor;
        uniform sampler2D tex1;
        uniform vec3 textColor;
        uniform vec3 bgColor;
        uniform float useRawTextureColor;
        void main() {
            vec4 tex = texture2D(tex1, uv);
            if (useRawTextureColor > 0.5) {
                if (tex.a < 0.001) discard;
                FragColor = tex;
            } else {
                vec3 base = tex.rgb;
                if (base.g < 0.001) discard;
                FragColor = vec4(mix(bgColor, textColor, base), 1.0);
            }
        }
    """
) {
    val bounds = glGetUniformLocation(pointer, "bounds")
    val uvBounds = glGetUniformLocation(pointer, "uvBounds")
    val useRawTextureColor = glGetUniformLocation(pointer, "useRawTextureColor")
    val textColor = glGetUniformLocation(pointer, "textColor")
    val bgColor = glGetUniformLocation(pointer, "bgColor")
}
