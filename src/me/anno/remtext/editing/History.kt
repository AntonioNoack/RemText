package me.anno.remtext.editing

class History {

    val changes = ArrayList<Change>()
    var size = 0

    fun push(change: Change) {
        change.redo()

        changes.subList(size, changes.size).clear()
        changes.add(change)
        size++

        if (changes.size > 512) {
            changes.removeAt(0)
            size--
        }
    }

    fun undo() {
        if (size > 0) {
            val lastChange = changes[--size]
            lastChange.undo()
        }
    }

    fun redo() {
        if (size < changes.size) {
            val nextChange = changes[size++]
            nextChange.redo()
        }
    }

}