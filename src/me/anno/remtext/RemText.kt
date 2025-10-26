package me.anno.remtext

import me.anno.remtext.Controls.addListeners
import me.anno.remtext.Rendering.renderWindow
import me.anno.remtext.Window.createWindow
import java.io.File

// todo:
//  block folding: if line ends on {[(, count until we reach the opposite, folding them...
//  multiple open files? / new file menu?
//  hex-editor mode?
//  monospaced-mode

fun main(args: Array<String>) {

    val file = if (args.isNotEmpty()) {
        File(args[0])
    } else {
        val home = File(System.getProperty("user.home"))
        File(home, "Documents/Test Text.html")
    }

    Rendering.file = OpenFile(file)

    createWindow()
    addListeners()
    renderWindow()
}
