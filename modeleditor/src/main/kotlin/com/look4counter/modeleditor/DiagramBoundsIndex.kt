package com.look4counter.modeleditor

import java.awt.geom.Rectangle2D
import java.util.TreeMap

internal class DiagramBoundsIndex {
    private val entries = mutableMapOf<Long, Rectangle2D.Double>()
    private val x = TreeMap<Double, Int>()
    private val y = TreeMap<Double, Int>()

    fun clear() { entries.clear(); x.clear(); y.clear() }

    fun set(key: Long, bounds: Rectangle2D?) {
        if (entries[key] == bounds) return
        fun change(axis: TreeMap<Double, Int>, value: Double, delta: Int) {
            val count = (axis[value] ?: 0) + delta
            if (count == 0) axis.remove(value) else axis[value] = count
        }
        fun change(rect: Rectangle2D, delta: Int) {
            change(x, rect.minX, delta); change(x, rect.maxX, delta)
            change(y, rect.minY, delta); change(y, rect.maxY, delta)
        }
        entries.remove(key)?.let { change(it, -1) }
        if (bounds != null) {
            val copy = Rectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height)
            entries[key] = copy
            change(copy, 1)
        }
    }

    fun bounds(): Rectangle2D.Double? {
        if (entries.isEmpty()) return null
        return Rectangle2D.Double(x.firstKey() - 8, y.firstKey() - 8,
            maxOf(1.0, x.lastKey() - x.firstKey() + 16), maxOf(1.0, y.lastKey() - y.firstKey() + 16))
    }
}
