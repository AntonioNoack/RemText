package me.anno.remtext.editing

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

class StringListTransferable(
    private val data: List<String>
) : Transferable {

    companion object {
        val separator: String = System.lineSeparator()
        val stringListFlavor = DataFlavor(
            List::class.java,
            "application/x-kotlin-string-list"
        )
    }

    override fun getTransferDataFlavors() = arrayOf(stringListFlavor, DataFlavor.stringFlavor)
    override fun isDataFlavorSupported(f: DataFlavor) = f == stringListFlavor || f == DataFlavor.stringFlavor
    override fun getTransferData(f: DataFlavor): Any {
        return when (f) {
            stringListFlavor -> data
            DataFlavor.stringFlavor -> data.joinToString(separator)
            else -> throw UnsupportedFlavorException(f)
        }
    }
}