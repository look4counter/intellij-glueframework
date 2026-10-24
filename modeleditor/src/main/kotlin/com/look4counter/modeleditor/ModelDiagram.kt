package com.look4counter.modeleditor

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.openapi.util.Key

internal val explicitWireContactsKey = Key.create<Boolean>("glue.diagram.explicitWireContacts")

data class DiagramPoint(val x: Double, val y: Double) {
    operator fun plus(other: DiagramPoint) = DiagramPoint(x + other.x, y + other.y)
}

enum class DiagramNodeKind { Activity, InitialState, FinalState }

data class DiagramNode(
    val kind: DiagramNodeKind, val id: String, val name: String,
    val location: DiagramPoint, val width: Double, val height: Double,
    val inputs: List<Int> = emptyList(), val outputs: List<Int> = emptyList(),
    val className: String = "",
) {
    val center get() = DiagramPoint(location.x + width / 2, location.y + height / 2)
    val isActivity get() = kind == DiagramNodeKind.Activity
}

data class DiagramBendpoint(val first: DiagramPoint, val second: DiagramPoint, val weight: Double?)
data class DiagramWire(
    val index: Int, val id: String, val condition: String,
    val start: DiagramNode?, val end: DiagramNode?,
    val firstRelativePoint: DiagramPoint?, val lastRelativePoint: DiagramPoint?,
    val bendpoints: List<DiagramBendpoint>,
    val explicitContacts: Boolean = false,
) {
    val canEditCondition get() = start?.kind != DiagramNodeKind.InitialState
    val showsCondition get() = canEditCondition && condition.isNotBlank()
}

enum class DiagramChangeKind { Added, Removed, Modified }

data class DiagramChangeHighlights(
    val nodeChanges: Map<String, DiagramChangeKind> = emptyMap(),
    val wireChanges: Map<String, DiagramChangeKind> = emptyMap(),
    val removedNodes: List<DiagramNode> = emptyList(),
    val removedWires: List<DiagramWire> = emptyList(),
    val diagramPropertiesChanged: Boolean = false,
) {
    val isEmpty get() = nodeChanges.isEmpty() && wireChanges.isEmpty() && !diagramPropertiesChanged
}
data class ModelDiagram(val nodes: List<DiagramNode>, val wires: List<DiagramWire>, val message: String? = null,
                        val properties: List<ActivityProperty> = emptyList())

object ModelDiagramParser {
    private val reference = Regex("^//@wire\\.(\\d+)$")

    fun parse(file: XmlFile): ModelDiagram {
        if (PsiTreeUtil.findChildOfType(file, PsiErrorElement::class.java) != null)
            return ModelDiagram(emptyList(), emptyList(), "XML 오류를 Source 탭에서 확인하세요.")
        val root = file.rootTag
        if (root == null || root.localName != "GlueDiagram")
            return ModelDiagram(emptyList(), emptyList(), "표시할 GlueDiagram이 없습니다.")
        val nodes = root.subTags.filter { it.localName == "child" }.mapNotNull { tag ->
            val type = tag.getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance")?.substringAfterLast(':')
            val kind = DiagramNodeKind.entries.firstOrNull { it.name == type } ?: return@mapNotNull null
            val activity = kind == DiagramNodeKind.Activity
            DiagramNode(kind, tag.getAttributeValue("id").orEmpty(), DiagramXmlText.read(tag, "name"),
                point(tag.getAttributeValue("location")) ?: DiagramPoint(0.0, 0.0),
                if (activity) point(tag.getAttributeValue("size"))?.x?.takeIf { it > 0 } ?: 100.0 else 20.0,
                if (activity) 30.0 else 20.0, refs(tag, "input"), refs(tag, "output"),
                tag.subTags.firstOrNull { it.localName == "property" && DiagramXmlText.read(it, "key") == "class" }
                    ?.let { DiagramXmlText.read(it, "value").trim() }.orEmpty())
        }
        val wires = root.subTags.filter { it.localName == "wire" }.mapIndexed { index, tag ->
            DiagramWire(index, tag.getAttributeValue("id").orEmpty(), DiagramXmlText.read(tag, "guardcondition").ifBlank { "success" },
                nodes.firstOrNull { index in it.outputs }, nodes.firstOrNull { index in it.inputs },
                point(tag.getAttributeValue("firstRelativePoint")), point(tag.getAttributeValue("lastRelativePoint")),
                tag.subTags.filter { it.localName == "bendpoint" }.map { bend ->
                    DiagramBendpoint(point(bend.getAttributeValue("firstDimension")) ?: DiagramPoint(0.0, 0.0),
                        point(bend.getAttributeValue("secondDimension")) ?: DiagramPoint(0.0, 0.0),
                        bend.getAttributeValue("weight")?.toDoubleOrNull()?.takeIf { it.isFinite() })
                }, tag.getUserData(explicitWireContactsKey) == true)
        }
        return ModelDiagram(nodes, wires, properties = root.subTags.filter { it.localName == "property" }.map {
            ActivityProperty(DiagramXmlText.read(it, "key"), DiagramXmlText.read(it, "value"))
        })
    }

    private fun refs(tag: XmlTag, attribute: String): List<Int> = tag.getAttributeValue(attribute).orEmpty()
        .split(Regex("\\s+")).mapNotNull { reference.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }

    private fun point(value: String?): DiagramPoint? {
        val parts = value?.split(',') ?: return null
        if (parts.size != 2) return null
        val x = parts[0].trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val y = parts[1].trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        return DiagramPoint(x, y)
    }
}
