package com.look4counter.modeleditor

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

object WireSnap {
    fun dragPosition(wire: DiagramWire, point: DiagramPoint): DiagramPoint {
        val nodes = listOfNotNull(wire.start, wire.end).filter { it.isActivity }
        fun snapped(horizontal: Boolean): Double {
            val value = if (horizontal) point.x else point.y
            val ranges = nodes.map { node ->
                val start = if (horizontal) node.location.x else node.location.y
                val size = if (horizontal) node.width else node.height
                val inset = minOf(8.0, size / 2)
                start + inset to start + size - inset
            }
            val commonLow = ranges.maxOfOrNull { it.first }
            val commonHigh = ranges.minOfOrNull { it.second }
            return nodes.mapIndexedNotNull { index, node ->
                var low = ranges[index].first; var high = ranges[index].second
                if (wire.bendpoints.isEmpty() && nodes.size == 2 && commonLow!! <= commonHigh!!) {
                    low = commonLow; high = commonHigh
                }
                if (value !in low..high) return@mapIndexedNotNull null
                val center = if (horizontal) node.center.x else node.center.y
                if (!horizontal) center.takeIf { it in low..high }
                else {
                    val first = ceil((low - center) / 30); val last = floor((high - center) / 30)
                    if (first > last) null else center + round((value - center) / 30).coerceIn(first, last) * 30
                }
            }.minByOrNull { kotlin.math.abs(it - value) } ?: value
        }
        return DiagramPoint(snapped(true), snapped(false))
    }
    fun attach(wire: DiagramWire, start: Boolean, point: DiagramPoint): DiagramWire {
        val node = (if (start) wire.start else wire.end) ?: return wire
        if (!node.isActivity) return wire
        if (wire.bendpoints.isEmpty() && wire.start?.isActivity == true && wire.end?.isActivity == true) return wire
        val path = DiagramGeometry.path(wire)
        if (path.size < 2) return wire
        val neighbor = if (start) path[1] else path[path.lastIndex - 1]
        val vertical = neighbor.x in node.location.x..(node.location.x + node.width)
        val contact = if (vertical) DiagramPoint(point.x,
            if (neighbor.y < node.center.y) node.location.y else node.location.y + node.height)
            else DiagramPoint(if (neighbor.x < node.center.x) node.location.x else node.location.x + node.width, node.center.y)
        val current = if (start) path.first() else path.last()
        if (kotlin.math.hypot(current.x - contact.x, current.y - contact.y) < 0.000001) return wire
        val bend = if (vertical) DiagramPoint(contact.x, (contact.y + neighbor.y) / 2)
            else DiagramPoint((contact.x + neighbor.x) / 2, contact.y)
        return DiagramEdits.moveBendpoint(wire, if (start || wire.bendpoints.isEmpty()) 0 else wire.bendpoints.lastIndex,
            bend, wire.bendpoints.isEmpty())
    }
    fun position(node: DiagramNode, point: DiagramPoint, opposite: DiagramNode? = null): DiagramPoint {
        if (!node.isActivity) return point
        val inset = minOf(8.0, node.width / 2)
        var low = node.location.x + inset
        var high = node.location.x + node.width - inset
        if (opposite?.isActivity == true) {
            val commonLow = maxOf(low, opposite.location.x + minOf(8.0, opposite.width / 2))
            val commonHigh = minOf(high, opposite.location.x + opposite.width - minOf(8.0, opposite.width / 2))
            if (commonLow <= commonHigh) { low = commonLow; high = commonHigh }
        }
        val first = ceil((low - node.center.x) / 30)
        val last = floor((high - node.center.x) / 30)
        val x = if (first <= last) node.center.x + round((point.x - node.center.x) / 30).coerceIn(first, last) * 30
            else point.x.coerceIn(low, high)
        return DiagramPoint(x, node.center.y)
    }
}
