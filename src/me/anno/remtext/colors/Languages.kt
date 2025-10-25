package me.anno.remtext.colors

import me.anno.remtext.colors.impl.*

object Languages {
    val highlighters = HashMap<String, Language>()

    init {
        highlighters["c"] = CLikeLanguage(CLikeLanguageType.C)
        highlighters["h"] = CLikeLanguage(CLikeLanguageType.C)
        highlighters["cpp"] = CLikeLanguage(CLikeLanguageType.CPP)
        highlighters["hpp"] = CLikeLanguage(CLikeLanguageType.CPP)
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
        highlighters["vert"] = CLikeLanguage(CLikeLanguageType.GLSL)
        highlighters["frag"] = CLikeLanguage(CLikeLanguageType.GLSL)
        highlighters["xml"] = XMLLanguage(false)
        highlighters["html"] = XMLLanguage(true)
        highlighters["php"] = XMLLanguage(true)
        highlighters["hbs"] = XMLLanguage(true) // handlebars
        highlighters["sh"] = ShellLanguage
        highlighters["bat"] = BatchLanguage
        highlighters["css"] = CSSLanguage
    }
}