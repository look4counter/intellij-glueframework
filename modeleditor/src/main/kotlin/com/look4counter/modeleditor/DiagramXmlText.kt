package com.look4counter.modeleditor

import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.XmlUtil
import kotlin.math.roundToLong

object DiagramXmlText {
    fun coordinate(value: Double): String {
        require(value.isFinite())
        return value.roundToLong().toString()
    }

    fun point(value: DiagramPoint): String = "${coordinate(value.x)},${coordinate(value.y)}"

    fun normalizeCoordinates(tag: XmlTag) {
        for (attribute in listOf("location", "size", "firstRelativePoint", "lastRelativePoint", "firstDimension", "secondDimension")) {
            val original = tag.getAttributeValue(attribute) ?: continue
            val parts = original.split(',')
            if (parts.size != 2) continue
            val values = parts.map { it.trim().toDoubleOrNull()?.takeIf(Double::isFinite) }
            if (values.any { it == null }) continue
            val normalized = point(DiagramPoint(values[0]!!, values[1]!!))
            if (normalized != original) tag.setAttribute(attribute, normalized)
        }
        tag.subTags.filter { it.localName == "bendpoint" }.forEach(::normalizeCoordinates)
    }

    fun read(tag: XmlTag, attribute: String): String = tag.getAttribute(attribute)?.displayValue.orEmpty()

    fun write(tag: XmlTag, attribute: String, value: String) {
        tag.setAttribute(attribute, XmlUtil.escape(value)
            .replace("\r", "&#13;").replace("\n", "&#10;").replace("\t", "&#9;"))
    }
}
