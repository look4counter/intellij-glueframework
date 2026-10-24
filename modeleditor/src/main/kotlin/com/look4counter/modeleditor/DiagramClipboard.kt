package com.look4counter.modeleditor

import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

data class CopiedDiagramChild(val node: DiagramNode, val xml: String)
data class CopiedDiagramWire(val wire: DiagramWire, val xml: String)

class DiagramClipboardData(
    val children: List<CopiedDiagramChild>,
    val wires: List<CopiedDiagramWire>,
    val namespaces: Map<String, String>,
    val screenOffset: DiagramPoint,
    val zoom: Double = 1.0,
) {
    var pasteCount: Int = 0
}

object DiagramClipboard {
    private val flavor = DataFlavor(
        "${DataFlavor.javaJVMLocalObjectMimeType};class=${DiagramClipboardData::class.java.name}",
        "Glue diagram selection", DiagramClipboardData::class.java.classLoader)

    fun read(): DiagramClipboardData? = CopyPasteManager.getInstance().getContents<DiagramClipboardData>(flavor)

    fun write(data: DiagramClipboardData) {
        CopyPasteManager.getInstance().setContents(object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(flavor)
            override fun isDataFlavorSupported(candidate: DataFlavor) = candidate == flavor
            override fun getTransferData(candidate: DataFlavor): Any {
                if (!isDataFlavorSupported(candidate)) throw UnsupportedFlavorException(candidate)
                return data
            }
        })
    }
}
