package me.anno.remtext.colors

import me.anno.remtext.colors.impl.*

object Languages {

    val highlighters = HashMap<String, Language>()

    init {
        highlighters["c"] = CLikeLanguage(CLikeLanguageType.C)
        highlighters["cpp"] = CLikeLanguage(CLikeLanguageType.CPP)
        highlighters["cs"] = CLikeLanguage(CLikeLanguageType.CSHARP)
        highlighters["go"] = CLikeLanguage(CLikeLanguageType.GO)
        highlighters["java"] = CLikeLanguage(CLikeLanguageType.JAVA)
        highlighters["js"] = CLikeLanguage(CLikeLanguageType.JAVASCRIPT)
        highlighters["json"] = CLikeLanguage(CLikeLanguageType.JSON)
        highlighters["kt"] = CLikeLanguage(CLikeLanguageType.KOTLIN)
        highlighters["py"] = CLikeLanguage(CLikeLanguageType.PYTHON)
        highlighters["swift"] = CLikeLanguage(CLikeLanguageType.SWIFT)
        highlighters["rs"] = CLikeLanguage(CLikeLanguageType.RUST)
        highlighters["zig"] = CLikeLanguage(CLikeLanguageType.ZIG)
        highlighters["hlsl"] = CLikeLanguage(CLikeLanguageType.HLSL)
        highlighters["glsl"] = CLikeLanguage(CLikeLanguageType.GLSL)
        highlighters["xml"] = XMLLanguage(false)
        highlighters["html"] = XMLLanguage(true)
        highlighters["sh"] = ShellLanguage
        highlighters["bat"] = BatchLanguage
        highlighters["css"] = CSSLanguage
        highlighters["yml"] = YAMLLanguage
        highlighters["md"] = MarkdownLanguage
        alias("yml", "yaml")
        alias("js", "javascript")
        alias("kt", "kotlin")
        alias("sh", "hash")
        alias("cs", "csharp")
        alias("py", "pyx", "cython")
        alias("html", "php", "hbs", "htm")
        alias("c", "h")
        alias("cpp", "hpp")
        alias("glsl", "vert", "frag", "geo")
        alias("md", "markdown")
    }

    fun alias(src: String, vararg aliases: String) {
        val language = highlighters[src]!!
        for (dst in aliases) {
            highlighters[dst] = language
        }
    }
}