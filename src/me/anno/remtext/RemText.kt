package me.anno.remtext

import me.anno.remtext.Controls.addListeners
import me.anno.remtext.Rendering.renderWindow
import me.anno.remtext.Window.createWindow
import java.io.File

// todo:
//  block folding
//  find & replace
//  load files?
//  JSON/syntax highlighting
//  JSON formatter
//  hex-editor mode?
// done:
//  high performance,
//  quick starting, light-weight text editor
//  word-wrap,
//  select and copy text
//  saving
//  undo/redo history:
//      we have paste & delete selection ->
//      save what we delete, save what we paste and where,
//      and then we should be able to easily undo and redo these operations

// done first use engine for text-layout,
//  later move away from it for best startup times

fun main(args: Array<String>) {

    val file = if (args.isNotEmpty()) {
        File(args[0])
    } else {
        val home = File(System.getProperty("user.home"))
        File(home, "Documents/Test Text.txt")
    }

    Rendering.file = OpenFile(file)

    createWindow()
    addListeners()
    renderWindow()
}