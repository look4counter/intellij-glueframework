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
        require(initial != null && initial.outputs.size == 1 && initial.inputs.isEmpty()) { "InitialState는 하나이며 시작 연결도 하나여야 합니다." }
        val reference = Regex("^//@wire\\.(\\d+)$")
        children.forEach { child ->
            for (attribute in listOf("input", "output")) {
                val refs = child.getAttributeValue(attribute).orEmpty().split(Regex("\\s+")).filter { it.isNotEmpty() }
                require(refs.distinct().size == refs.size && refs.all { token ->
                    reference.matchEntire(token)?.groupValues?.get(1)?.toIntOrNull() in diagram.wires.indices
                }) { "존재하지 않거나 중복된 Wire 참조가 있습니다." }
            }
        }
        diagram.wires.forEach { wire ->
            require(diagram.nodes.count { wire.index in it.outputs } == 1 && diagram.nodes.count { wire.index in it.inputs } == 1) {
                "Wire ${wire.index}의 양쪽 Child 연결이 올바르지 않습니다."
            }
            require(wire.start?.kind != DiagramNodeKind.FinalState && wire.end?.kind != DiagramNodeKind.InitialState) {
                "InitialState 입력 또는 FinalState 출력 연결은 지원하지 않습니다."
            }
        }
        val first = diagram.wires[initial.outputs.single()].end
        require(first?.isActivity == true) { "InitialState는 Activity에 연결되어야 합니다." }
        val activities = diagram.nodes.filter { it.isActivity }
        require(activities.all { it.name.isNotBlank() } && activities.map { it.name }.distinct().size == activities.size) {
            "Activity Name이 비어 있거나 중복됩니다."
        }
        fun element(name: String) = Element(name, namespace)
        val service = element("service").setAttribute("name", "$baseName-service").setAttribute("initial", first!!.name)
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
            val transitions = node.outputs.map { index ->
                val wire = diagram.wires[index]
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
