package com.look4counter.modeleditor

import org.jdom.Element
import org.jdom.output.Format
import org.jdom.output.XMLOutputter

object ModelServiceXmlWriter {
    private val attributeOrder = mapOf(
        "service" to listOf("name", "initial"),
        "transaction-manager" to listOf("id", "commit"),
        "activity" to listOf("name", "class"),
        "transition" to listOf("name", "value"),
        "property" to listOf("name", "value"),
    )

    fun write(service: Element): String {
        require(service.name == "service" && service.namespaceURI.isNotEmpty())
        val escaping = XMLOutputter(Format.getRawFormat().setEncoding("UTF-8"))
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            fun appendElement(element: Element, depth: Int) {
                val attributes = attributeOrder.getValue(element.name)
                require(element.attributes.map { it.name }.toSet() == attributes.toSet())
                append("    ".repeat(depth)).append('<').append(element.name)
                for (name in attributes) {
                    append(' ').append(name).append("=\"")
                        .append(escaping.escapeAttributeEntities(element.getAttributeValue(name))).append('"')
                }
                if (depth == 0) append(" xmlns=\"").append(escaping.escapeAttributeEntities(service.namespaceURI)).append('"')
                if (element.children.isEmpty()) append("/>\n")
                else {
                    append(">\n")
                    element.children.forEach { appendElement(it, depth + 1) }
                    append("    ".repeat(depth)).append("</").append(element.name).append(">\n")
                }
            }
            appendElement(service, 0)
        }.removeSuffix("\n")
    }
}
