package com.look4counter.modeleditor

import com.intellij.openapi.progress.ProcessCanceledException
import org.eclipse.elk.alg.layered.options.LayerConstraint
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider
import org.eclipse.elk.alg.layered.options.LayeredOptions
import org.eclipse.elk.alg.layered.options.CrossingMinimizationStrategy
import org.eclipse.elk.alg.layered.options.PortSortingStrategy
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.math.ElkPadding
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.options.Direction
import org.eclipse.elk.core.options.EdgeRouting
import org.eclipse.elk.core.options.PortConstraints
import org.eclipse.elk.core.options.PortSide
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.ElkPort
import org.eclipse.elk.graph.util.ElkGraphUtil
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class ServiceDiagramRoute(val start: DiagramPoint, val end: DiagramPoint, val bends: List<DiagramPoint>) {
    val points get() = listOf(start) + bends + end
}

data class ServiceDiagramPlacement(
    val activities: Map<String, DiagramPoint>, val initial: DiagramPoint, val final: DiagramPoint,
    val routes: List<ServiceDiagramRoute>,
    val widths: Map<String, Double>,
)

object ServiceDiagramLayout {
    private const val ALGORITHM = "org.eclipse.elk.layered"
    private val metadata by lazy {
        // The IDE context class loader need not include this plugin's services.
        LayoutMetaDataService.getInstance(ServiceDiagramLayout::class.java.classLoader).apply {
            if (getAlgorithmData(ALGORITHM) == null) registerLayoutMetaDataProviders(LayeredMetaDataProvider())
        }
    }

    // Even widths keep the fixed center ports on the integer pixel grid.
    fun width(name: String): Double = ceil((name.length * 9.0 + 24).coerceIn(100.0, 480.0) / 2) * 2

    fun arrange(names: List<String>, initial: String, edges: Map<String, List<String>>): ServiceDiagramPlacement {
        require(names.distinct().size == names.size && "end" !in names)
        require(initial.isEmpty() || initial in names)
        require(edges.keys.all { it in names } && edges.values.flatten().all { it == "end" || it in names })
        try {
            metadata
            val graph = ElkGraphUtil.createGraph().apply {
                identifier = "service"
                setProperty(CoreOptions.ALGORITHM, ALGORITHM)
                setProperty(CoreOptions.DIRECTION, Direction.DOWN)
                setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL)
                setProperty(CoreOptions.RANDOM_SEED, 1)
                setProperty(CoreOptions.PADDING, ElkPadding(40.0))
                setProperty(CoreOptions.SEPARATE_CONNECTED_COMPONENTS, false)
                setProperty(CoreOptions.SPACING_NODE_NODE, 60.0)
                setProperty(CoreOptions.SPACING_EDGE_EDGE, 20.0)
                setProperty(CoreOptions.SPACING_EDGE_NODE, 24.0)
                setProperty(CoreOptions.SPACING_PORT_PORT, 18.0)
                setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, 80.0)
                setProperty(LayeredOptions.SPACING_EDGE_EDGE_BETWEEN_LAYERS, 20.0)
                setProperty(LayeredOptions.SPACING_EDGE_NODE_BETWEEN_LAYERS, 24.0)
                setProperty(LayeredOptions.MERGE_EDGES, false)
                setProperty(LayeredOptions.CROSSING_MINIMIZATION_STRATEGY, CrossingMinimizationStrategy.LAYER_SWEEP)
                setProperty(LayeredOptions.PORT_SORTING_STRATEGY, PortSortingStrategy.INPUT_ORDER)
            }
            fun node(id: String, nodeWidth: Double, nodeHeight: Double): ElkNode = ElkGraphUtil.createNode(graph).apply {
                identifier = id
                width = nodeWidth; height = nodeHeight
                setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_SIDE)
            }
            val start = node("initial", 20.0, 20.0).apply {
                setProperty(LayeredOptions.LAYERING_LAYER_CONSTRAINT, LayerConstraint.FIRST_SEPARATE)
            }
            val finish = node("final", 20.0, 20.0).apply {
                setProperty(LayeredOptions.LAYERING_LAYER_CONSTRAINT, LayerConstraint.LAST_SEPARATE)
            }
            val indices = names.mapIndexed { i, name -> name to i + 2 }.toMap()
            val connections = mutableListOf<Pair<Int, Int>>()
            indices[initial]?.let { connections += 0 to it }
            names.forEach { name -> edges[name].orEmpty().forEach { target ->
                connections += indices.getValue(name) to if (target == "end") 1 else indices.getValue(target)
            } }
            val incoming = IntArray(names.size + 2)
            val outgoing = IntArray(names.size + 2)
            connections.forEach { (from, to) -> outgoing[from]++; incoming[to]++ }
            val widths = names.mapIndexed { i, name -> name to maxOf(width(name), (maxOf(incoming[i + 2], outgoing[i + 2]) + 1) * 18.0) }.toMap()
            val nodes = names.mapIndexed { i, name -> name to node("activity:$i", widths.getValue(name), 30.0) }.toMap()
            fun port(owner: ElkNode, output: Boolean, edgeIndex: Int): ElkPort = ElkGraphUtil.createPort(owner).apply {
                identifier = "wire:$edgeIndex:${if (output) "out" else "in"}"
                width = 0.0; height = 0.0
                setProperty(CoreOptions.PORT_SIDE, if (output) PortSide.SOUTH else PortSide.NORTH)
            }
            val all = listOf(start, finish) + nodes.values
            val wires = mutableListOf<ElkEdge>()
            connections.forEachIndexed { index, (from, to) ->
                val source = port(all[from], true, index)
                val target = port(all[to], false, index)
                wires += ElkGraphUtil.createSimpleEdge(source, target).apply {
                    identifier = "wire:$index"
                }
            }
            RecursiveGraphLayoutEngine().layout(graph, BasicProgressMonitor())
            fun point(x: Double, y: Double): DiagramPoint {
                require(x.isFinite() && y.isFinite()) { "ELK가 유한하지 않은 좌표를 반환했습니다." }
                return DiagramPoint(x.roundToLong().toDouble(), y.roundToLong().toDouble())
            }
            fun contact(x: Double, y: Double, nodeIndex: Int, output: Boolean): DiagramPoint {
                if (nodeIndex >= 2) return point(x, y)
                val owner = all[nodeIndex]
                val dx = (x - owner.x - 10).coerceIn(-10.0, 10.0)
                val dy = sqrt(maxOf(0.0, 100 - dx * dx))
                return point(x, owner.y + 10 + if (output) dy else -dy)
            }
            val routes = wires.mapIndexed { index, wire ->
                val section = requireNotNull(wire.sections.singleOrNull()) { "ELK의 단일 연결 경로를 확인할 수 없습니다." }
                ServiceDiagramRoute(contact(section.startX, section.startY, connections[index].first, true),
                    contact(section.endX, section.endY, connections[index].second, false),
                    section.bendPoints.map { point(it.x, it.y) })
            }
            return ServiceDiagramPlacement(nodes.mapValues { (_, node) -> point(node.x, node.y) },
                point(start.x, start.y), point(finish.x, finish.y), routes, widths)
        } catch (cancelled: ProcessCanceledException) {
            throw cancelled
        } catch (error: RuntimeException) {
            throw IllegalArgumentException("ELK Layered 배치를 계산하지 못했습니다: ${error.message.orEmpty()}", error)
        }
    }
}
