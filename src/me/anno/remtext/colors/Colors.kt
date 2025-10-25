package me.anno.remtext.colors

import me.anno.remtext.Window
import me.anno.remtext.gfx.Color

// todo load them from a config .txt file?
object Colors {

    const val DEFAULT = 0.toByte()
    const val COMMENT = 1.toByte()
    const val ML_COMMENT = 2.toByte()
    const val NUMBER = 3.toByte()
    const val SYMBOL = 4.toByte()
    const val KEYWORD = 5.toByte()
    const val BRACKET = 6.toByte()
    const val STRING = 7.toByte()
    const val ML_STRING = 8.toByte()
    const val TODO = 9.toByte()
    const val VARIABLE = 10.toByte()
    const val DOC_COMMENT = 11.toByte()

    val textColor = Color(0xdddddd)
    val commentColor = Color(0x7FD723)
    val numberColor = Color(0x33bbcc)
    val symbolColor = Color(0xaaaaaa)
    val keywordColor = Color(0xE59F39)
    val bracketColor = Color(0xc0a070)
    val stringColor = Color(0x6DBF24)
    val todoColor = Color(0xA7BF23)
    val variableColor = Color(0xffffff)
    val docColor = Color(0x7FD723)

    val darkStyle = arrayOf(
        textColor,
        commentColor,
        commentColor,
        numberColor,
        symbolColor,
        keywordColor,
        bracketColor,
        stringColor,
        stringColor,
        todoColor,
        variableColor,
        docColor,
    )

    val lightStyle = arrayOf(
        textColor,
        commentColor,
        commentColor,
        numberColor,
        symbolColor,
        keywordColor,
        bracketColor,
        stringColor,
        stringColor,
        todoColor,
        variableColor,
        docColor,
    )

    val style get() = if (Window.isDarkTheme) darkStyle else lightStyle
    operator fun get(color: Byte): Color = style[color.toInt()]

    operator fun get(char: Char): Color {
        val type = when (char) {
            in "()[]{}" -> BRACKET
            in "+-/*<>=?:" -> SYMBOL
            in '0'..'9' -> NUMBER
            else -> DEFAULT
        }
        return this[type]
    }
}