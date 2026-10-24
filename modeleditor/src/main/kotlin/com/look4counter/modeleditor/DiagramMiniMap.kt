package com.look4counter.modeleditor

import java.awt.Rectangle
import java.awt.geom.Rectangle2D

data class MiniMapProjection(
    val frame: Rectangle,
    val contentArea: Rectangle2D.Double,
    val modelBounds: Rectangle2D.Double,
    val modelViewport: Rectangle2D.Double,
    val overviewBounds: Rectangle2D.Double,
    val scale: Double,
    val offsetX: Double,
    val offsetY: Double,
) {
    fun map(point: DiagramPoint) = DiagramPoint(point.x * scale + offsetX, point.y * scale + offsetY)
    fun map(rectangle: Rectangle2D): Rectangle2D.Double = Rectangle2D.Double(
        rectangle.x * scale + offsetX, rectangle.y * scale + offsetY, rectangle.width * scale, rectangle.height * scale)
    val viewport get() = map(modelViewport)
}

object DiagramMiniMap {
    fun layout(nodes: List<DiagramNode>, paths: List<List<DiagramPoint>>, labels: List<Rectangle2D>,
               visible: Rectangle, translation: DiagramPoint, zoom: Double = 1.0): MiniMapProjection? {
        if (visible.width < 160 || visible.height < 120 || (nodes.isEmpty() && paths.isEmpty())) return null
        val points = nodes.flatMap { listOf(it.location, it.location + DiagramPoint(it.width, it.height)) } +
            paths.flatten() + labels.flatMap { listOf(DiagramPoint(it.minX, it.minY), DiagramPoint(it.maxX, it.maxY)) }
        if (points.isEmpty()) return null
        val minX = points.minOf { it.x } - 8
        val minY = points.minOf { it.y } - 8
        val bounds = Rectangle2D.Double(minX, minY, maxOf(1.0, points.maxOf { it.x } + 8 - minX),
            maxOf(1.0, points.maxOf { it.y } + 8 - minY))
        val width = minOf(200, maxOf(96, visible.width / 3))
        val height = minOf(140, maxOf(72, visible.height / 3))
        val frame = Rectangle(visible.x + visible.width - width - 12, visible.y + 12, width, height)
        val content = Rectangle2D.Double(frame.x + 8.0, frame.y + 24.0, width - 16.0, height - 32.0)
        val viewport = Rectangle2D.Double((visible.x - translation.x) / zoom, (visible.y - translation.y) / zoom,
            visible.width / zoom, visible.height / zoom)
        val overview = Rectangle2D.Double().apply { setRect(bounds); add(viewport) }
        val scale = minOf(0.5, content.width / overview.width, content.height / overview.height)
        return MiniMapProjection(frame, content, bounds, viewport, overview, scale,
            content.centerX - overview.centerX * scale, content.centerY - overview.centerY * scale)
    }
}
