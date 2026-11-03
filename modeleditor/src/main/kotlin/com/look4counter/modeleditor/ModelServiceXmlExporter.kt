package com.look4counter.modeleditor

import com.intellij.psi.xml.XmlFile
import org.jdom.Element
import org.jdom.Namespace

object ModelServiceXmlExporter {
    private val namespace = Namespace.getNamespace("http://www.poscoict.com/glueframework/service")

    fun export(file: XmlFile, baseName: String): String {
        val diagram = ModelDiagramParser.parse(file)
        require(diagram.message == null) { diagram.message.orEmpty() }
        val root = requireNotNull(file.rootTag)
        val children = root.subTags.filter { it.localName == "child" }
        require(children.size == diagram.nodes.size) { "지원하지 않는 Child 종류가 있습니다." }
        require(diagram.nodes.all { it.id.isNotBlank() } && diagram.nodes.map { it.id }.distinct().size == diagram.nodes.size) {
            "Child ID가 비어 있거나 중복됩니다."
        }
        val initial = diagram.nodes.filter { it.kind == DiagramNodeKind.InitialState }.singleOrNull()
        require(initial != null) { "InitialState는 하나여야 합니다." }
        val outputCounts = diagram.nodes.flatMap { it.outputs }.groupingBy { it }.eachCount()
        val inputCounts = diagram.nodes.flatMap { it.inputs }.groupingBy { it }.eachCount()
        val validWires = diagram.wires.filter { wire ->
            val start = wire.start
            val end = wire.end
            outputCounts[wire.index] == 1 && inputCounts[wire.index] == 1 && start != null && end != null &&
                ((start.kind == DiagramNodeKind.InitialState && end.isActivity) ||
                    (start.isActivity && (end.isActivity || end.kind == DiagramNodeKind.FinalState)))
        }.associateBy { it.index }
        val first = initial.outputs.mapNotNull(validWires::get).singleOrNull()?.end
        val activities = diagram.nodes.filter { it.isActivity }
        require(activities.all { it.name.isNotBlank() } && activities.map { it.name }.distinct().size == activities.size) {
            "Activity Name이 비어 있거나 중복됩니다."
        }
        fun element(name: String) = Element(name, namespace)
        val service = element("service").setAttribute("name", "$baseName-service").setAttribute("initial", first?.name.orEmpty())
        diagram.properties.filter { it.key != "description" }.forEach { property ->
            require(property.key.isNotBlank() && property.value in listOf("true", "false")) {
                "Diagram property ${property.key}의 값은 true 또는 false여야 합니다."
            }
            service.addContent(element("transaction-manager").setAttribute("id", property.key).setAttribute("commit", property.value))
        }
        activities.forEach { node ->
            val tag = children.single { it.getAttributeValue("id") == node.id }
            val properties = tag.subTags.filter { it.localName == "property" }.map {
                ActivityProperty(DiagramXmlText.read(it, "key"), DiagramXmlText.read(it, "value"))
            }
            require(properties.count { it.key == "class" } == 1 && node.className.isNotBlank()) { "${node.name}의 class가 없습니다." }
            val activity = element("activity").setAttribute("name", node.name).setAttribute("class", node.className)
            val transitions = node.outputs.mapNotNull(validWires::get).map { wire ->
                val target = requireNotNull(wire.end)
                wire.condition to if (target.kind == DiagramNodeKind.FinalState) "end" else target.name
            }
            ModelServiceOutputOrder.transitions(transitions).forEach { (condition, target) ->
                activity.addContent(element("transition").setAttribute("name", condition).setAttribute("value", target))
            }
            ModelServiceOutputOrder.properties(properties).forEach { property ->
                require(property.key.isNotBlank()) { "${node.name}에 이름이 없는 property가 있습니다." }
                activity.addContent(element("property").setAttribute("name", property.key).setAttribute("value", property.value))
            }
            service.addContent(activity)
        }
        return ModelServiceXmlWriter.write(service)
    }
}
