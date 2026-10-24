package com.look4counter.modeleditor

import java.awt.BorderLayout
import com.intellij.openapi.ui.ComboBox
import javax.swing.JPanel
import kotlin.math.roundToInt

class ZoomControls(private val canvas: ModelDiagramCanvas) : JPanel(BorderLayout()) {
    val select = ComboBox((20..150 step 10).map { "$it%" }.toTypedArray())
    private var updating = false
    init {
        name = "model.zoom"
        isOpaque = false
        select.name = "model.zoom.select"
        select.toolTipText = "캔버스 확대·축소"
        add(select)
        select.addActionListener {
            if (!updating) (select.selectedItem as? String)?.removeSuffix("%")?.toDoubleOrNull()?.let { canvas.setZoom(it / 100) }
        }
        canvas.onZoomChanged = { refresh() }
        refresh()
    }
    private fun refresh() {
        updating = true
        try { select.selectedItem = "${(canvas.zoom * 100).roundToInt()}%" }
        finally { updating = false }
    }
}
