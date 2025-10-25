package me.anno.remtext.colors

object HighlighterLib {
    val highlighters = HashMap<String, Highlighter>()

    init {
        highlighters["js"] = JsonHighlighter(false)
        highlighters["json"] = JsonHighlighter(true)
    }
}