package me.anno.remtext.colors

import me.anno.remtext.colors.Colors.BRACKET
import me.anno.remtext.colors.Colors.COMMENT
import me.anno.remtext.colors.Colors.DEFAULT
import me.anno.remtext.colors.Colors.KEYWORD
import me.anno.remtext.colors.Colors.ML_COMMENT
import me.anno.remtext.colors.Colors.ML_STRING
import me.anno.remtext.colors.Colors.NUMBER
import me.anno.remtext.colors.Colors.STRING
import me.anno.remtext.colors.Colors.SYMBOL
import me.anno.remtext.font.Line

class JsonHighlighter(val isJavaScript: Boolean) : Highlighter {

    companion object {
        val keywords = ("abstract,arguments,async,await,boolean,break,byte,case,catch,char,class,const," +
                "continue,debugger,default,delete,do,double,else,enum,eval,export,extends,false,final," +
                "finally,float,for,function,goto,if,implements,function,import,in,instanceof,int,interface," +
                "let,long,native,new,null,package,private,protected,public,return,short,static,super,switch," +
                "synchronized,this,throw,throws,transient,true,try,typeof,using,var,void,volatile,while,with,yield")
            .split(',').groupBy { it[0] }
    }

    private fun isLetter(char: Char): Boolean {
        return char.isLetterOrDigit() || char in "_$"
    }

    override fun highlight(line: Line, state0: Byte) {

        var state = if (state0 == ML_COMMENT || state0 == ML_STRING) state0 else DEFAULT

        val text = line.text
        val colors = line.colors
        var i = line.i0

        loop@ while (i < line.i1) {
            when (state) {
                DEFAULT -> {
                    if (isJavaScript) {
                        val keywords = keywords[text[i].lowercaseChar()] ?: emptyList()
                        val keyword = keywords.firstOrNull { keyword -> text.startsWith(keyword, i) }
                        if (keyword != null && (
                                    (i + keyword.length) >= text.length ||
                                            !isLetter(text[i + keyword.length])
                                    )
                        ) {
                            colors.fill(KEYWORD, i, i + keyword.length)
                            i += keyword.length
                            continue@loop
                        }
                    }

                    when {
                        line.startsWith("/*", i) -> {
                            val end = line.indexOf("*/", i + 2, false) + 2
                            if (end >= 0) {
                                colors.fill(ML_COMMENT, i, end + 2)
                                i = end + 2
                            } else {
                                colors.fill(ML_COMMENT, i, line.i1)
                                i = line.i1
                            }
                        }
                        line.startsWith("//", i) -> {
                            val end = line.indexOf("todo", i + 2, true)
                            if (end >= 0) {
                                colors.fill(COMMENT, i, end)
                                colors.fill(Colors.TODO, end, line.i1)
                                i = line.i1
                            } else {
                                colors.fill(COMMENT, i, i + 2)
                                i = line.i1
                            }
                        }
                        else -> when (text[i]) {
                            '"' -> {
                                val end = findEndOfString(line, i, '"')
                                colors.fill(STRING, i, end)
                                i = end
                            }
                            '\'' -> {
                                val end = findEndOfString(line, i, '\'')
                                colors.fill(STRING, i, end)
                                i = end
                            }
                            '`' -> {
                                colors[i++] = ML_STRING
                                state = ML_STRING
                            }
                            in "+-" -> {
                                if (i + 1 < line.i1 && text[i + 1] in ".0123456789") {
                                    val end = findEndOfNumber(line, i)
                                    colors.fill(NUMBER, i, end)
                                    i = end
                                } else {
                                    colors[i++] = SYMBOL
                                }
                            }
                            '.' -> {
                                if (i + 1 < line.i1 && text[i + 1] in '0'..'9') {
                                    val end = findEndOfNumber(line, i)
                                    colors.fill(NUMBER, i, end)
                                    i = end
                                } else {
                                    colors[i++] = SYMBOL
                                }
                            }
                            in '0'..'9' -> {
                                val end = findEndOfNumber(line, i)
                                colors.fill(NUMBER, i, end)
                                i = end
                            }
                            in "/*<>%" -> {
                                colors[i++] = SYMBOL
                            }
                            in "([{" -> {
                                colors[i++] = BRACKET
                            }
                            in ")]}" -> {
                                colors[i++] = BRACKET
                            }
                            else -> i++
                        }
                    }
                }
                ML_COMMENT -> {
                    if (text.startsWith("*/", i)) {
                        colors.fill(ML_COMMENT, i, i + 2)
                        state = DEFAULT
                        i += 2
                    } else {
                        colors[i++] = ML_COMMENT
                    }
                }
                COMMENT, STRING -> throw IllegalStateException("Cannot happen")
                ML_STRING -> {
                    // find end of ML_STRING
                    val j = line.indexOf('`', i, false)
                    if (j >= 0) {
                        colors.fill(ML_STRING, i, j + 1)
                        state = DEFAULT
                        i = j + 1
                    }
                }
                else -> i++
            }
        }
    }

    private fun findEndOfString(line: Line, i: Int, symbol: Char): Int {
        val text = line.text
        // find end of string
        var end = i + 1
        while (end < line.i1) {
            when (text[end++]) {
                '\\' -> end++
                symbol -> return end
                else -> {}
            }
        }
        return line.i1
    }

    private fun isHexChar(char: Char): Boolean {
        return when (char) {
            in '0'..'9', in 'A'..'F', in 'a'..'f' -> true
            else -> false
        }
    }

    private fun findEndOfNumber(line: Line, i: Int): Int {
        var j = i
        val text = line.text
        if (text[j] in "+-") j++
        when {
            line.startsWith("0x", j, true) -> {
                j += 2
                while (j < line.i1 && isHexChar(text[j])) j++
                return j
            }
            line.startsWith("0b", j, true) -> {
                while (j < line.i1 && text[j] in '0'..'1') j++
                return j
            }
        }

        while (j < line.i1 && text[j] in '0'..'9') j++
        if (line.startsWith(".", j)) {
            j++
            while (j < line.i1 && text[j] in '0'..'9') j++
        }
        if (line.startsWith("e", j, true)) {
            j++
            if (j < line.i1 && text[j] in "+-") j++
            while (j < line.i1 && text[j] in '0'..'9') j++
        }
        return j
    }

}