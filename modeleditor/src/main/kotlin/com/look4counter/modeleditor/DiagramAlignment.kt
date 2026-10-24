package com.look4counter.modeleditor

import kotlin.math.abs

data class AlignmentGuide(val from: DiagramPoint, val to: DiagramPoint, val distance: Double? = null)
data class AlignmentResult(val delta: DiagramPoint, val guides: List<AlignmentGuide>)

object DiagramAlignment {
    private const val threshold = 5.0
    private data class MoveMatch(val correction: Double, val guides: (DiagramNode) -> List<AlignmentGuide>,
                                 val valid: (DiagramNode) -> Boolean = { true })

    private fun start(node: DiagramNode, x: Boolean) = if (x) node.location.x else node.location.y
    private fun size(node: DiagramNode, x: Boolean) = if (x) node.width else node.height
    private fun end(node: DiagramNode, x: Boolean) = start(node, x) + size(node, x)

    private fun sharedLane(a: DiagramNode, b: DiagramNode, group: DiagramNode, x: Boolean): Boolean =
        maxOf(start(a, !x), start(b, !x), start(group, !x)) < minOf(end(a, !x), end(b, !x), end(group, !x))

    private fun spacingMatch(group: DiagramNode, targets: List<DiagramNode>, x: Boolean): MoveMatch? {
        val lane = targets.filter { maxOf(start(it, !x), start(group, !x)) < minOf(end(it, !x), end(group, !x)) }
            .sortedBy { start(it, x) }
        val maximumEnd = DoubleArray(lane.size)
        lane.forEachIndexed { index, node -> maximumEnd[index] = maxOf(end(node, x), maximumEnd.getOrElse(index - 1) { Double.NEGATIVE_INFINITY }) }
        fun clear(position: Double): Boolean {
            var low = 0; var high = lane.size
            while (low < high) {
                val middle = (low + high) / 2
                if (start(lane[middle], x) < position + size(group, x)) low = middle + 1 else high = middle
            }
            return low == 0 || maximumEnd[low - 1] <= position
        }
        var best: MoveMatch? = null
        for ((a, b) in lane.zipWithNext()) {
            val gap = start(b, x) - end(a, x)
            if (gap < 0 || !sharedLane(a, b, group, x)) continue
            fun candidate(position: Double, between: Boolean, before: Boolean = false) {
                val correction = position - start(group, x)
                if (abs(correction) > threshold || (best != null && abs(correction) >= abs(best!!.correction)) || !clear(position)) return
                best = MoveMatch(correction, { moved ->
                    val ordered = if (between) listOf(a, moved, b) else if (before) listOf(moved, a, b) else listOf(a, b, moved)
                    val level = if (x) ordered.minOf { it.location.y } - 14 else ordered.maxOf { it.location.x + it.width } + 14
                    ordered.zipWithNext().map { (first, second) ->
                        val from = end(first, x); val to = start(second, x)
                        if (x) AlignmentGuide(DiagramPoint(from, level), DiagramPoint(to, level), to - from)
                        else AlignmentGuide(DiagramPoint(level, from), DiagramPoint(level, to), to - from)
                    }
                }, { moved -> sharedLane(a, b, moved, x) && targets.none { target ->
                    maxOf(moved.location.x, target.location.x) < minOf(moved.location.x + moved.width, target.location.x + target.width) &&
                        maxOf(moved.location.y, target.location.y) < minOf(moved.location.y + moved.height, target.location.y + target.height)
                } })
            }
            if (gap >= size(group, x)) candidate((end(a, x) + start(b, x) - size(group, x)) / 2, between = true)
            candidate(start(a, x) - gap - size(group, x), between = false, before = true)
            candidate(end(b, x) + gap, between = false)
        }
        return best
    }
    private fun anchors(node: DiagramNode, x: Boolean): List<Double> = if (x)
        listOf(node.location.x, node.center.x, node.location.x + node.width)
        else listOf(node.location.y, node.center.y, node.location.y + node.height)

    fun move(selected: List<DiagramNode>, targets: List<DiagramNode>, raw: DiagramPoint): AlignmentResult {
        if (selected.isEmpty()) return AlignmentResult(raw, emptyList())
        val left = selected.minOf { it.location.x }; val top = selected.minOf { it.location.y }
        val right = selected.maxOf { it.location.x + it.width }; val bottom = selected.maxOf { it.location.y + it.height }
        val group = DiagramNode(DiagramNodeKind.Activity, "", "", DiagramPoint(left, top), right - left, bottom - top)
        fun edgeMatch(x: Boolean): MoveMatch? {
            val movement = if (x) raw.x else raw.y
            val match = targets.flatMap { target -> anchors(target, x).flatMap { anchor ->
                anchors(group, x).map { own -> Triple(anchor - own - movement, anchor, target) }
            } }.filter { abs(it.first) <= threshold }.minByOrNull { abs(it.first) } ?: return null
            val target = match.third
            return MoveMatch(match.first, { moved -> listOf(if (x)
                AlignmentGuide(DiagramPoint(match.second, minOf(moved.location.y, target.location.y) - 8),
                    DiagramPoint(match.second, maxOf(moved.location.y + moved.height, target.location.y + target.height) + 8))
                else AlignmentGuide(DiagramPoint(minOf(moved.location.x, target.location.x) - 8, match.second),
                    DiagramPoint(maxOf(moved.location.x + moved.width, target.location.x + target.width) + 8, match.second))) })
        }
        val edgeX = edgeMatch(true); val edgeY = edgeMatch(false)
        val aligned = group.copy(location = group.location + raw + DiagramPoint(edgeX?.correction ?: 0.0, edgeY?.correction ?: 0.0))
        // Spacing corrections are measured from the raw pointer, just like edge alignment.
        val spacingX = spacingMatch(aligned.copy(location = DiagramPoint(left + raw.x, aligned.location.y)), targets, true)
        val spacingY = spacingMatch(aligned.copy(location = DiagramPoint(aligned.location.x, top + raw.y)), targets, false)
        fun choose(edge: MoveMatch?, spacing: MoveMatch?) = if (spacing != null &&
            (edge == null || abs(spacing.correction) < abs(edge.correction))) spacing else edge
        var matchX = choose(edgeX, spacingX); var matchY = choose(edgeY, spacingY)
        fun moved() = group.copy(location = group.location + raw + DiagramPoint(matchX?.correction ?: 0.0, matchY?.correction ?: 0.0))
        repeat(2) {
            if (matchX?.valid?.invoke(moved()) == false) matchX = edgeX
            if (matchY?.valid?.invoke(moved()) == false) matchY = edgeY
        }
        val moved = moved()
        val guides = mutableListOf<AlignmentGuide>()
        fun add(edge: MoveMatch?, spacing: MoveMatch?, chosen: MoveMatch?) {
            chosen ?: return
            guides += chosen.guides(moved)
            if (chosen === edge && spacing != null && abs(spacing.correction - chosen.correction) < 0.000001 && spacing.valid(moved))
                guides += spacing.guides(moved)
        }
        add(edgeX, spacingX, matchX); add(edgeY, spacingY, matchY)
        return AlignmentResult(DiagramPoint(raw.x + (matchX?.correction ?: 0.0), raw.y + (matchY?.correction ?: 0.0)), guides)
    }

    fun resize(selected: List<DiagramNode>, targets: List<DiagramNode>, raw: Double, left: Boolean): AlignmentResult {
        val minimum = 16.0 - selected.minOf { it.width }
        val delta = maxOf(raw, minimum)
        data class Candidate(val delta: Double, val guide: AlignmentGuide)
        val candidates = selected.flatMap { own -> targets.flatMap { target ->
            val edge = if (left) own.location.x else own.location.x + own.width
            val direction = if (left) -1 else 1
            val alignment = anchors(target, true).map { anchor ->
                Candidate((anchor - edge) * direction, AlignmentGuide(
                    DiagramPoint(anchor, minOf(own.location.y, target.location.y) - 8),
                    DiagramPoint(anchor, maxOf(own.location.y + own.height, target.location.y + target.height) + 8)))
            }
            if (!target.isActivity) alignment else alignment + Candidate(target.width - own.width,
                AlignmentGuide(DiagramPoint(target.location.x, target.location.y - 8),
                    DiagramPoint(target.location.x + target.width, target.location.y - 8)))
        } }.filter { it.delta >= minimum && abs(it.delta - delta) <= threshold }
        val best = candidates.minByOrNull { abs(it.delta - delta) }
        return AlignmentResult(DiagramPoint(best?.delta ?: delta, 0.0), best?.let { listOf(it.guide) }.orEmpty())
    }
}
