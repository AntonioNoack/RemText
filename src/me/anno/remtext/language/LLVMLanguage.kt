package me.anno.remtext.language

import me.anno.remtext.Colors
import me.anno.remtext.Colors.COMMENT
import me.anno.remtext.Colors.DEFAULT
import me.anno.remtext.Colors.KEYWORD
import me.anno.remtext.Colors.NUMBER
import me.anno.remtext.Colors.STRING
import me.anno.remtext.Colors.VARIABLE
import me.anno.remtext.blocks.BlockStyle
import me.anno.remtext.font.Line

object LLVMLanguage : Language {

    // todo different color (?)
    private const val TYPE = KEYWORD

    // Core LLVM IR keywords/instructions
    private val keywords = (
            "define,declare,global,constant,private,internal,external," +
                    "linkonce,weak,appending,dso_local,unnamed_addr," +
                    "alloca,load,store,getelementptr,phi,select,call,invoke," +
                    "ret,br,switch,indirectbr,resume,unreachable," +
                    "add,fadd,sub,fsub,mul,fmul,udiv,sdiv,fdiv,urem,srem,frem," +
                    "shl,lshr,ashr,and,or,xor," +
                    "icmp,fcmp,trunc,zext,sext,fptrunc,fpext," +
                    "uitofp,sitofp,fptoui,fptosi,inttoptr,ptrtoint,bitcast," +
                    "addrspacecast,extractelement,insertelement,shufflevector," +
                    "extractvalue,insertvalue,atomicrmw,cmpxchg,fence," +
                    "true,false,null,undef,poison"
            ).split(',')

    // Common LLVM IR types
    private val types = hashSetOf(
        "void",
        "half", "float", "double", "fp128",
        "x86_fp80", "ppc_fp128",
        "label", "metadata", "token",
        "i1", "i8", "i16", "i32", "i64", "i128"
    )

    override fun highlight(line: Line, state0: Byte): Byte {

        val text = line.text
        val colors = line.colors ?: return DEFAULT
        if (colors.isEmpty()) return DEFAULT

        var i = line.i0

        while (i < line.i1) {

            when {
                text[i] == ';' -> { // comments
                    val todoIndex = line.indexOf("TODO", i + 1, true)
                    val end = line.i1

                    if (todoIndex >= 0) {
                        colors.fill(COMMENT, i, todoIndex)
                        colors.fill(Colors.TODO, todoIndex, end)
                    } else {
                        colors.fill(COMMENT, i, end)
                    }

                    i = end
                }

                text[i] == '"' -> { // strings
                    val end = CLikeLanguage.findEndOfString(line, i, '"')
                    colors.fill(STRING, i, end)
                    i = end
                }

                // local/global identifiers
                text[i] == '%' || text[i] == '@' -> {
                    val start = i++
                    while (
                        i < line.i1 &&
                        (
                                text[i].isLetterOrDigit() ||
                                        text[i] == '_' ||
                                        text[i] == '.' ||
                                        text[i] == '$'
                                )
                    ) {
                        i++
                    }
                    colors.fill(VARIABLE, start, i)
                }

                // numbers
                text[i].isDigit() || (
                        text[i] == '-' &&
                                i + 1 < line.i1 &&
                                text[i + 1].isDigit()
                        ) -> {

                    val end = CLikeLanguage.findEndOfNumber(line, i)
                    colors.fill(NUMBER, i, end)
                    i = end
                }

                // metadata references like !0
                text[i] == '!' -> {
                    val start = i++
                    while (i < line.i1 && text[i].isLetterOrDigit()) {
                        i++
                    }
                    colors.fill(TYPE, start, i)
                }

                // identifiers / keywords / types
                text[i].isLetter() -> {

                    val start = i
                    i++

                    while (
                        i < line.i1 &&
                        (
                                text[i].isLetterOrDigit() ||
                                        text[i] == '_' ||
                                        text[i] == '.'
                                )
                    ) {
                        i++
                    }

                    val word = text.substring(start, i)

                    when {
                        word in keywords -> {
                            colors.fill(KEYWORD, start, i)
                        }
                        word in types || (
                                word.startsWith("i") &&
                                        word.length > 1 &&
                                        word.substring(1).all { it.isDigit() }
                                ) -> {
                            colors.fill(TYPE, start, i)
                        }
                    }
                }
                else -> i++
            }
        }

        val state = DEFAULT
        colors[line.i1] = state
        return state
    }

    override fun getBlockStyle() = BlockStyle.INDENTATION

    override fun toString(): String = "LLVM IR"
}