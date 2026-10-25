package com.look4counter.modeleditor

object DiagramEdits {
    fun dragMidpoint(wire: DiagramWire, segmentIndex: Int, point: DiagramPoint): DiagramWire {
        val path = DiagramGeometry.path(wire)
        if (segmentIndex !in 0 until path.lastIndex) return wire
        slideDirectActivityConnection(wire, point)?.let { return it }
        val a = path[segmentIndex]
        val b = path[segmentIndex + 1]
        val moved = moveBendpoint(wire, segmentIndex, point, true)
        return if (withinMergeBand(a, b, point) && canMergeAt(moved, segmentIndex)) wire else moved
    }

    private fun slideDirectActivityConnection(wire: DiagramWire, point: DiagramPoint): DiagramWire? {
        val start = wire.start ?: return null
        val end = wire.end ?: return null
        if (wire.bendpoints.isNotEmpty() || !start.isActivity || !end.isActivity) return null
        val preferred = wire.firstRelativePoint ?: start.center
        fun slideIfClear(inRange: Boolean, at: DiagramPoint): DiagramWire {
            val moved = moveBendpoint(wire, 0, point, true)
            return if (inRange && canMergeAt(moved, 0)) wire.copy(firstRelativePoint = at, explicitContacts = false) else moved
        }
        val left = maxOf(start.location.x + 8, end.location.x + 8)
        val right = minOf(start.location.x + start.width - 8, end.location.x + end.width - 8)
        if (left <= right) return slideIfClear(point.x in left..right, preferred.copy(x = point.x))
        val top = maxOf(start.location.y + 8, end.location.y + 8)
        val bottom = minOf(start.location.y + start.height - 8, end.location.y + end.height - 8)
        if (top <= bottom) return slideIfClear(point.y in top..bottom, preferred.copy(y = point.y))
        return null
    }

    private fun withinMergeBand(a: DiagramPoint, b: DiagramPoint, point: DiagramPoint): Boolean {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val length = kotlin.math.hypot(dx, dy)
        val distance = if (length == 0.0) kotlin.math.hypot(point.x - a.x, point.y - a.y)
            else kotlin.math.abs(dx * (point.y - a.y) - dy * (point.x - a.x)) / length
        return distance <= 5.0 + 1e-9
    }

    fun dragBendpoint(wire: DiagramWire, index: Int, point: DiagramPoint): DiagramWire {
        if (index !in wire.bendpoints.indices) return wire
        val start = wire.start ?: return wire
        val end = wire.end ?: return wire
        val absolute = DiagramGeometry.path(wire).drop(1).dropLast(1)
        val retained = wire.bendpoints.mapIndexedNotNull { i, bend ->
            if (i == index) null else {
                val at = absolute[i]
                DiagramBendpoint(DiagramPoint(at.x - start.center.x, at.y - start.center.y),
                    DiagramPoint(at.x - end.center.x, at.y - end.center.y), bend.weight)
            }
        }
        val merged = wire.copy(bendpoints = retained)
        val path = DiagramGeometry.path(merged)
        val moved = moveBendpoint(wire, index, point, false)
        return if (withinMergeBand(path[index], path[index + 1], point) &&
            canMergeAt(moved, index)) merged else moved
    }

    private fun canMergeAt(moved: DiagramWire, index: Int): Boolean {
        val path = DiagramGeometry.path(moved)
        val nodes = listOfNotNull(moved.start, moved.end)
        return canMerge(path[index], path[index + 1], nodes) &&
            canMerge(path[index + 1], path[index + 2], nodes)
    }

    private fun canMerge(a: DiagramPoint, b: DiagramPoint, nodes: List<DiagramNode>): Boolean =
        nodes.none { crossesInterior(a, b, it) }

    fun relocateNodes(wire: DiagramWire, nodes: Map<DiagramNode, DiagramNode>): DiagramWire {
        val start = nodes[wire.start] ?: wire.start
        val end = nodes[wire.end] ?: wire.end
        fun delta(old: DiagramNode?, moved: DiagramNode?): DiagramPoint? {
            if (old == null || moved == null || old.width != moved.width || old.height != moved.height) return null
            return DiagramPoint(moved.location.x - old.location.x, moved.location.y - old.location.y)
        }
        val startDelta = delta(wire.start, start)
        val groupDelta = startDelta?.takeIf { it == delta(wire.end, end) }
        fun translated(old: DiagramNode?, moved: DiagramNode?, point: DiagramPoint?): DiagramPoint? {
            if (point != null && groupDelta != null) return point + groupDelta
            if (old == null || moved == null || old.isActivity || point == null) return point
            return point + DiagramPoint(moved.center.x - old.center.x, moved.center.y - old.center.y)
        }
        fun resizeDelta(old: DiagramNode?, resized: DiagramNode?): DiagramPoint {
            if (old == null || resized == null || old.width == resized.width && old.height == resized.height)
                return DiagramPoint(0.0, 0.0)
            return DiagramPoint(resized.center.x - old.center.x, resized.center.y - old.center.y)
        }
        val startResize = resizeDelta(wire.start, start)
        val endResize = resizeDelta(wire.end, end)
        val resized = startResize != DiagramPoint(0.0, 0.0) || endResize != DiagramPoint(0.0, 0.0)
        val directContact = if (resized && wire.bendpoints.isEmpty() &&
            wire.start?.isActivity == true && wire.end?.isActivity == true && wire.firstRelativePoint == null)
            DiagramGeometry.path(wire).firstOrNull() else null
        val bends = if (!resized) wire.bendpoints
            else wire.bendpoints.map { bend -> bend.copy(
                first = DiagramPoint(bend.first.x - startResize.x, bend.first.y - startResize.y),
                second = DiagramPoint(bend.second.x - endResize.x, bend.second.y - endResize.y)) }
        return wire.copy(start = start, end = end, explicitContacts = wire.explicitContacts && groupDelta != null,
            firstRelativePoint = translated(wire.start, start, wire.firstRelativePoint) ?: directContact,
            lastRelativePoint = translated(wire.end, end, wire.lastRelativePoint), bendpoints = bends)
    }

    private fun crossesInterior(a: DiagramPoint, b: DiagramPoint, node: DiagramNode): Boolean {
        val dx = b.x - a.x
        val dy = b.y - a.y
        fun circle(x: Double, y: Double, radius: Double): Boolean {
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared == 0.0) 0.0
                else (((x - a.x) * dx + (y - a.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val distance = kotlin.math.hypot(a.x + t * dx - x, a.y + t * dy - y)
            return distance < radius - 1e-9
        }
        fun rectangle(left: Double, top: Double, right: Double, bottom: Double): Boolean {
            var low = 0.0
            var high = 1.0
            for ((origin, delta, minimum, maximum) in listOf(
                listOf(a.x, dx, left, right), listOf(a.y, dy, top, bottom))) {
                if (delta == 0.0) {
                    if (origin <= minimum || origin >= maximum) return false
                } else {
                    val first = (minimum - origin) / delta
                    val last = (maximum - origin) / delta
                    low = maxOf(low, minOf(first, last))
                    high = minOf(high, maxOf(first, last))
                }
            }
            return high - low > 1e-9
        }
        if (!node.isActivity) return circle(node.center.x, node.center.y, 10.0)
        val left = node.location.x
        val top = node.location.y
        val right = left + node.width
        val bottom = top + node.height
        val radius = minOf(8.0, node.width / 2, node.height / 2)
        return rectangle(left + radius, top, right - radius, bottom) ||
            rectangle(left, top + radius, right, bottom - radius) ||
            listOf(left + radius, right - radius).any { x ->
                listOf(top + radius, bottom - radius).any { y -> circle(x, y, radius) }
            }
    }

    fun moveBendpoint(wire: DiagramWire, index: Int, point: DiagramPoint, insert: Boolean): DiagramWire {
        val start = wire.start ?: return wire
        val end = wire.end ?: return wire
        fun bend(at: DiagramPoint, weight: Double?) = DiagramBendpoint(
            DiagramPoint(at.x - start.center.x, at.y - start.center.y),
            DiagramPoint(at.x - end.center.x, at.y - end.center.y), weight)
        val bends = if (insert) {
            // Adding a point changes the interpolation ratios. Re-encode existing absolute positions.
            val absolute = DiagramGeometry.path(wire).drop(1).dropLast(1)
            absolute.mapIndexed { i, at -> bend(at, wire.bendpoints[i].weight) }.toMutableList().apply {
                add(index, bend(point, 0.5))
            }
        } else wire.bendpoints.toMutableList().apply { set(index, bend(point, get(index).weight)) }
        return wire.copy(bendpoints = bends)
    }

    fun applyWire(diagram: ModelDiagram, edited: DiagramWire): ModelDiagram {
        val replacements = diagram.nodes.associateWith { node ->
            node.copy(inputs = node.inputs.filter { it != edited.index } + if (node == edited.end) listOf(edited.index) else emptyList(),
                outputs = node.outputs.filter { it != edited.index } + if (node == edited.start) listOf(edited.index) else emptyList())
        }
        return diagram.copy(nodes = diagram.nodes.map { replacements.getValue(it) }, wires = diagram.wires.map {
            val wire = if (it.index == edited.index) edited else it
            wire.copy(start = replacements[wire.start], end = replacements[wire.end])
        })
    }
}
