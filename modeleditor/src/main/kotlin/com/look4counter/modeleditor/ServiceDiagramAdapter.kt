package com.look4counter.modeleditor

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jdom.Element
import org.jdom.Namespace
import org.jdom.output.XMLOutputter
import java.util.IdentityHashMap

class ServiceDiagramAdapter(private val project: Project, private val file: VirtualFile) {
    companion object {
        const val SERVICE_NAMESPACE = "http://www.poscoict.com/glueframework/service"
        fun accepts(project: Project, file: VirtualFile): Boolean =
            ModelEditorSettings.getInstance(project).state.noUseGlueUmlAdFile && file.name.endsWith("-service.xml", true)
    }

    private var stamp = -1L
    private var projected: XmlFile? = null
    private var original: XmlFile? = null
    private var pendingChanges = false
    private var committedDiagram: ModelDiagram? = null
    private val activities = mutableMapOf<String, XmlTag>()
    private val transitions = mutableMapOf<String, XmlTag>()
    private val properties = IdentityHashMap<XmlTag, XmlTag>()
    private fun XmlTag.serviceChildren(name: String) = subTags.filter { it.localName == name && it.namespace == SERVICE_NAMESPACE }
    private fun stamp() = FileDocumentManager.getInstance().getDocument(file)?.modificationStamp ?: -1L
    fun invalidate() { if (!pendingChanges) projected = null }
    fun hasPendingChanges() = pendingChanges && !changeHighlights().isEmpty
    fun markPendingChanges() { pendingChanges = true }
    fun pendingView(): XmlFile? = projected
    fun rollback() {
        pendingChanges = false
        projected = null
    }

    fun changeHighlights(): DiagramChangeHighlights {
        if (!pendingChanges) return DiagramChangeHighlights()
        val baseline = committedDiagram ?: return DiagramChangeHighlights()
        val current = projected?.let(ModelDiagramParser::parse) ?: return DiagramChangeHighlights()
        val beforeNodes = baseline.nodes.associateBy { it.id }
        val afterNodes = current.nodes.associateBy { it.id }
        val nodeChanges = linkedMapOf<String, DiagramChangeKind>()
        afterNodes.forEach { (id, node) ->
            val before = beforeNodes[id]
            nodeChanges[id] = when {
                before == null -> DiagramChangeKind.Added
                before != node -> DiagramChangeKind.Modified
                else -> null
            } ?: return@forEach
        }
        beforeNodes.keys.filter { it !in afterNodes }.forEach { nodeChanges[it] = DiagramChangeKind.Removed }
        val beforeWires = baseline.wires.associateBy { it.id }
        val afterWires = current.wires.associateBy { it.id }
        fun sameWire(a: DiagramWire, b: DiagramWire) = a.condition == b.condition &&
            a.start?.let { it.id to it.kind } == b.start?.let { it.id to it.kind } &&
            a.end?.let { it.id to it.kind } == b.end?.let { it.id to it.kind } &&
            a.firstRelativePoint == b.firstRelativePoint && a.lastRelativePoint == b.lastRelativePoint && a.bendpoints == b.bendpoints
        val wireChanges = linkedMapOf<String, DiagramChangeKind>()
        afterWires.forEach { (id, wire) ->
            val before = beforeWires[id]
            when {
                before == null -> wireChanges[id] = DiagramChangeKind.Added
                !sameWire(before, wire) -> wireChanges[id] = DiagramChangeKind.Modified
            }
        }
        beforeWires.keys.filter { it !in afterWires }.forEach { wireChanges[it] = DiagramChangeKind.Removed }
        return DiagramChangeHighlights(nodeChanges, wireChanges,
            baseline.nodes.filter { nodeChanges[it.id] == DiagramChangeKind.Removed },
            baseline.wires.filter { wireChanges[it.id] == DiagramChangeKind.Removed },
            baseline.properties != current.properties)
    }

    fun read(source: XmlFile): XmlFile {
        if (projected != null && (stamp == stamp() || pendingChanges)) return projected!!
        require(PsiTreeUtil.findChildOfType(source, PsiErrorElement::class.java) == null) { "Service XML 오류를 Source 탭에서 확인하세요." }
        val root = requireNotNull(source.rootTag) { "Service XML 루트가 없습니다." }
        require(root.localName == "service" && root.namespace == SERVICE_NAMESPACE) { "Glue Service XML 형식을 확인하세요." }
        val tags = root.serviceChildren("activity")
        val names = tags.map { DiagramXmlText.read(it, "name") }
        require(names.all { it.isNotBlank() && it != "end" } && names.distinct().size == names.size) { "Activity 이름이 비어 있거나 중복되거나 end입니다." }
        tags.forEach { tag ->
            val keys = tag.serviceChildren("property").map { DiagramXmlText.read(it, "name") }
            require(keys.none { it.isBlank() || it == "class" } && keys.distinct().size == keys.size) { "Activity property 이름이 비어 있거나 중복되거나 class입니다." }
        }
        val initialName = DiagramXmlText.read(root, "initial")
        require(initialName.isEmpty() || initialName in names) { "initial 대상 Activity를 찾을 수 없습니다: $initialName" }
        val edges = tags.associate { tag -> DiagramXmlText.read(tag, "name") to tag.serviceChildren("transition").map {
            DiagramXmlText.read(it, "value").also { target -> require(target == "end" || target in names) { "transition 대상을 찾을 수 없습니다: $target" } }
        } }
        val placement = ServiceDiagramLayout.arrange(names, initialName, edges)
        val xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
        val model = Element("GlueDiagram").also { it.addNamespaceDeclaration(xsi) }
        val childByName = linkedMapOf<String, Element>()
        val sourceActivities = mutableMapOf<String, XmlTag>()
        val sourceTransitions = mutableMapOf<String, XmlTag>()
        fun child(id: String, type: String, at: DiagramPoint, width: Double): Element = Element("child")
            .setAttribute("id", id).setAttribute("type", type, xsi).setAttribute("location", DiagramXmlText.point(at))
            .setAttribute("size", "${DiagramXmlText.coordinate(width)},-1").also { model.addContent(it) }
        val start = child("0", "InitialState", placement.initial, -1.0)
        val finish = child("1", "FinalState", placement.final, -1.0)
        tags.forEachIndexed { i, tag ->
            val name = names[i]
            val node = child((i + 2).toString(), "Activity", placement.activities.getValue(name), placement.widths.getValue(name)).setAttribute("name", name)
            node.addContent(Element("property").setAttribute("key", "class").setAttribute("value", DiagramXmlText.read(tag, "class")))
            tag.serviceChildren("property").forEach {
                node.addContent(Element("property").setAttribute("key", DiagramXmlText.read(it, "name")).setAttribute("value", DiagramXmlText.read(it, "value")))
            }
            sourceActivities[(i + 2).toString()] = tag
            childByName[name] = node
        }
        root.serviceChildren("transaction-manager").forEach {
            model.addContent(Element("property").setAttribute("key", DiagramXmlText.read(it, "id")).setAttribute("value", DiagramXmlText.read(it, "commit")))
        }
        root.serviceChildren("property").firstOrNull { DiagramXmlText.read(it, "name") == "description" }?.let {
            model.addContent(Element("property").setAttribute("key", "description").setAttribute("value", DiagramXmlText.read(it, "value")))
        }
        var wireIndex = 0
        fun wire(from: Element, to: Element, condition: String, sourceTag: XmlTag?) {
            val index = wireIndex++
            val id = (tags.size + 2 + index).toString()
            val tag = Element("wire").setAttribute("id", id).setAttribute("guardcondition", condition.ifBlank { "success" })
            fun center(node: Element): DiagramPoint {
                val at = node.getAttributeValue("location").split(',').map(String::toDouble)
                val activity = node.getAttributeValue("type", xsi) == "Activity"
                return DiagramPoint(at[0] + (if (activity) node.getAttributeValue("size").substringBefore(',').toDouble() else 20.0) / 2, at[1] + if (activity) 15.0 else 10.0)
            }
            val fromCenter = center(from)
            val toCenter = center(to)
            val route = placement.routes[index]
            tag.setAttribute("firstRelativePoint", DiagramXmlText.point(route.start))
            tag.setAttribute("lastRelativePoint", DiagramXmlText.point(route.end))
            route.bends.forEach { at ->
                tag.addContent(Element("bendpoint")
                    .setAttribute("firstDimension", DiagramXmlText.point(DiagramPoint(at.x - fromCenter.x, at.y - fromCenter.y)))
                    .setAttribute("secondDimension", DiagramXmlText.point(DiagramPoint(at.x - toCenter.x, at.y - toCenter.y))))
            }
            fun ref(node: Element, name: String) { node.setAttribute(name, listOfNotNull(node.getAttributeValue(name), "//@wire.$index").joinToString(" ")) }
            ref(from, "output"); ref(to, "input")
            model.addContent(tag)
            if (sourceTag != null) sourceTransitions[id] = sourceTag
        }
        childByName[initialName]?.let { wire(start, it, "success", null) }
        tags.forEachIndexed { i, tag -> tag.serviceChildren("transition").forEach {
            wire(childByName.getValue(names[i]), if (DiagramXmlText.read(it, "value") == "end") finish else childByName.getValue(DiagramXmlText.read(it, "value")), DiagramXmlText.read(it, "name"), it)
        } }
        val xml = PsiFileFactory.getInstance(project).createFileFromText("service-view.glue_uml_ad", GlueUmlAdFileType.INSTANCE, XMLOutputter().outputString(model)) as XmlFile
        val viewRoot = xml.rootTag!!
        viewRoot.subTags.filter { it.localName == "wire" }.forEach { it.putUserData(explicitWireContactsKey, true) }
        activities.clear(); activities.putAll(sourceActivities)
        transitions.clear(); transitions.putAll(sourceTransitions)
        original = source
        projected = xml
        stamp = stamp()
        pendingChanges = false
        committedDiagram = ModelDiagramParser.parse(xml)
        rebuildViewLinks(xml)
        return xml
    }

    private fun rebuildViewLinks(view: XmlFile) {
        val root = requireNotNull(original).rootTag!!
        val viewRoot = view.rootTag!!
        viewRoot.subTags.filter { it.localName == "wire" }.forEach { it.putUserData(explicitWireContactsKey, true) }
        properties.clear()
        viewRoot.subTags.filter { it.localName == "child" }.forEach { node ->
            val target = activities[node.getAttributeValue("id")] ?: return@forEach
            val actual = target.serviceChildren("property").associateBy { DiagramXmlText.read(it, "name") }
            node.subTags.filter { it.localName == "property" && DiagramXmlText.read(it, "key") != "class" }
                .forEach { property -> actual[DiagramXmlText.read(property, "key")]?.let { properties[property] = it } }
        }
        val actualRootProperties = root.serviceChildren("transaction-manager").associateBy { DiagramXmlText.read(it, "id") } +
            root.serviceChildren("property").filter { DiagramXmlText.read(it, "name") == "description" }
                .associateBy { DiagramXmlText.read(it, "name") }
        viewRoot.subTags.filter { it.localName == "property" }.forEach { property ->
            actualRootProperties[DiagramXmlText.read(property, "key")]?.let { properties[property] = it }
        }
    }

    fun source(): XmlFile = requireNotNull(original)

    fun validate(view: XmlFile) {
        require(stamp == stamp()) { "Source XML이 편집 중 변경되었습니다. 변경 내용을 다시 읽은 뒤 Commit하세요." }
        val diagram = ModelDiagramParser.parse(view)
        require(diagram.message == null) { diagram.message.orEmpty() }
        val nodes = diagram.nodes.filter { it.isActivity }
        require(nodes.all { it.name.isNotBlank() && it.name != "end" } && nodes.map { it.name }.distinct().size == nodes.size) { "Activity 이름은 비어 있거나 중복되거나 end일 수 없습니다." }
        val starts = diagram.nodes.filter { it.kind == DiagramNodeKind.InitialState }
        require(starts.size <= 1 && starts.all { it.inputs.isEmpty() && it.outputs.size <= 1 }) { "InitialState는 하나의 Activity에만 연결할 수 있습니다." }
        require(diagram.nodes.count { it.kind == DiagramNodeKind.FinalState } <= 1) { "Service XML은 FinalState를 자동으로 하나만 표시합니다." }
        require(diagram.wires.all { it.start != null && it.end != null && it.start.kind != DiagramNodeKind.FinalState && it.end.kind != DiagramNodeKind.InitialState && (it.start.isActivity || it.end.isActivity) }) { "Service로 표현할 수 없는 Wire 연결입니다." }
        val root = requireNotNull(view.rootTag) { "편집 중인 Diagram 루트가 없습니다." }
        root.subTags.filter { it.localName == "child" }.forEach { node ->
            val keys = node.subTags.filter { it.localName == "property" }.map { DiagramXmlText.read(it, "key") }
            require(keys.all { it.isNotBlank() } && keys.distinct().size == keys.size) { "Activity property 이름이 비어 있거나 중복됩니다." }
        }
        val rootProperties = root.subTags.filter { it.localName == "property" }
        val rootKeys = rootProperties.map { DiagramXmlText.read(it, "key") }
        require(rootKeys.all { it.isNotBlank() } && rootKeys.distinct().size == rootKeys.size) { "Diagram property 이름이 비어 있거나 중복됩니다." }
        require(rootProperties.filter { DiagramXmlText.read(it, "key") != "description" }.all { DiagramXmlText.read(it, "value") in listOf("true", "false") }) { "transaction-manager commit은 true 또는 false여야 합니다." }
    }

    fun apply(view: XmlFile) {
        validate(view)
        val diagram = ModelDiagramParser.parse(view)
        val nodes = diagram.nodes.filter { it.isActivity }
        val starts = diagram.nodes.filter { it.kind == DiagramNodeKind.InitialState }
        val viewRoot = view.rootTag!!
        val viewNodes = viewRoot.subTags.filter { it.localName == "child" }.associateBy { it.getAttributeValue("id") }
        val viewWires = viewRoot.subTags.filter { it.localName == "wire" }
        val rootProperties = viewRoot.subTags.filter { it.localName == "property" }
        val root = source().rootTag!!
        fun set(tag: XmlTag, key: String, value: String) { if (DiagramXmlText.read(tag, key) != value) DiagramXmlText.write(tag, key, value) }
        fun add(parent: XmlTag, name: String) = parent.addSubTag(parent.createChildTag(name, root.namespace, null, false), false)
        val retained = nodes.map { it.id }.toSet()
        activities.filterKeys { it !in retained }.values.filter { it.isValid }.forEach { it.delete() }
        activities.keys.retainAll(retained)
        val transitionCopies = transitions.filterValues { it.isValid }.mapValues { it.value.text }
        fun syncProperties(target: XmlTag, desired: List<XmlTag>, transaction: Boolean = false) {
            val kept = mutableSetOf<XmlTag>()
            desired.forEach { property ->
                val key = DiagramXmlText.read(property, "key")
                val actual = properties[property]?.takeIf { it.isValid && it.parentTag == target && it !in kept }
                    ?: add(target, if (transaction && key != "description") "transaction-manager" else "property")
                set(actual, if (actual.localName == "transaction-manager") "id" else "name", key)
                set(actual, if (actual.localName == "transaction-manager") "commit" else "value", DiagramXmlText.read(property, "value"))
                properties[property] = actual
                kept += actual
            }
            target.subTags.filter {
                it.namespace == SERVICE_NAMESPACE && it !in kept &&
                    (if (transaction) it.localName == "transaction-manager" ||
                        (it.localName == "property" && DiagramXmlText.read(it, "name") == "description")
                    else it.localName == "property")
            }.forEach { it.delete() }
        }
        syncProperties(root, rootProperties, true)
        nodes.forEach { node ->
            val actual = activities[node.id]?.takeIf { it.isValid } ?: add(root, "activity").also { activities[node.id] = it }
            set(actual, "name", node.name)
            val props = viewNodes.getValue(node.id).subTags.filter { it.localName == "property" }
            set(actual, "class", props.firstOrNull { DiagramXmlText.read(it, "key") == "class" }?.let { DiagramXmlText.read(it, "value") }.orEmpty())
            syncProperties(actual, props.filter { DiagramXmlText.read(it, "key") != "class" })
            val outgoing = node.outputs.map { diagram.wires[it] }
            val existing = actual.serviceChildren("transition")
            val pairs = outgoing.map { it.condition to if (it.end!!.kind == DiagramNodeKind.FinalState) "end" else it.end.name }
            if (existing.map { DiagramXmlText.read(it, "name").ifBlank { "success" } to DiagramXmlText.read(it, "value") } != pairs) {
                existing.forEach { it.delete() }
                outgoing.forEachIndexed { i, wire ->
                    val id = viewWires[wire.index].getAttributeValue("id").orEmpty()
                    val draft = transitionCopies[id]?.let { com.intellij.psi.XmlElementFactory.getInstance(project).createTagFromText(it) }
                        ?: actual.createChildTag("transition", root.namespace, null, false)
                    val before = actual.subTags.firstOrNull { it.localName == "property" }
                    val tag = (if (before == null) actual.addSubTag(draft, false) else actual.addBefore(draft, before)) as XmlTag
                    set(tag, "name", pairs[i].first); set(tag, "value", pairs[i].second)
                    transitions[id] = tag
                }
            }
        }
        val initial = starts.singleOrNull()?.outputs?.singleOrNull()?.let { diagram.wires[it].end?.name }.orEmpty()
        set(root, "initial", initial)
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
        stamp = stamp()
        pendingChanges = false
        projected = null
    }
}
