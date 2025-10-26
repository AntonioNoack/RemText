package me.anno.remtext

import me.anno.remtext.Controls.addListeners
import me.anno.remtext.Rendering.renderWindow
import me.anno.remtext.Window.createWindow
import java.io.File

// todo:
//  block folding: if line ends on {[(, count until we reach the opposite, folding them...
//  multiple open files? / new file menu?
//  monospaced-mode

// todo can we make JavaScript inside HTML inside Markdown work?

// more sth for a separate editor:
//  hex-editor mode?

fun main(args: Array<String>) {

    if (args.size != 1) {
        println("Usage: RemText.jar <filePath>")
        return
    }

    println("Opening '${args[0]}'")
    val file = File(args[0])
    Rendering.file = OpenFile(file)

    createWindow()
    addListeners()
    renderWindow()
}
