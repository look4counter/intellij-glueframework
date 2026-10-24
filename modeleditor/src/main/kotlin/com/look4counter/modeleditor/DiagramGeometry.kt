package com.look4counter.modeleditor

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object DiagramGeometry {
    fun previewStateEndpoint(wire: DiagramWire, start: Boolean, center: DiagramPoint): List<DiagramPoint> {
        if (wire.start == null || wire.end == null) return emptyList()
        val state = DiagramNode(DiagramNodeKind.InitialState, "drag-preview", "",
            DiagramPoint(center.x - 10, center.y - 10), 20.0, 20.0)
        val source = if (start) state else wire.start
        val target = if (start) wire.end else state
        return path(wire.copy(start = source, end = target,
            firstRelativePoint = if (start) null else wire.firstRelativePoint,
            lastRelativePoint = if (start) wire.lastRelativePoint else null))
    }

    fun labelCenter(points: List<DiagramPoint>): DiagramPoint? {
        if (points.size < 2) return null
        val index = (points.size - 2) / 2
        val a = points[index]
        val b = points[index + 1]
        return DiagramPoint((a.x + b.x) / 2, (a.y + b.y) / 2)
    }

    fun path(wire: DiagramWire): List<DiagramPoint> {
        val start = wire.start ?: return emptyList()
        val end = wire.end ?: return emptyList()
        val explicit = wire.explicitContacts && wire.firstRelativePoint != null && wire.lastRelativePoint != null
        if (wire.bendpoints.isEmpty() && !explicit) {
            val (a, b) = endpoints(start, end, wire.firstRelativePoint ?: start.center, wire.lastRelativePoint)
            return listOf(a, b)
        }
        val bends = wire.bendpoints.mapIndexed { i, bend ->
            val ratio = (i + 1).toDouble() / (wire.bendpoints.size + 1)
            val a = start.center + bend.first
            val b = end.center + bend.second
            DiagramPoint(a.x + (b.x - a.x) * ratio, a.y + (b.y - a.y) * ratio)
        }
        if (explicit) return listOf(wire.firstRelativePoint!!) + bends + wire.lastRelativePoint!!
        val first = if (start.isActivity) toward(start, bends.first()) else stateContact(start, bends.first(), wire.firstRelativePoint)
        val last = if (end.isActivity) toward(end, bends.last()) else stateContact(end, bends.last(), wire.lastRelativePoint)
        return listOf(first) + bends + last
    }

    fun endpoints(start: DiagramNode, end: DiagramNode, preferred: DiagramPoint,
                  lastPreferred: DiagramPoint? = null): Pair<DiagramPoint, DiagramPoint> {
        if (!start.isActivity && !end.isActivity)
            return stateContact(start, end.center, preferred) to stateContact(end, start.center, lastPreferred)
        if (!start.isActivity) {
            val toward = if (validStatePreference(start, preferred)) stateContact(start, end.center, preferred) else start.center
            val b = toward(end, toward)
            return stateContact(start, b, preferred) to b
        }
        if (!end.isActivity) {
            val toward = if (validStatePreference(end, lastPreferred)) stateContact(end, start.center, lastPreferred) else end.center
            val a = toward(start, toward)
            return a to stateContact(end, a, lastPreferred)
        }
        val left = max(start.location.x + 8, end.location.x + 8)
        val right = min(start.location.x + start.width - 8, end.location.x + end.width - 8)
        if (left <= right) {
            val x = preferred.x.coerceIn(left, right)
            val (startY, endY) = closestEdges(start.location.y, start.location.y + start.height,
                end.location.y, end.location.y + end.height)
            return DiagramPoint(x, startY) to DiagramPoint(x, endY)
        }
        val top = max(start.location.y + 8, end.location.y + 8)
        val bottom = min(start.location.y + start.height - 8, end.location.y + end.height - 8)
        if (top <= bottom) {
            val y = preferred.y.coerceIn(top, bottom)
            val (startX, endX) = closestEdges(start.location.x, start.location.x + start.width,
                end.location.x, end.location.x + end.width)
            return DiagramPoint(startX, y) to DiagramPoint(endX, y)
        }
        val a = corner(start, end.center)
        val b = corner(end, start.center)
        return circle(a, b) to circle(b, a)
    }

    private fun closestEdges(startLow: Double, startHigh: Double, endLow: Double, endHigh: Double): Pair<Double, Double> {
        val preferred = if (endLow + endHigh >= startLow + startHigh) startHigh to endLow else startLow to endHigh
        return listOf(preferred, startLow to endLow, startLow to endHigh, startHigh to endLow, startHigh to endHigh)
            .minBy { (a, b) -> kotlin.math.abs(a - b) }
    }

    private fun toward(node: DiagramNode, point: DiagramPoint): DiagramPoint {
        if (!node.isActivity) return node.center
        if (point.x in node.location.x..(node.location.x + node.width)) {
            val inset = minOf(8.0, node.width / 2)
            return DiagramPoint(point.x.coerceIn(node.location.x + inset, node.location.x + node.width - inset),
                node.location.y + if (point.y >= node.center.y) node.height else 0.0)
        }
        if (point.y in node.location.y..(node.location.y + node.height)) {
            return DiagramPoint(if (point.x < node.location.x) node.location.x else node.location.x + node.width,
                point.y.coerceIn(node.location.y + 8, node.location.y + node.height - 8))
        }
        return circle(corner(node, point), point)
    }

    private fun validStatePreference(node: DiagramNode, point: DiagramPoint?): Boolean {
        if (point == null) return false
        val distance = hypot(point.x - node.center.x, point.y - node.center.y)
        return distance > 1e-9 && distance <= 10.0 + 1e-9
    }

    private fun stateContact(node: DiagramNode, toward: DiagramPoint, preferred: DiagramPoint?): DiagramPoint {
        val direction = if (validStatePreference(node, preferred)) preferred!! else toward
        val target = if (hypot(direction.x - node.center.x, direction.y - node.center.y) <= 1e-9)
            node.center + DiagramPoint(1.0, 0.0) else direction
        return circle(node.center, target, 10.0)
    }

    private fun corner(node: DiagramNode, other: DiagramPoint) = DiagramPoint(
        node.location.x + if (other.x >= node.center.x) node.width - 8 else 8.0,
        node.location.y + if (other.y >= node.center.y) node.height - 8 else 8.0)

    private fun circle(center: DiagramPoint, toward: DiagramPoint, radius: Double = 9.0): DiagramPoint {
        val dx = toward.x - center.x
        val dy = toward.y - center.y
        val length = hypot(dx, dy)
        return if (length == 0.0) center else DiagramPoint(center.x + dx * radius / length, center.y + dy * radius / length)
    }
}
