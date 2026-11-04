package com.look4counter.modeleditor

import kotlin.math.abs

class OrthogonalWireSnap {
    private var x: Double? = null
    private var y: Double? = null

    fun reset() { x = null; y = null }

    fun position(raw: DiagramPoint, neighbors: List<DiagramPoint>, zoom: Double,
                 fallback: DiagramPoint = raw): DiagramPoint {
        require(zoom.isFinite() && zoom > 0)
        fun axis(value: Double, candidates: List<Double>, retained: Double?): Double? {
            if (retained != null && retained in candidates && abs(value - retained) * zoom <= 10.0 + 1e-9) return retained
            return candidates.minByOrNull { abs(value - it) }?.takeIf { abs(value - it) * zoom <= 6.0 + 1e-9 }
        }
        x = axis(raw.x, neighbors.map { it.x }, x)
        y = axis(raw.y, neighbors.map { it.y }, y)
        return DiagramPoint(x ?: fallback.x, y ?: fallback.y)
    }

    companion object {
        fun neighbors(wire: DiagramWire, index: Int, insert: Boolean): List<DiagramPoint> {
            val path = DiagramGeometry.path(wire)
            if (path.size < 2) return emptyList()
            fun anchor(position: Int): DiagramPoint? = when (position) {
                0 -> if (wire.explicitContacts && wire.start?.isActivity == true) path.first() else wire.start?.center
                path.lastIndex -> if (wire.explicitContacts && wire.end?.isActivity == true) path.last() else wire.end?.center
                else -> path.getOrNull(position)
            }
            return listOfNotNull(anchor(index), anchor(index + if (insert) 1 else 2))
        }
    }
}
